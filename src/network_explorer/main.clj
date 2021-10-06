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
  (map (fn [e]
         [(-> e (.atStartOfDay java.time.ZoneOffset/UTC) .toInstant java.util.Date/from)
          (-> e (.plusDays 1) (.atStartOfDay java.time.ZoneOffset/UTC) .toInstant java.util.Date/from)])
       (seq (.collect (.datesUntil since until) (java.util.stream.Collectors/toList)))))

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
  #_(:body (http/get "https://raw.githubusercontent.com/jalehman/urbit-metrics/master/historic-events.csv" {:timeout 300000
                                                                                                          :connect-timeout 300000
                                                                                                          :http-client {:ssl-context {:insecure? true}}}))
  (:body (http/get "https://azimuth.network/stats/events.txt" {:timeout 300000
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


(def spawned-query
  '[:find (pull ?e [*])
    :in $ [?urbit-id ...]
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/target-node ?p]
           [?e :pki-event/type :spawn]])

(def activated-query
  '[:find (pull ?e [*])
    :in $ [?urbit-id ...]
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :activate]])

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
                    [:node/online :default false]
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
                    [:node/online :default false]
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
                    [:node/online :default false]
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
                    [:node/online :default false]
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
  '[:find ?s (count ?e)
    :in $ ?event-type [[?s ?u] ...]
    :keys date count
    :where [?e :pki-event/type ?event-type]
           [?e :pki-event/time ?t]
           [(<= ?s ?t)]
           [(>= ?u ?t)]])

(def aggregate-query-node-type
  '[:find ?s (count ?e)
    :in $ ?node-type ?event-type [[?s ?u] ...]
    :keys date count
    :where [?e :pki-event/type ?event-type]
           [?e :pki-event/node ?p]
           [?p :node/type ?node-type]
           [?e :pki-event/time ?t]
           [(<= ?s ?t)]
           [(>= ?u ?t)]])

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
        ;; pki event from historic-events.txt
        historic   203009
        client     (get-client)
        conn       (d/connect client {:db-name "network-explorer"})
        db         (d/db conn)
        newest-id  (or (ffirst (d/q '[:find (max ?id) :where [_ :pki-event/id ?id]] db)) -1)
        lines      (->> (str/split-lines (get-pki-data))
                        (map (fn [l] (str/split l #",")))
                        (drop 1)
                        reverse
                        (drop (- (inc historic) (inc newest-id))))
        nodes      (reduce pki-line->nodes #{} lines)
        no-sponsor (map node->node-tx-no-sponsor nodes)
        node-txs   (map node->node-tx nodes)
        pki-txs    (mapcat pki-line->txs
                           (range (inc newest-id) (+ (inc newest-id) (count lines)))
                           lines)]
    (d/transact conn {:tx-data no-sponsor})
    (d/transact conn {:tx-data node-txs})
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
    :date           (format-pki-time value)
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
      (if (= (ffirst d) (:date (first q)))
        (recur (rest d) (rest q) (conj res (first q)))
        (recur (rest d) q (conj res {:date (ffirst d) :count 0}))))))

(defn get-aggregate-pki-events
  ([event-type since db]
   (let [dr (date-range since (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1))]
     (add-zero-counts dr (d/q aggregate-query
                              db
                              event-type
                              dr))))
  ([node-type event-type since db]
   (let [dr (date-range since (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1))]
     (add-zero-counts dr (d/q aggregate-query-node-type
                              db
                              node-type
                              event-type
                              (date-range since (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1)))))))

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

(defn root-handler [req]
  (let [client       (get-client)
        conn         (d/connect client {:db-name "network-explorer"})
        db           (d/db conn)
        path         (get req :uri)
        query-params (get req :params)]
    (case path
      "/get-node"                 (get-node* query-params db)
      "/get-nodes"                (get-nodes* query-params db)
      "/get-aggregate-pki-events" (get-aggregate-pki-events* query-params db)
      "/get-pki-events"           (get-pki-events* query-params db)
      "/get-activity"             (get-activity* query-params db)
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

;; (def conn (d/connect (get-client) {:db-name "network-explorer"}))
