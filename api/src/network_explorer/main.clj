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

(def kids-hashes (atom {:all [] :planet [] :star [] :galaxy []}))

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
  ([limit offset since type db]
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

(defn get-aggregate-status
  ([_]
   (let [conn          (d/connect (get-client) {:db-name "network-explorer-2"})
         db            (d/db conn)
         tomorrow      (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1)
         azimuth-start (map str (date-range (java.time.LocalDate/parse "2018-11-27") tomorrow))
         online-start  (map str (date-range (java.time.LocalDate/parse "2022-06-03") tomorrow))]
     (map merge
          (running-total
           :set-networking-keys
           (add-zero-counts
            azimuth-start
            (d/q set-networking-keys-query db)
            :count))
          (running-total
           :spawned
           (add-zero-counts
            azimuth-start
            (d/q aggregate-query db :spawn)
            :count))
          (running-total
           :activated
           (add-zero-counts
            azimuth-start
            (d/q aggregate-query db :activate)
            :count))
          (concat (repeat 1284 {})
                  (conj (pop (add-zero-counts online-start (d/q online-query db) :online)) {}))
          (get-locked-aggregate db))))
  ([node-type _]
   (let [conn          (d/connect (get-client) {:db-name "network-explorer-2"})
         db            (d/db conn)
         tomorrow      (.plusDays (java.time.LocalDate/now java.time.ZoneOffset/UTC) 1)
         azimuth-start (map str (date-range (java.time.LocalDate/parse "2018-11-27") tomorrow))
         online-start  (map str (date-range (java.time.LocalDate/parse "2022-06-03") tomorrow))
         locked        (if (= :star node-type) (get-locked-aggregate db) (repeat {}))
         spawned       (if (= :galaxy node-type) (repeat {:spawned 256})
                           (running-total
                            :spawned
                            (add-zero-counts
                             azimuth-start
                             (d/q spawned-query-node-type db node-type)
                             :count)))
         set-keys      (running-total
                        :set-networking-keys
                        (add-zero-counts
                         azimuth-start
                         (d/q set-networking-keys-query-node-type db node-type)
                         :count))]
     (concat (map merge
                  spawned
                  (running-total
                   :activated
                   (add-zero-counts
                    azimuth-start
                    (d/q aggregate-query-node-type db :activate node-type)
                    :count))
                  set-keys
                  (concat (repeat 1284 {})
                          (conj (pop (add-zero-counts
                                      online-start
                                      (d/q online-query-node-type db node-type)
                                      :online))
                                {}))
                  locked)
             (when (= :star node-type) (drop (count set-keys) locked))))))

(defn get-kids-hashes
  ([db] (get-kids-hashes db :all))
  ([db node-type]
   (let [ds  (if (= node-type :all)
               (d/q '[:find ?e ?h ?r
                      :where [?p :ping/kids ?h]
                      [?p :ping/urbit-id ?e]
                      [?p :ping/received ?r]] db)
               (d/q '[:find ?e ?h ?r
                      :in $ ?node-type
                      :where [?e :node/type ?node-type]
                      [?p :ping/urbit-id ?e]
                      [?p :ping/kids ?h]
                      [?p :ping/received ?r]] db node-type))]
     (->>  ds
           (sort-by last)
           (partition-by (comp date->day last))
           (map (fn [e] (assoc (frequencies
                                (map (comp second last)
                                     (vals (group-by first e))))
                               :date (date->day (last (first e))))))))))

(defn get-kids-hashes-memoized
  ([db]
   (get-kids-hashes-memoized db :all))
  ([db node-type]
   (if (not-empty (node-type @kids-hashes))
     (node-type @kids-hashes)
     (let [res (get-kids-hashes db node-type)]
       (swap! kids-hashes assoc node-type res)
       res))))

(def get-aggregate-status-memoized
  (memo/fifo get-aggregate-status :fifo/threshold 4))

(defn refresh-aggregate-cache [db]
  (let [latest-tx  (ffirst (d/q '[:find (max 1 ?tx)
                                  :where [_ :pki-event/time ?tx]]
                                db))]
    (get-aggregate-status-memoized latest-tx)
    (get-aggregate-status-memoized :galaxy latest-tx)
    (get-aggregate-status-memoized :star latest-tx)
    (get-aggregate-status-memoized :planet latest-tx)
    (get-kids-hashes-memoized db :all)
    (get-kids-hashes-memoized db :planet)
    (get-kids-hashes-memoized db :star)
    (get-kids-hashes-memoized db :galaxy)))


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
    (doseq [txs (drop-last 1 pki-txs)]
      (d/transact conn {:tx-data txs}))
    (when-not (empty? pki-txs)
      (refresh-aggregate-cache (:db-after (d/transact conn {:tx-data (last pki-txs)}))))
    (pr-str (count pki-txs))))

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
    (doseq [tx (drop-last 1 data)]
      (d/transact conn {:tx-data [tx]}))
    (memo/memo-clear! get-aggregate-status-memoized)
    (reset! kids-hashes {:all [] :planet [] :star [] :galaxy []})
    (refresh-aggregate-cache (:db-after (d/transact conn {:tx-data [(last data)]})))
    (pr-str (count data))))



(defn get-aggregate-status* [query-params db]
  (let [node-type (keyword (get query-params :nodeType))
        since     (java.time.LocalDate/parse (get query-params :since "2018-11-27"))
        until     (java.time.LocalDate/parse (get query-params :until "3000-01-01"))
        latest-tx (ffirst (d/q '[:find (max 1 ?tx) :where [_ :pki-event/time ?tx]] db))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (take-while
             (fn [e] (.isBefore (java.time.LocalDate/parse (:date e)) until))
             (drop-while
              (fn [e] (.isBefore (java.time.LocalDate/parse (:date e)) since))
              (if node-type
                (get-aggregate-status-memoized node-type latest-tx)
                (get-aggregate-status-memoized latest-tx))))
            :value-fn stringify-date)}))

(defn get-kids-hashes* [query-params]
  (let [node-type (keyword (get query-params :nodeType))
        since     (java.time.LocalDate/parse (get query-params :since "2018-11-27"))
        until     (java.time.LocalDate/parse (get query-params :until "3000-01-01"))
        conn      (d/connect (get-client) {:db-name "network-explorer-2"})
        db        (d/db conn)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            (take-while
             (fn [e] (.isBefore (java.time.LocalDate/parse (:date e)) until))
             (drop-while
              (fn [e] (.isBefore (java.time.LocalDate/parse (:date e)) since))
              (if node-type
                (get-kids-hashes-memoized db node-type)
                (get-kids-hashes-memoized db)))))}))

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
      "/get-kids-hashes"          (get-kids-hashes* query-params)
      {:status 404})))

(def app-handler
  (-> root-handler
      kwparams/wrap-keyword-params
      params/wrap-params))

;; (def conn (d/connect (get-client) {:db-name "network-explorer-2"}))
