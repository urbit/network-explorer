(ns network-explorer.main
  (:require [datomic.client.api :as d]
            [hato.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clj-ob.ob :as ob]
            [datomic.ion.dev :as ion]
            [datomic.ion.cast :as cast]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kwparams]
            [datomic.ion.dev :as dev]))

(def cfg {:server-type :ion
          :region "us-east-2" ;; e.g. us-east-1
          :system "network-explorer"
          :endpoint "https://80dqm67uab.execute-api.us-east-2.amazonaws.com"})

(def get-client (memoize (fn [] (d/client cfg))))

(def LSR-START 1546990481000)

(defn parse-deposit [timestamp s]
  (let [[addr star] (map (partial apply str) (partition 64 s))]
    {:address   (str "0x" (subs addr 24))
     :star      (ob/biginteger->patp (BigInteger. star 16))
     :deposited-at timestamp
     :type :deposit}))

(defn parse-withdraw [timestamp from]
  {:type :withdraw
   :address from
   :withdrawn-at timestamp})

(defn parse-register [timestamp s]
  (let [[addr wnd amt rat rtu] (map (partial apply str) (partition 64 s))]
    {:address (str "0x" (subs addr 24))
     :windup (java.util.Date. (+ LSR-START (* 1000 (Long/parseLong wnd 16))))
     :amount (Integer/parseInt amt 16)
     :rate (Integer/parseInt rat 16)
     :rate-unit (Long/parseLong rtu 16)
     :timestamp timestamp
     :type :register}))

(defn parse-transfer-batch [s from]
  {:from-address (str "0x" (subs s 24))
   :to-address from
   :type :transfer-batch})

(defn parse-transaction [{:strs [input from timeStamp]}]
  (let [m {"0xbfca1ead" :register
           "0xe6deefa9" :deposit
           "0xbf547894" :transfer-batch
           "0x51cff8d9" :withdraw}
        timestamp (-> timeStamp BigInteger. java.time.Instant/ofEpochSecond java.util.Date/from)]
    (case (get m (subs input 0 10))
      :register (parse-register timestamp (subs input 10))
      :deposit  (parse-deposit timestamp (subs input 10))
      :transfer-batch (parse-transfer-batch (subs input 10) from)
      :withdraw (parse-withdraw timestamp from)
      nil)))

(defn parse-transactions [ts]
  (->> ts
       (filter (fn [e] (= "0" (get e "isError"))))
       (map parse-transaction)
       (reduce (fn [acc e]
                 (case (:type e)
                   :register (assoc acc (:address e)
                                    (assoc e :stars []
                                           :withdrawn []
                                           :next-unlock (:windup e)
                                           :current-rate 1))
                   :deposit
                   (let [{:keys [amount
                                 withdrawn
                                 stars
                                 next-unlock
                                 rate
                                 current-rate
                                 rate-unit]} (get acc (:address e))]
                     (if (> amount (inc (+ (count withdrawn) (count stars))))
                       (update-in acc [(:address e) :stars] conj e)
                       (-> (update-in acc [(:address e) :stars] conj e)
                           (update-in
                            [(:address e) :stars]
                            (fn [stars]
                              (vec (rseq (mapv (fn [x u] (assoc x :unlocked-at u))
                                               (rseq stars)
                                               (->> (iterate inc 0)
                                                    (map (fn [y]
                                                           (java.util.Date. (+ (.getTime next-unlock)
                                                                               (* 1000 y rate-unit)))))
                                                    (map (partial repeat rate))
                                                    (mapcat identity)
                                                    (drop (dec current-rate)))))))))))
                   :transfer-batch (-> (assoc acc (:to-address e) (get acc (:from-address e)))
                                       (dissoc (:from-address e)))
                   :withdraw
                   (let [star (last (:stars (get acc (:address e))))
                         unlocked? (boolean (:unlocked-at star))
                         {:keys [next-unlock rate rate-unit current-rate]} (get acc (:address e))]
                     (-> acc
                         (update-in [(:address e) :withdrawn]
                                    conj
                                    (if unlocked?
                                      (merge e star)
                                      (merge e star {:unlocked-at next-unlock})))
                         (update-in
                          [(:address e) :next-unlock]
                          (if (or unlocked? (not (= current-rate rate)))
                            identity
                            (fn [u] (java.util.Date. (+ (.getTime u) (* 1000 rate-unit))))))
                         (update-in [(:address e) :current-rate]
                                    (if (= current-rate rate) (constantly 1) inc))
                         (update-in [(:address e) :stars] pop)))
                   acc
                   )) {})
       vals
       (mapcat (fn [e]
                 (map (fn [s] (merge {:lsr/address (:address s)
                                      :lsr/star {:db/id [:node/urbit-id (:star s)]}
                                      :lsr/deposited-at (:deposited-at s)}
                                     (when (:unlocked-at s)
                                       {:lsr/unlocked-at (:unlocked-at s)})
                                     (when (:withdrawn-at s)
                                       {:lsr/withdrawn-at (:withdrawn-at s)})))
                      (concat (:stars e) (:withdrawn e)))))))

(defn get-transactions []
  (loop [r []
         startblock "0"]
    (let [res (-> (http/get "https://api.etherscan.io/api"
                            {:query-params {:module "account"
                                            :action "txlist"
                                            :address "0x86cd9cd0992f04231751e3761de45cecea5d1801"
                                            :startblock startblock
                                            :endblock 99999999
                                            :page 1
                                            :offset 10000
                                            :sort "asc"
                                            :apikey "GGVBET75PP24QF7G1PSFAIIU1B24MC8BJM"}})
                  :body
                  json/read-str
                  (get "result"))
          block (get (last res) "blockNumber")]
      (Thread/sleep 1000)
      (if (< (count res) 10000)
        (concat r res)
        (recur (concat r (take-while (fn [e] (not= block (get e "blockNumber"))) res)) block)))))

(defn get-radar-data []
  (-> (http/get "http://165.232.131.25/~radar.json" {:timeout 300000 :connect-timeout 300000})
      :body
      json/read-str))

(defn transform-radar-time [n]
  (-> (java.time.Instant/ofEpochMilli n)
      (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
      (.truncatedTo java.time.temporal.ChronoUnit/DAYS)
      .toInstant
      java.util.Date/from))

(defn date-range [since until]
  (seq (.collect (.datesUntil since until) (java.util.stream.Collectors/toList))))

(defn distinct-by
  "Returns a stateful transducer that removes elements by calling f on each step as a uniqueness key.
   Returns a lazy sequence when provided with a collection."
  ([f]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [v (f input)]
            (if (contains? @seen v)
              result
              (do (vswap! seen conj v)
                  (rf result input)))))))))
  ([f xs]
   (sequence (distinct-by f) xs)))

(defn add-composite-index
  "Only required because lookup refs do not work when transacting an entity with
  a tuple index that contains a :db.type/ref."
  [data]
  (let [client (get-client)
        conn   (d/connect client {:db-name "network-explorer"})
        db     (d/db conn)
        urbit-ids (map (comp second :db/id :ping/urbit-id first) data)
        urbit-id->eid (into {} (d/q '[:find ?u ?e
                                      :in $ [?u ...]
                                      :where [?e :node/urbit-id ?u]] db urbit-ids))]

    (map (fn [es]
           (map (fn [e]
                  (assoc e :ping/time+urbit-id
                         [(:ping/time e) (urbit-id->eid (-> e
                                                            :ping/urbit-id
                                                            :db/id
                                                            second))])) es)) data)))

(defn radar-data->txs [data]
  (->> data
       (map (fn [[k v]]
              (map (fn [e]
                     {:ping/result (get e "result")
                      :ping/time (transform-radar-time (get e "response"))
                      :ping/urbit-id {:db/id [:node/urbit-id k]}}) v)))
       (remove empty?)
       (map (fn [e] (distinct-by :ping/time e)))
       (filter (fn [e]
                 (#{:galaxy :star :planet} (ob/clan (-> (first e)
                                                        :ping/urbit-id
                                                        :db/id
                                                        second)))))
       add-composite-index
       (mapcat identity)))

(defn get-pki-data []
  (:body (http/get "https://gaze-exports.s3.us-east-2.amazonaws.com/events.txt"
                   {:timeout 300000
                    :connect-timeout 300000
                    :http-client {:ssl-context {:insecure? true}}})))


(defn parse-pki-time [s]
  (.parse
   (doto (java.text.SimpleDateFormat. "~yyyy.MM.dd..HH.mm.ss")
     (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
   s))

(defn format-pki-time [inst]
  (-> (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
      (doto (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
      (.format inst (java.lang.StringBuffer.) (java.text.FieldPosition. 0))
      .toString))

(defn inc-attr
  "Transaction function that increments the value of entity's card-1
attr by amount, treating a missing value as 1."
  [db entity attr amount]
  (let [m (d/pull db {:eid entity :selector [:db/id attr]})]
    [[:db/add (:db/id m) attr (+ (or (attr m) 1) amount)]]))

(defn add-urbit-id
  "Transaction function that checks whether an urbit-id has a sponsor
  set, setting it with ob/sein if needed."
  [db urbit-id]
  (let [s (ffirst (d/q '[:find ?s
                         :in $ ?urbit-id
                         :where [?e :node/urbit-id ?urbit-id]
                         [?e :node/sponsor ?s]]
                       db
                       urbit-id))]
    [{:node/urbit-id urbit-id
      :node/point   (ob/patp->biginteger urbit-id)
      :node/type    (ob/clan urbit-id)
      :node/sponsor (if s s [:node/urbit-id (ob/sein urbit-id)])}]))

(def pki-events-query
  '[:find (pull ?e [:pki-event/id
                    {:pki-event/node [:node/urbit-id]}
                    {:pki-event/target-node [:node/urbit-id]}
                    :pki-event/type
                    :pki-event/time
                    :pki-event/address
                    :pki-event/continuity
                    :pki-event/revision])
    :in $ ?urbit-id ?since
    :where [?p :node/urbit-id ?urbit-id]
           (or [?e :pki-event/node ?p]
               [?e :pki-event/target-node ?p])
           [?e :pki-event/time ?t]
           [(<= ?since ?t)]])

(def all-urbit-ids-query-types
  '[:find (pull ?e [:node/urbit-id
                    :node/type
                    [:node/revision :default 0]
                    [:node/continuity :default 0]
                    [:node/num-owners :default 1]
                    [:node/ownership-address :default nil]
                    [:node/management-proxy :default nil]
                    [:node/transfer-proxy :default nil]
                    [:node/voting-proxy :default nil]
                    [:node/spawn-proxy :default nil]
                    {:node/sponsor [:node/urbit-id]}
                    {[:node/_sponsor :as :node/kids :default []] [:node/urbit-id]}])
    :in $ [?type ...]
    :where [?e :node/urbit-id]
           [?e :node/type ?type]])

(def all-urbit-ids-query
  '[:find (pull ?e [:node/urbit-id
                    :node/type
                    [:node/revision :default 0]
                    [:node/continuity :default 0]
                    [:node/num-owners :default 1]
                    [:node/ownership-address :default nil]
                    [:node/management-proxy :default nil]
                    [:node/transfer-proxy :default nil]
                    [:node/voting-proxy :default nil]
                    [:node/spawn-proxy :default nil]
                    {:node/sponsor [:node/urbit-id {:node/sponsor [:node/urbit-id]}]}
                    {[:node/_sponsor :as :node/kids :default []] [:node/urbit-id]}])
    :where [?e :node/urbit-id]])


(def urbit-ids-query-types
  '[:find (pull ?e [:node/urbit-id
                    :node/type
                    [:node/revision :default 0]
                    [:node/continuity :default 0]
                    [:node/num-owners :default 1]
                    [:node/ownership-address :default nil]
                    [:node/management-proxy :default nil]
                    [:node/transfer-proxy :default nil]
                    [:node/voting-proxy :default nil]
                    [:node/spawn-proxy :default nil]
                    {:node/sponsor [:node/urbit-id]}
                    {[:node/_sponsor :as :node/kids :default []]
                     [:node/urbit-id :node/continuity :node/revision :node/num-owners]}])
    :in $ [?urbit-id ...] [?type ...]
    :where [?e :node/urbit-id ?urbit-id]
           [?e :node/type ?type]])

(def urbit-ids-query
  '[:find (pull ?e [:node/urbit-id
                    :node/type
                    [:node/revision :default 0]
                    [:node/continuity :default 0]
                    [:node/num-owners :default 1]
                    [:node/ownership-address :default nil]
                    [:node/management-proxy :default nil]
                    [:node/transfer-proxy :default nil]
                    [:node/voting-proxy :default nil]
                    [:node/spawn-proxy :default nil]
                    {:node/sponsor [:node/urbit-id {:node/sponsor [:node/urbit-id]}]}
                    {[:node/_sponsor :as :node/kids :default []]
                     [:node/urbit-id :node/continuity :node/revision :node/num-owners]}])
    :in $ [?urbit-id ...]
    :where [?e :node/urbit-id ?urbit-id]])

(def activity-query
  '[:find (pull ?e [:ping/time
                    :ping/response
                    {:ping/urbit-id [:node/urbit-id]}
                    :ping/result])
    :in $ ?urbit-id ?since
    :where [?e :ping/urbit-id ?p]
           [?p :node/urbit-id ?urbit-id]
           [?e :ping/time ?t]
           [(<= ?since ?t)]])

(def aggregate-query
  '[:find ?t
    :in $ ?event-type ?since ?until
    :where [?e :pki-event/type ?event-type]
           [?e :pki-event/time ?t]
           [(<= ?since ?t)]
           [(>= ?until ?t)]])

(def aggregate-query-node-type
  '[:find ?t
    :in $ ?node-type ?event-type ?since ?until
    :where [?e :pki-event/type ?event-type]
           [?e :pki-event/node ?p]
           [?p :node/type ?node-type]
           [?e :pki-event/time ?t]
           [(<= ?since ?t)]
           [(>= ?until ?t)]])

(defn pki-line->nodes [acc l]
  (->> (filter ob/patp? l)
       (apply conj acc)))

(defn node->node-tx [s]
  `(network-explorer.main/add-urbit-id ~s))

(defn node->node-tx-no-sponsor [s]
  (merge {:node/urbit-id  s
          :node/point     (ob/patp->biginteger s)
          :node/type      (ob/clan s)}))

(defn pki-line->txs [idx l]
  (let [base {:pki-event/id   idx
              :pki-event/time (parse-pki-time (first l))
              :pki-event/node {:db/id [:node/urbit-id (second l)]}}]
    [(case (nth l 2)
       "keys"         [(merge base {:pki-event/type :change-networking-keys
                                    :pki-event/revision (Long/valueOf (nth l 3))})
                       {:node/urbit-id (second l)
                        :node/revision (Long/valueOf (nth l 3))}]
       "breached"     [(merge base {:pki-event/type :broke-continuity
                                    :pki-event/continuity (Long/valueOf (nth l 3))})
                       {:node/urbit-id (second l)
                        :node/continuity (Long/valueOf (nth l 3))}]
       "management-p" [(merge base {:pki-event/type :change-management-proxy
                                    :pki-event/address (nth l 3)})
                       {:node/urbit-id (second l)
                        :node/management-proxy (nth l 3)}]
       "transfer-p"   [(merge base {:pki-event/type :change-transfer-proxy
                                    :pki-event/address (nth l 3)})
                       {:node/urbit-id (second l)
                        :node/transfer-proxy (nth l 3)}]
       "spawn-p"      [(merge base {:pki-event/type :change-spawn-proxy
                                    :pki-event/address (nth l 3)})
                       {:node/urbit-id (second l)
                        :node/spawn-proxy (nth l 3)}]
       "voting-p"     [(merge base {:pki-event/type :change-voting-proxy
                                    :pki-event/address (nth l 3)})
                       {:node/urbit-id (second l)
                        :node/voting-proxy (nth l 3)}]
       "owner"        [(merge base {:pki-event/type :change-ownership
                                    :pki-event/address (nth l 3)})
                       {:node/urbit-id (second l)
                        :node/ownership-address (nth l 3)}
                       `(network-explorer.main/inc-attr [:node/urbit-id ~(second l)] :node/num-owners 1)]
       "activated"    [(merge base {:pki-event/type :activate})]
       "spawned"      [(merge base {:pki-event/type :spawn
                                    :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}})]
       "invite"       [(merge base {:pki-event/type :invite
                                    :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}
                                    :pki-event/address (nth l 4)})]
       "sponsor"      (case (nth l 3)
                        "escaped to"    [(merge base {:pki-event/type :escaped
                                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}})
                                         {:node/urbit-id (second l)
                                          :node/sponsor {:db/id [:node/urbit-id (nth l 4)]}}]
                        "detached from" [(merge base {:pki-event/type :lost-sponsor
                                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}})
                                         [:db/retract [:node/urbit-id (second l)] :node/sponsor [:node/urbit-id (nth l 4)]]])
       "escape-req"   (case (nth l 3)
                        "canceled"   [(merge base {:pki-event/type :escape-canceled})]
                        [(merge base {:pki-event/type :escape-requested
                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}})]))]))

(defn mapcat-indexed [f & colls]
 (apply concat (apply map-indexed f colls)))

(defn pki-data->tx [data]
  (let [lines (->> (str/split-lines data)
                   (map (fn [l] (str/split l #",")))
                   (drop 1)
                   reverse)
        node-txs (map node->node-tx (reduce pki-line->nodes #{} lines))
        pki-txs  (mapcat-indexed pki-line->txs lines)]
    [node-txs pki-txs]))

(defn update-data [_]
  (let [;; pki-event/id is just line index, historic is the index of the last
        ;; pki event from https://gaze-exports.s3.us-east-2.amazonaws.com/events-2018-2019-2020.txt
        historic   274811
        client     (get-client)
        conn       (d/connect client {:db-name "network-explorer-2"})
        db         (d/db conn)
        newest-id  (or (ffirst (d/q '[:find (max ?id) :where [_ :pki-event/id ?id]] db)) -1)
        lines      (->> (str/split-lines (get-pki-data))
                        (map (fn [l] (str/split l #",")))
                        (drop 1)
                        reverse
                        (drop (- (inc newest-id) (inc historic))))
        nodes      (reduce pki-line->nodes #{} lines)
        no-sponsor (map node->node-tx-no-sponsor nodes)
        node-txs   (map node->node-tx nodes)
        pki-txs    (mapcat pki-line->txs
                           (range (inc newest-id) (+ (inc newest-id) (count lines)))
                           lines)]
    (d/transact conn {:tx-data no-sponsor})
    (doseq [txs (partition 30000 30000 nil node-txs)]
      (d/transact conn {:tx-data txs}))
    (doseq [txs pki-txs]
      (d/transact conn {:tx-data txs}))
    #_(d/transact conn {:tx-data (radar-data->txs (get-radar-data))})
    (pr-str (count pki-txs))))

(defn get-all-nodes [limit offset types db]
  (let [query (if (empty? types)
                all-urbit-ids-query
                all-urbit-ids-query-types)
        args (if (empty? types) [db] [db types])]
    (mapcat identity (d/q {:query query
                           :args args
                           :limit limit
                           :offset offset}))))

(defn get-nodes [urbit-ids limit offset types db]
  (let [query (if (empty? types)
                urbit-ids-query
                urbit-ids-query-types)
        args (if (empty? types) [db urbit-ids] [db urbit-ids types])]
    (mapcat identity (d/q {:query query
                           :args args
                           :limit limit
                           :offset offset}))))

(defn get-nodes* [query-params db]
  (let [urbit-ids (if (get query-params :urbit-id)
                    (str/split (get query-params :urbit-id) #",")
                    #{})
        limit (Integer/parseInt (get query-params :limit "1000"))
        offset  (Integer/parseInt (get query-params :offset "0"))
        types (if (get query-params :node-types)
                (map keyword (str/split (get query-params :node-types) #","))
                #{})]
    (if-not (every? ob/patp? urbit-ids)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "One or more invalid urbit-ids"})}
      (if-not (every? #{:galaxy :star :planet :moon :comet} types)
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "One or more invalid node-types"})}
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str (if (empty? urbit-ids)
                                 (get-all-nodes limit offset types db)
                                 (get-nodes urbit-ids limit offset types db)))}))))

(defn get-node [urbit-id db]
  (first (get-nodes [urbit-id] 1 0 [] db)))

(defn get-node* [query-params db]
  (let [urbit-id (get query-params :urbit-id)]
    (if-not (ob/patp? urbit-id)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Invalid urbit-id"})}
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str (get-node urbit-id db))})))

(defn stringify-date [key value]
  (case key
    :pki-event/time (format-pki-time value)
    :ping/time      (format-pki-time value)
    :ping/response  (format-pki-time value)
    value))

(defn get-pki-events [urbit-id since db]
  (->> (d/q pki-events-query db urbit-id since)
       (mapcat identity)
       (sort-by (comp - :pki-event/id))))

(defn get-all-pki-events
  ([limit offset since db]
   (let [selector [:pki-event/id
                   {:pki-event/node [:node/urbit-id]}
                   {:pki-event/target-node [:node/urbit-id]}
                   :pki-event/type
                   :pki-event/time
                   :pki-event/address
                   :pki-event/continuity
                   :pki-event/revision]]
     (take-while (fn [e] (.after (:pki-event/time e) since))
                 (d/index-pull db {:index :avet
                                   :selector selector
                                   :start [:pki-event/id]
                                   :reverse true
                                   :limit limit
                                   :offset offset}))))
  ([limit offset since type db]
   (let [selector [:pki-event/id
                   {:pki-event/node [:node/urbit-id :node/type]}
                   {:pki-event/target-node [:node/urbit-id]}
                   :pki-event/type
                   :pki-event/time
                   :pki-event/address
                   :pki-event/continuity
                   :pki-event/revision]]
     (into [] (comp
               (filter (fn [e] (= type (:node/type (:pki-event/node e)))))
               (drop offset)
               (take limit)
               (take-while (fn [e] (.after (:pki-event/time e) since))))
           (d/index-pull db {:index :avet
                             :selector selector
                             :start [:pki-event/id]
                             :reverse true})))))

(defn get-pki-events* [query-params db]
  (let [urbit-id  (get query-params :urbit-id)
        node-type (keyword (get query-params :nodeType))
        since     (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" )
                          (get query-params :since "1970-01-01T00:00:00.000Z"))
        limit     (Integer/parseInt (get query-params :limit "1000"))
        offset    (Integer/parseInt (get query-params :offset "0"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (if urbit-id
                             (get-pki-events urbit-id since db)
                             (if node-type
                               (get-all-pki-events limit offset since node-type db)
                               (get-all-pki-events limit offset since db)))
                           :value-fn stringify-date)}))

(defn add-zero-counts [dr query-res]
  (loop [d dr
         q query-res
         res []]
    (if-not (seq d)
      res
      (if (= (first d) (:date (first q)))
        (recur (rest d) (rest q) (conj res (first q)))
        (recur (rest d) q (conj res {:date (first d) :count 0}))))))

(defn run-aggregate-query [since f]
  (let [dr (map str (date-range since (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1)))]
    (add-zero-counts
     dr
     (into []
           (comp (partition-by
                  (fn [e] (-> (first e)
                              .toInstant
                              (.atZone java.time.ZoneOffset/UTC)
                              .toLocalDate
                              .toString)))
                 (map (fn [e] {:date (-> (ffirst e)
                                         .toInstant
                                         (.atZone java.time.ZoneOffset/UTC)
                                         .toLocalDate
                                         .toString)
                               :count (count e)})))
           (sort (f))))))

(defn get-aggregate-pki-events
  ([event-type since db]
   (run-aggregate-query
    since
    #(d/q aggregate-query
          db
          event-type
          (java.util.Date/from (.toInstant (.atStartOfDay since java.time.ZoneOffset/UTC)))
          (java.util.Date.))))
  ([node-type event-type since db]
   (run-aggregate-query
    since
    #(d/q aggregate-query-node-type
          db
          node-type
          event-type
          (java.util.Date/from (.toInstant (.atStartOfDay since java.time.ZoneOffset/UTC)))
          (java.util.Date.)))))

(defn get-aggregate-pki-events* [query-params db]
  (let [since (java.time.LocalDate/parse (get query-params :since "2018-11-27"))
        node-type  (keyword (get query-params :nodeType))
        event-type (keyword (get query-params :eventType))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (if node-type
              (get-aggregate-pki-events node-type event-type since db)
              (get-aggregate-pki-events event-type since db)) :value-fn stringify-date)}))

(defn get-all-activity [limit offset db]
  (let [selector [:ping/time
                  :ping/response
                  {:ping/urbit-id [:node/urbit-id]}
                  :ping/result]]
    (d/index-pull db {:index :avet
                      :selector selector
                      :start [:ping/time]
                      :reverse true
                      :limit limit
                      :offset offset})))

(defn get-activity [urbit-id since db]
  (mapcat identity (d/q {:query activity-query
                         :args [db urbit-id since]})))

(defn get-activity* [query-params db]
  (let [urbit-id (get query-params :urbit-id)
        since    (parse-pki-time (get query-params :since "~1970.1.1..00.00.00"))
        limit    (Integer/parseInt (get query-params :limit "1000"))
        offset   (Integer/parseInt (get query-params :offset "0"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (if urbit-id
                             (get-activity urbit-id since db)
                             (get-all-activity limit offset db))
                           :value-fn stringify-date)}))
(def unlockable-query
  '[:find (count ?l) ?until
    :keys count date
    :in $ [?until ...]
    :where [?l :lsr/star ?e]
           [?l :lsr/unlocked-at ?u]
           [?l :lsr/deposited-at ?d]
           [(> ?u ?until)]
           [(> ?until ?d)]
           [?l :lsr/withdrawn-at ?w]
           [(> ?w ?until)]
           #_(or-join [?l ?until]
             [(missing? $ ?l :lsr/withdrawn-at)]
             (and [?l :lsr/withdrawn-at ?w]
                  [(> ?w ?until)]))])

(def locked-query
  '[:find (count ?e) ?until
    :keys locked date
    :in $ [?until ...]
    :where [?l :lsr/star ?e]
           [?l :lsr/unlocked-at ?u]
           [?l :lsr/deposited-at ?d]
           [(> ?u ?until)]
           [(> ?until ?d)]])

(def activated-query
  '[:find ?t
    :in $
    :where [?a :pki-event/type :activate]
           [?a :pki-event/time ?t]])


(def activated-query-node-type
  '[:find ?t
    :in $ ?node-type
    :where [?e :node/type ?node-type]
           [?p :pki-event/node ?e]
           [?s :pki-event/type :activate]
           [?s :pki-event/time ?t]])

(def spawned-query
  '[:find ?t
    :in $
    :where [?s :pki-event/type :spawn]
           [?s :pki-event/time ?t]])

(def spawned-query-node-type
  '[:find ?t
    :in $ ?node-type
    :where [?e :node/type ?node-type]
    [?p :pki-event/node ?e]
    [?s :pki-event/type :spawn]
    [?s :pki-event/time ?t]])

(def set-networking-keys-query
  '[:find ?t
    :in $
    :where [?s :pki-event/revision 1]
           [?s :pki-event/type :change-networking-keys]
           [?s :pki-event/time ?t]])

(def set-networking-keys-query-node-type
  '[:find ?t
    :in $ ?node-type
    :where [?s :pki-event/revision 1]
           [?s :pki-event/node ?e]
           [?e :node/type ?node-type]
           [?s :pki-event/type :change-networking-keys]
           [?s :pki-event/time ?t]])


(defn running-total [key agg]
  (loop [sum 0
         in agg
         out []]
    (if-not (seq in)
      out
      (recur (+ sum (:count (first in)))
             (rest in)
             (conj out {:date (:date (first in)) key (+ (:count (first in)) sum)})))))

(defn get-locked-aggregate [db]
  (let [datoms (d/q '[:find ?d ?u
                  :where [?e :lsr/unlocked-at ?u]
                         [?e :lsr/deposited-at ?d]
                         [(> ?u ?d)]] db)
        ds (sort (map first datoms))
        us (sort (map second datoms))
        dr (map str (date-range (java.time.LocalDate/parse "2018-11-27")
                                (.plusDays (.toLocalDate (.atZone (.toInstant (last us)) java.time.ZoneOffset/UTC)) 1)))
        deposits  (add-zero-counts
                   dr
                   (into []
                         (comp (partition-by
                                (fn [e] (-> e
                                            .toInstant
                                            (.atZone java.time.ZoneOffset/UTC)
                                            .toLocalDate
                                            .toString)))
                               (map (fn [e] {:date (-> (first e)
                                                       .toInstant
                                                       (.atZone java.time.ZoneOffset/UTC)
                                                       .toLocalDate
                                                       .toString)
                                             :count (count e)})))
                         ds))
        unlocks  (add-zero-counts
                  dr
                  (into []
                        (comp (partition-by
                               (fn [e] (-> e
                                           .toInstant
                                           (.atZone java.time.ZoneOffset/UTC)
                                           .toLocalDate
                                           .toString)))
                              (map (fn [e] {:date (-> (first e)
                                                      .toInstant
                                                      (.atZone java.time.ZoneOffset/UTC)
                                                      .toLocalDate
                                                      .toString)
                                            :count (count e)})))
                        us))

        ]
    (running-total :locked (map (fn [d u] {:date (:date d) :count (- (:count d) (:count u))}) deposits unlocks))))

(defn get-aggregate-status
  ([since until db]
   (map merge
        (running-total :set-networking-keys (run-aggregate-query
                                             (java.time.LocalDate/parse "2018-11-27")
                                             #(d/q set-networking-keys-query db)))
        (running-total :spawned (run-aggregate-query
                                 (java.time.LocalDate/parse "2018-11-27")
                                 #(d/q spawned-query db)))
        (running-total :activated (run-aggregate-query
                                   (java.time.LocalDate/parse "2018-11-27")
                                   #(d/q activated-query db)))
        (get-locked-aggregate db)))
  ([since until node-type db]
   (map merge
        (running-total :set-networking-keys (run-aggregate-query
                                             (java.time.LocalDate/parse "2018-11-27")
                                             #(d/q set-networking-keys-query-node-type db node-type)))
        (running-total :spawned (run-aggregate-query
                                 (java.time.LocalDate/parse "2018-11-27")
                                 #(d/q spawned-query-node-type db node-type)))
        (running-total :activated (run-aggregate-query
                                   (java.time.LocalDate/parse "2018-11-27")
                                   #(d/q activated-query-node-type db node-type)))
        (if (= :star node-type) (get-locked-aggregate db) (repeat {})))))

(defn get-aggregate-status* [query-params db]
  (let [since (java.time.LocalDate/parse (get query-params :since "2018-11-27"))
        until (java.time.LocalDate/parse (get query-params :until "2025-01-01"))
        node-type  (keyword (get query-params :nodeType))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (if node-type
              (get-aggregate-status since until node-type db)
              (get-aggregate-status since until db))
            :value-fn stringify-date)}))

(defn root-handler [req]
  (let [client       (get-client)
        conn         (d/connect client {:db-name "network-explorer-2"})
        db           (d/db conn)
        path         (get req :uri)
        query-params (get req :params)]
    (case path
      "/get-node"                 (get-node* query-params db)
      "/get-nodes"                (get-nodes* query-params db)
      "/get-aggregate-pki-events" (get-aggregate-pki-events* query-params db)
      "/get-pki-events"           (get-pki-events* query-params db)
      "/get-activity"             (get-activity* query-params db)
      "/get-aggregate-status"     (get-aggregate-status* query-params db)
      {:status 404})))

(defn deploy-build! []
  (let [rev (-> (clojure.java.shell/sh "git" "rev-parse" "HEAD")
                :out
                str/trim-newline)]
    (ion/push {:rev rev})
    (ion/deploy {:group "datomic-storage"
                 :rev rev})))
(def app-handler
  (-> root-handler
      kwparams/wrap-keyword-params
      params/wrap-params))

;; (def conn (d/connect (get-client) {:db-name "network-explorer-2"}))
