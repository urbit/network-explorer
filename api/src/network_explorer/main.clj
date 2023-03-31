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
            [datomic.ion.dev :as dev]
            [clojure.core.memoize :as memo]))

(def cfg {:server-type :ion
          :region "us-east-2" ;; e.g. us-east-1
          :system "network-explorer"
          :endpoint "https://80dqm67uab.execute-api.us-east-2.amazonaws.com"})

(def get-client (memoize (fn [] (d/client cfg))))

(defn get-radar-data []
  (:body (http/get "https://gaze-exports.s3.us-east-2.amazonaws.com/radar.txt"
                   {:timeout 300000
                    :connect-timeout 300000
                    :http-client {:ssl-context {:insecure? true}}})))

(defn transform-radar-time [n]
  (-> (java.time.Instant/ofEpochMilli n)
      (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
      (.truncatedTo java.time.temporal.ChronoUnit/DAYS)
      .toInstant
      java.util.Date/from))

(defn date-range [since until]
  (seq (.collect (.datesUntil since until) (java.util.stream.Collectors/toList))))

(defn get-pki-data []
  (:body (http/get "https://gaze-exports.s3.us-east-2.amazonaws.com/events.txt"
                   {:timeout 300000
                    :connect-timeout 300000
                    :http-client {:ssl-context {:insecure? true}}})))


(defn parse-pki-time [s]
  (try
    (.parse
     (doto (java.text.SimpleDateFormat. "~yyyy.MM.dd..HH.mm.ss")
       (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
     s)
    (catch Exception e
      (.parse
       (doto (java.text.SimpleDateFormat. "~yyyy.MM.dd")
         (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
       s))))

(defn radar-data->txs [[recvd sent point kids]]
  [{:node/urbit-id point
    :node/kids-hash kids}
   {:ping/sent (parse-pki-time (str/join ".." (take 2 (str/split sent #"\.\."))))
    :ping/received (parse-pki-time (str/join ".." (take 2 (str/split recvd #"\.\."))))
    :ping/kids kids
    :ping/urbit-id {:db/id [:node/urbit-id point]}}])

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
    [(merge {:node/urbit-id urbit-id
             :node/point   (ob/patp->biginteger urbit-id)
             :node/type    (ob/clan urbit-id)}
            (when-not (= :galaxy (ob/clan urbit-id))
              {:node/sponsor (if s s [:node/urbit-id (ob/sein urbit-id)])}))]))


(defn date->day [date]
  (-> date
      .toInstant
      (.atZone java.time.ZoneOffset/UTC)
      .toLocalDate
      .toString))

(defn date->midnight-utc [date]
  (-> date
      .toInstant
      (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
      (.truncatedTo java.time.temporal.ChronoUnit/DAYS)
      .toInstant
      java.util.Date/from))

(def pki-events-query
  '[:find (pull ?e [:pki-event/id
                    {:pki-event/node [:node/urbit-id]}
                    {:pki-event/target-node [:node/urbit-id]}
                    :pki-event/dominion
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
                    [:node/kids-hash :default nil]
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
                    [:node/kids-hash :default nil]
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
                    [:node/kids-hash :default nil]
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
                    [:node/kids-hash :default nil]
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


(def locked-query
  '[:find ?date-s (count ?l)
    :keys date count
    :in $
    :where [?l :lsr/deposited-at ?d]
           [?l :lsr/unlocked-at ?u]
           [(> ?u ?d)]
           [(network-explorer.main/date->day ?d) ?date-s]])

(def unlocked-query
  '[:find ?date-s (count ?l)
    :keys date count
    :in $
    :where [?l :lsr/deposited-at ?d]
           [?l :lsr/unlocked-at ?u]
           [(> ?u ?d)]
           [(network-explorer.main/date->day ?u) ?date-s]])

(def spawned-query-node-type
  '[:find ?date-s (count ?s)
    :in $ ?node-type
    :keys date count
    :where [?e :node/type ?node-type]
           [?s :pki-event/target-node ?e]
           [?s :pki-event/type :spawn]
           [?s :pki-event/time ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])


(def aggregate-query
  '[:find ?date-s (count ?s)
    :in $ ?event-type
    :keys date count
    :where [?s :pki-event/type ?event-type]
           [?s :pki-event/time ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def aggregate-query-node-type
  '[:find ?date-s (count ?s)
    :in $ ?event-type ?node-type
    :keys date count
    :where [?e :node/type ?node-type]
           [?s :pki-event/node ?e]
           [?s :pki-event/type ?event-type]
           [?s :pki-event/time ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def aggregate-query-since
  '[:find ?date-s (count ?s)
    :in $ ?event-type ?since
    :keys date count
    :where [?s :pki-event/type ?event-type]
           [?s :pki-event/time ?t]
           [(<= ?since ?t)]
           [?s :pki-event/node ?e]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def aggregate-query-since-node-type
  '[:find ?date-s (count ?s)
    :in $ ?event-type ?since ?node-type
    :keys date count
    :where [?s :pki-event/type ?event-type]
           [?s :pki-event/node ?e]
           [?e :node/type ?node-type]
           [?s :pki-event/time ?t]
           [(<= ?since ?t)]
           [(network-explorer.main/date->day ?t) ?date-s]])


(def set-networking-keys-query
  '[:find ?date-s (count-distinct ?p)
    :in $
    :keys date count
    :where (or (and [?s :pki-event/revision 1]
                    [?s :pki-event/dominion :l1])
               (and [?s :pki-event/revision 2]
                    [?s :pki-event/dominion :l2]))
           [?s :pki-event/type :change-networking-keys]
           [?s :pki-event/node ?p]
           [?s :pki-event/time ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def set-networking-keys-query-node-type
  '[:find ?date-s (count-distinct ?p)
    :in $ ?node-type
    :keys date count
    :where (or (and [?s :pki-event/revision 1]
                    [?s :pki-event/dominion :l1])
               (and [?s :pki-event/revision 2]
                    [?s :pki-event/dominion :l2]))
           [?s :pki-event/type :change-networking-keys]
           [?s :pki-event/node ?p]
           [?p :node/type ?node-type]
           [?s :pki-event/time ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def online-query
  '[:find ?date-s (count-distinct ?u)
    :in $
    :keys date online
    :where [?e :ping/received ?t]
           [?e :ping/urbit-id ?u]
           [(network-explorer.main/date->day ?t) ?date-s]])

(def online-query-node-type
  '[:find ?date-s (count-distinct ?u)
    :in $ ?node-type
    :keys date online
    :where [?u :node/type ?node-type]
           [?e :ping/urbit-id ?u]
           [?e :ping/received ?t]
           [(network-explorer.main/date->day ?t) ?date-s]])


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
              :pki-event/node {:db/id [:node/urbit-id (second l)]}
              :pki-event/dominion (case (nth l 2) "l1" :l1 "l2" :l2)}]
    [(case (nth l 3)
       "keys"         [(merge base {:pki-event/type :change-networking-keys
                                    :pki-event/revision (Long/valueOf (nth l 4))})
                       {:node/urbit-id (second l)
                        :node/revision (Long/valueOf (nth l 4))}]
       "breached"     [(merge base {:pki-event/type :broke-continuity
                                    :pki-event/continuity (Long/valueOf (nth l 4))})
                       {:node/urbit-id (second l)
                        :node/continuity (Long/valueOf (nth l 4))}]
       "management-p" [(merge base {:pki-event/type :change-management-proxy
                                    :pki-event/address (nth l 4)})
                       {:node/urbit-id (second l)
                        :node/management-proxy (nth l 4)}]
       "transfer-p"   [(merge base {:pki-event/type :change-transfer-proxy
                                    :pki-event/address (nth l 4)})
                       {:node/urbit-id (second l)
                        :node/transfer-proxy (nth l 4)}]
       "spawn-p"      [(merge base {:pki-event/type :change-spawn-proxy
                                    :pki-event/address (nth l 4)})
                       {:node/urbit-id (second l)
                        :node/spawn-proxy (nth l 4)}]
       "voting-p"     [(merge base {:pki-event/type :change-voting-proxy
                                    :pki-event/address (nth l 4)})
                       {:node/urbit-id (second l)
                        :node/voting-proxy (nth l 4)}]
       "owner"        [(merge base {:pki-event/type :change-ownership
                                    :pki-event/address (nth l 4)})
                       {:node/urbit-id (second l)
                        :node/ownership-address (nth l 4)}
                       `(network-explorer.main/inc-attr [:node/urbit-id ~(second l)] :node/num-owners 1)]
       "activated"    [(merge base {:pki-event/type :activate})]
       "spawned"      [(merge base {:pki-event/type :spawn
                                    :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}})]
       "invite"       [(merge base {:pki-event/type :invite
                                    :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}
                                    :pki-event/address (nth l 5)})]
       "sponsor"      (case (nth l 4)
                        "escaped to"    [(merge base {:pki-event/type :escaped
                                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 5)]}})
                                         {:node/urbit-id (second l)
                                          :node/sponsor {:db/id [:node/urbit-id (nth l 5)]}}]
                        "detached from" [(merge base {:pki-event/type :lost-sponsor
                                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 5)]}})
                                         [:db/retract [:node/urbit-id (second l)] :node/sponsor [:node/urbit-id (nth l 5)]]])
       "escape-req"   (case (nth l 4)
                        "canceled"   [(merge base {:pki-event/type :escape-canceled})]
                        [(merge base {:pki-event/type :escape-requested
                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}})]))]))

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
        limit (parse-long (get query-params :limit "1000"))
        offset  (parse-long (get query-params :offset "0"))
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
    :aggregate/day  (date->day value)
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
                   :pki-event/dominion
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
  ([limit offset since node-type db]
   (let [selector [:pki-event/id
                   {:pki-event/node [:node/urbit-id :node/type]}
                   {:pki-event/target-node [:node/urbit-id]}
                   :pki-event/dominion
                   :pki-event/type
                   :pki-event/time
                   :pki-event/address
                   :pki-event/continuity
                   :pki-event/revision]]
     (into [] (comp
               (filter (fn [e] (= node-type (:node/type (:pki-event/node e)))))
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
        limit     (parse-long (get query-params :limit "1000"))
        offset    (parse-long (get query-params :offset "0"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (if urbit-id
                             (get-pki-events urbit-id since db)
                             (if node-type
                               (get-all-pki-events limit offset since node-type db)
                               (get-all-pki-events limit offset since db)))
                           :value-fn stringify-date)}))

(defn add-zero-counts [dr query-res k]
  (loop [d dr
         q query-res
         res []]
    (if-not (seq d)
      res
      (if (= (first d) (:date (first q)))
        (recur (rest d) (rest q) (conj res (first q)))
        (recur (rest d) q (conj res {:date (first d) k 0}))))))

(defn get-aggregate-pki-events
  ([event-type since db]
   (get-aggregate-pki-events nil event-type since db))
  ([node-type event-type since db]
   (let [tomorrow (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1)
         dr       (map str (date-range since tomorrow))
         date     (java.util.Date/from (.toInstant (.atStartOfDay since java.time.ZoneOffset/UTC)))]
     (add-zero-counts
      dr
      (if node-type
        (d/q aggregate-query-since-node-type db event-type date node-type)
        (d/q aggregate-query-since db event-type date))
      :count))))

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
        limit    (parse-long (get query-params :limit "1000"))
        offset   (parse-long (get query-params :offset "0"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (if urbit-id
                             (get-activity urbit-id since db)
                             (get-all-activity limit offset db))
                           :value-fn stringify-date)}))

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
  (let [ds (d/q locked-query db)
        us (d/q unlocked-query db)
        dr (map str (date-range
                     (java.time.LocalDate/parse "2018-11-27")
                     (.plusDays (java.time.LocalDate/parse (:date (last us))) 1)))
        deposits  (add-zero-counts dr ds :count)
        unlocks   (add-zero-counts dr us :count)]
    (running-total
     :locked
     (map (fn [d u] (update u :count (partial - (:count d))))

          deposits unlocks))))

(defn add-zero-counts-2 [dr query-res k]
  (loop [d dr
         q query-res
         res []]
    (if-not (seq d)
      res
      (if (= (:aggregate/day (first d)) (:aggregate/day (first q)))
        (recur (rest d) (rest q) (conj res (first q)))
        (recur (rest d) q (conj res {:aggregate/day (:aggregate/day (first d)) k 0}))))))

(defn update-aggregate-status
  ([conn db]
   (update-aggregate-status conn db :all)
   (update-aggregate-status conn db :galaxy)
   (update-aggregate-status conn db :star)
   (update-aggregate-status conn db :planet))
  ([conn db node-type]
   (let [prev (ffirst (d/q '[:find (max ?d)
                             :in $ ?node-type
                             :where [?e :aggregate/day ?d]
                                    [?e :aggregate/node-type ?node-type]
                                    [?e :aggregate/spawned]
                                    [?e :aggregate/activated]
                                    [?e :aggregate/set-networking-keys]
                                    [?e :aggregate/online]]
                            db node-type))
         yesterday (-> prev
                       .toInstant
                       (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
                       (.minusDays 1)
                       .toInstant
                       java.util.Date/from)
         [s a k] (first (d/q '[:find ?s ?a ?k
                               :in $ ?d ?node-type
                               :where [?e :aggregate/day ?d]
                                      [?e :aggregate/node-type ?node-type]
                                      [?e :aggregate/spawned ?s]
                                      [?e :aggregate/activated ?a]
                                      [?e :aggregate/set-networking-keys ?k]
                                      [?e :aggregate/online]]
                             db yesterday node-type))
         spawned (if (= node-type :all)
                   (d/q '[:find ?date-s (count ?p)
                          :in $ ?since
                          :keys :aggregate/day :aggregate/spawned
                          :where [?p :pki-event/time ?t]
                                 [(>= ?t ?since)]
                                 [?p :pki-event/type :spawn]
                                 [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                        db prev)
                   (d/q '[:find ?date-s (count ?p)
                          :in $ ?since ?node-type
                          :keys :aggregate/day :aggregate/spawned
                          :where [?p :pki-event/time ?t]
                                 [(>= ?t ?since)]
                                 [?p :pki-event/target-node ?e]
                                 [?e :node/type ?node-type]
                                 [?p :pki-event/type :spawn]
                                 [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                        db prev node-type)
                   )
         activated (if (= node-type :all)
                     (d/q '[:find ?date-s (count ?p)
                            :in $ ?since
                            :keys :aggregate/day :aggregate/activated
                            :where [?p :pki-event/time ?t]
                                   [(>= ?t ?since)]
                                   [?p :pki-event/type :activate]
                                   [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                          db prev)
                     (d/q '[:find ?date-s (count ?p)
                            :in $ ?since ?node-type
                            :keys :aggregate/day :aggregate/activated
                            :where [?p :pki-event/time ?t]
                                   [(>= ?t ?since)]
                                   [?p :pki-event/node ?e]
                                   [?e :node/type ?node-type]
                                   [?p :pki-event/type :activate]
                                   [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                          db prev node-type)

                     )
         set-keys (if (= node-type :all)
                    (d/q '[:find ?date-s (count-distinct ?p)
                           :in $ ?since
                           :keys :aggregate/day :aggregate/set-networking-keys
                           :where [?s :pki-event/time ?t]
                                  [(>= ?t ?since)]
                                  (or (and [?s :pki-event/revision 1]
                                           [?s :pki-event/dominion :l1])
                                      (and [?s :pki-event/revision 2]
                                           [?s :pki-event/dominion :l2]))
                                  [?s :pki-event/type :change-networking-keys]
                                  [?s :pki-event/node ?p]
                                  [?s :pki-event/time ?t]
                                  [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                         db prev)
                    (d/q '[:find ?date-s (count-distinct ?p)
                           :in $ ?since ?node-type
                           :keys :aggregate/day :aggregate/set-networking-keys
                           :where [?s :pki-event/time ?t]
                                  [(>= ?t ?since)]
                                  (or (and [?s :pki-event/revision 1]
                                           [?s :pki-event/dominion :l1])
                                      (and [?s :pki-event/revision 2]
                                           [?s :pki-event/dominion :l2]))
                                  [?s :pki-event/type :change-networking-keys]
                                  [?s :pki-event/node ?p]
                                  [?p :node/type ?node-type]
                                  [?s :pki-event/time ?t]
                                  [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                         db prev node-type))
          online (if (= node-type :all)
                   (d/q '[:find ?date-s (count-distinct ?u)
                          :in $ ?since
                          :keys :aggregate/day :aggregate/online
                          :where [?e :ping/received ?t]
                                 [(>= ?t ?since)]
                                 [?e :ping/urbit-id ?u]
                                 [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                         db prev)
                   (d/q '[:find ?date-s (count-distinct ?u)
                          :in $ ?since ?node-type
                          :keys :aggregate/day :aggregate/online
                          :where [?e :ping/received ?t]
                                 [(>= ?t ?since)]
                                 [?e :ping/urbit-id ?u]
                                 [?u :node/type ?node-type]
                                 [(network-explorer.main/date->midnight-utc ?t) ?date-s]]
                        db prev node-type))]
     (d/transact
      conn
      {:tx-data
       (map merge (->>
                   (map (fn [{:keys [:aggregate/day :aggregate/spawned]}]
                          {:aggregate/day day :aggregate/spawned spawned
                           :aggregate/node-type node-type
                           :aggregate/day+node-type [day node-type]})
                        (reductions (fn [acc e]
                                      (update e :aggregate/spawned
                                              (fn [x] (+ x (:aggregate/spawned acc)))))
                                    {:aggregate/spawned s}
                                    (add-zero-counts-2 online spawned :aggregate/spawned)))
                   (drop 1))
            (->>
             (map (fn [{:keys [:aggregate/day :aggregate/activated]}]
                    {:aggregate/day day :aggregate/activated activated
                     :aggregate/node-type node-type
                     :aggregate/day+node-type [day node-type]})
                  (reductions (fn [acc e]
                                (update e :aggregate/activated
                                        (fn [x] (+ x (:aggregate/activated acc)))))
                              {:aggregate/activated a}
                              (add-zero-counts-2 online activated :aggregate/activated)))
             (drop 1))
            (->>
             (map (fn [{:keys [:aggregate/day :aggregate/set-networking-keys]}]
                    {:aggregate/day day :aggregate/set-networking-keys set-networking-keys
                     :aggregate/node-type node-type
                     :aggregate/day+node-type [day node-type]})
                  (reductions (fn [acc e]
                                (update e :aggregate/set-networking-keys
                                        (fn [x]
                                          (+ x (:aggregate/set-networking-keys acc)))))
                              {:aggregate/set-networking-keys k}
                              (add-zero-counts-2 online set-keys :aggregate/set-networking-keys)))
             (drop 1))
            (map (fn [{:keys [:aggregate/day :aggregate/online]}]
                   {:aggregate/day day :aggregate/online online
                    :aggregate/node-type node-type
                    :aggregate/day+node-type [day node-type]})
                 online))}))))

(defn get-aggregate-status
  ([db since until]
   (get-aggregate-status db since until :all))
  ([db since until node-type]
   (->> (d/q '[:find (pull ?e [:aggregate/day
                               :aggregate/spawned
                               :aggregate/activated
                               :aggregate/set-networking-keys
                               :aggregate/locked
                               :aggregate/online])
               :in $ ?since ?until ?node-type
               :where [?e :aggregate/node-type ?node-type]
                      [?e :aggregate/day ?d]
                      [?e :aggregate/spawned]
                      [(>= ?d ?since)]
                      [(>= ?until ?d)]]
             db since until node-type)
        (map first)
        (sort-by :aggregate/day))))

(defn update-kids-hashes
  ([conn db]
   (update-kids-hashes conn db :all)
   (update-kids-hashes conn db :galaxy)
   (update-kids-hashes conn db :star)
   (update-kids-hashes conn db :planet))
  ([conn db node-type]
   (let [prev     (ffirst (d/q '[:find (max ?d)
                                 :in $ ?node-type
                                 :where [?e :aggregate/day ?d]
                                        [?e :aggregate/node-type ?node-type]
                                        [?e :aggregate/kids-hashes]
                                 ] db node-type))
         ds       (if (= node-type :all)
                    (d/q '[:find ?e ?h ?r
                           :in $ ?since
                           :where [?p :ping/received ?r]
                                  [(>= ?r ?since)]
                                  [?p :ping/kids ?h]
                                  [?p :ping/urbit-id ?e]]
                         db prev)
                    (d/q '[:find ?e ?h ?r
                           :in $ ?node-type ?since
                           :where [?p :ping/received ?r]
                                  [(>= ?r ?since)]
                                  [?p :ping/urbit-id ?e]
                                  [?e :node/type ?node-type]
                                  [?p :ping/kids ?h]
                           ] db node-type prev))
         txs (->>  ds
                   (sort-by last)
                   (partition-by (comp date->day last))
                   (map (fn [e]
                          (let [day (-> (last (first e))
                                        .toInstant
                                        (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
                                        (.truncatedTo java.time.temporal.ChronoUnit/DAYS)
                                        .toInstant
                                        java.util.Date/from)]
                            {:aggregate/day day
                             :aggregate/node-type node-type
                             :aggregate/day+node-type [day node-type]
                             :aggregate/kids-hashes
                             (map (fn [[k v]]
                                    {:hash/kids-hash k
                                     :hash/count v
                                     :hash/day day
                                     :hash/node-type node-type
                                     :hash/kids-hash+day+node-type [k day node-type]})
                                  (frequencies
                                   (map (comp second last)
                                        (vals (group-by first e)))))}))))]
     (d/transact conn {:tx-data txs}))))

(defn get-kids-hashes
  ([db since until] (get-kids-hashes db since until :all))
  ([db since until node-type]
   (->> (d/q '[:find (pull ?e [:aggregate/day
                               {:aggregate/kids-hashes [:hash/kids-hash :hash/count]}])
               :in $ ?since ?until ?node-type
               :where [?e :aggregate/node-type ?node-type]
               [?e :aggregate/day ?d]
               [?e :aggregate/kids-hashes]
               [(>= ?d ?since)]
               [(>= ?until ?d)]]
             db since until node-type)
        (map first)
        (sort-by :aggregate/day))))

(defn get-online-stats
  ([db since until]
   (get-online-stats db since until :all))
  ([db since until node-type]
   (->> (d/q '[:find (pull ?e [:aggregate/day
                               :aggregate/new
                               :aggregate/churned
                               :aggregate/resurrected
                               :aggregate/retained])
               :in $ ?since ?until ?node-type
               :where [?e :aggregate/node-type ?node-type]
               [?e :aggregate/day ?d]
               [(>= ?d ?since)]
               [(>= ?until ?d)]
               [?e :aggregate/churned]]
             db since until node-type)
        (map first)
        (sort-by :aggregate/day))))

(defn update-online-stats
  ([conn db]
   (update-online-stats conn db :all)
   (update-online-stats conn db :galaxy)
   (update-online-stats conn db :star)
   (update-online-stats conn db :planet))
  ([conn db node-type]
   (let [prev (ffirst (d/q '[:find (max ?d)
                             :in $ ?node-type
                             :where [?e :aggregate/node-type ?node-type]
                                    [?e :aggregate/day ?d]
                                    [?e :aggregate/churned]
                             ]
                           db node-type))
         yesterday (-> prev
                       .toInstant
                       (java.time.ZonedDateTime/ofInstant java.time.ZoneOffset/UTC)
                       (.minusDays 1)
                       .toInstant
                       java.util.Date/from)
         seen-yes (if (= node-type :all)
                    (ffirst (d/q '[:find (distinct ?u)
                                   :in $ ?since ?until
                                   :where [?p :ping/received ?r]
                                   [(>= ?r ?since)]
                                   [(< ?r ?until)]
                                   [?p :ping/urbit-id ?e]
                                   [?e :node/urbit-id ?u]
                                   ] db yesterday prev))
                    (ffirst (d/q '[:find (distinct ?u)
                                   :in $ ?node-type ?since ?until
                                   :where [?p :ping/received ?r]
                                   [(>= ?r ?since)]
                                   [(< ?r ?until)]
                                   [?p :ping/urbit-id ?e]
                                   [?e :node/type ?node-type]
                                   [?e :node/urbit-id ?u]
                                   ] db node-type yesterday prev)))
         seen-ever (if (= node-type :all)
                     (ffirst (d/q {:query '[:find (distinct ?u)
                                            :in $ ?until
                                            :where [?p :ping/received ?r]
                                                   [(< ?r ?until)]
                                                   [?p :ping/urbit-id ?e]
                                                   [?e :node/urbit-id ?u]]
                                   :timeout 30000
                                   :args [db prev]}))
                     (ffirst (d/q {:query '[:find (distinct ?u)
                                            :in $ ?node-type ?until
                                            :where [?p :ping/received ?r]
                                            [(< ?r ?until)]
                                            [?p :ping/urbit-id ?e]
                                            [?e :node/urbit-id ?u]
                                            [?e :node/type ?node-type]
                                            ]
                                   :timeout 30000
                                   :args [db node-type prev]})))
         q  (if (= node-type :all)
                         (d/q '[:find ?u ?date-s
                                :in $ ?since
                                :where [?p :ping/received ?r]
                                       [(>= ?r ?since)]
                                       [?p :ping/urbit-id ?e]
                                       [?e :node/urbit-id ?u]
                                       [(network-explorer.main/date->midnight-utc ?r) ?date-s]
                                       ] db prev)
                         (d/q '[:find ?u ?date-s
                                :in $ ?node-type ?since
                                :where [?p :ping/received ?r]
                                       [(>= ?r ?since)]
                                       [?p :ping/urbit-id ?e]
                                       [?e :node/type ?node-type]
                                       [?e :node/urbit-id ?u]
                                       [(network-explorer.main/date->midnight-utc ?r) ?date-s]
                                ] db node-type prev))
         res  (->> q
                   (sort-by last)
                   (partition-by last)
                   (reductions
                    (fn [[stats acc] e]
                      (let [today (set (map first e))]
                        [(-> stats
                             (assoc :ever (clojure.set/union (:ever stats) today))
                             (assoc :yesterday today))
                         {:aggregate/node-type node-type
                          :aggregate/day (second (first e))
                          :aggregate/day+node-type [(second (first e)) node-type]
                          :aggregate/new (count (clojure.set/difference today (:ever stats)))
                          :aggregate/churned (count (clojure.set/difference (:yesterday stats) today))
                          :aggregate/retained (count (clojure.set/intersection (:yesterday stats) today))
                          :aggregate/resurrected (count (clojure.set/difference
                                               (clojure.set/intersection (:ever stats) today)
                                               (:yesterday stats)))}]))
                    [{:ever (set seen-ever) :yesterday (set seen-yes)}])
                   (drop 1)
                   (map second))]
     (d/transact conn {:tx-data res}))))

(defn update-data [_]
  (let [;; pki-event/id is just line index, historic is the index of the last
        ;; pki event from the previous events.txt file, currently
        ;; https://gaze-exports.s3.us-east-2.amazonaws.com/events-l2.txt
        historic   450141
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
    (pr-str (count pki-txs))))

(defn update-aggregates [conn db]
  (update-online-stats conn db)
  (update-kids-hashes conn db)
  (pr-str (count (update-aggregate-status conn db))))

(defn update-radar-data [_]
  (let [historic 628892
        client (get-client)
        conn   (d/connect client {:db-name "network-explorer-2"})
        db     (d/db conn)
        pings  (- (ffirst (d/q '[:find (count ?e) :where [?e :ping/received]] db)) historic)
        data   (->> (get-radar-data)
                    str/split-lines
                    (drop 1)
                    (map (fn [l] (str/split l #",")))
                    (filter (fn [[_ _ p _]] (#{:galaxy :star :planet} (ob/clan p))))
                    (drop-last pings)
                    (mapcat radar-data->txs)
                    reverse)]
    (if (empty? data)
      "nothing to update"
      (update-aggregates
       conn
       (:db-after
        (loop [txs data]
          (if (= (count txs) 1)
            (d/transact conn {:tx-data [(first txs)]})
            (do (d/transact conn {:tx-data [(first txs)]})
              (recur (rest txs))))))))))

(defn get-aggregate-status* [query-params db]
  (let [node-type (keyword (get query-params :nodeType))
        since     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :since "2018-11-27"))
        until     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :until "3000-01-01"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (if node-type
              (get-aggregate-status db since until node-type)
              (get-aggregate-status db since until))
            :value-fn stringify-date)}))

(defn get-kids-hashes* [query-params db]
  (let [node-type (keyword (get query-params :nodeType))
        since     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :since "2018-11-27"))
        until     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :until "3000-01-01"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (if node-type
              (get-kids-hashes db since until node-type)
              (get-kids-hashes db since until)) :value-fn stringify-date)}))


(defn get-online-stats* [query-params db]
  (let [node-type (keyword (get query-params :nodeType))
        since     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :since "2018-11-27"))
        until     (.parse (doto (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
                          (get query-params :until "3000-01-01"))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (if node-type
              (get-online-stats db since until node-type)
              (get-online-stats db since until)) :value-fn stringify-date)}))

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
      "/get-kids-hashes"          (get-kids-hashes* query-params db)
      "/get-online-stats"         (get-online-stats* query-params db)
      {:status 404})))

(def app-handler
  (-> root-handler
      kwparams/wrap-keyword-params
      params/wrap-params))

;; (def conn (d/connect (get-client) {:db-name "network-explorer-2"}))
