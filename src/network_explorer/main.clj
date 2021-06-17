(ns network-explorer.main
  (:require [datomic.client.api :as d]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [network-explorer.ob :as ob]
            [datomic.ion.cast :as cast]
            [datomic.ion.lambda.api-gateway :as apigw]))

(def cfg {:server-type :ion
          :region "us-west-2" ;; e.g. us-east-1
          :system "datomic-storage"
          :endpoint "http://entry.datomic-storage.us-west-2.datomic.net:8182/"
          :proxy-port 8182})


;; (def conn (d/connect client {:db-name "network-explorer"}))

(defn get-radar-data []
  (-> (http/get "http://35.247.74.19:8080/~radar.json")
      :body
      json/read-str))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn radar-data->txs [data]
  (->> (map-kv data first)
       (map (fn [[k v]]
              (merge
               {:node/urbit-id k
                :node/point    (ob/patp->biginteger k)
                :node/sponsor  {:db/id [:node/urbit-id (ob/sein k)]}
                :node/type     (ob/clan k)
                :node/online   (boolean (get v "response"))}
               (when (get v "ping")
                 {:node/ping-time (-> (get v "ping")
                                      java.time.Instant/ofEpochMilli
                                      java.util.Date/from)})
               (when (get v "response")
                 {:node/response-time (-> (get v "response")
                                          java.time.Instant/ofEpochMilli
                                          java.util.Date/from)}))))))
(defn get-pki-data []
  (:body (http/get "https://azimuth.network/stats/events.txt" {:insecure? true})))

(defn parse-pki-time [s]
  (.parse
   (doto (java.text.SimpleDateFormat. "~yyyy.MM.dd..HH.mm.ss")
     (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
   s))

(defn inc-attr
  "Transaction function that increments the value of entity's card-1
attr by amount, treating a missing value as 1."
  [db entity attr amount]
  (let [m (d/pull db {:eid entity :selector [:db/id attr]})]
    [[:db/add (:db/id m) attr (+ (or (attr m) 1) amount)]]))

(def management-proxy-query
  '[:find (pull ?e [:pki-event/address])
    :in $ [?urbit-id ...]
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-management-proxy]
           [(q '[:find (max ?t)
                 :in $ ?urbit-id
                 :where
                 [?p :node/urbit-id ?urbit-id]
                 [?e :pki-event/node ?p]
                 [?e :pki-event/type :change-management-proxy]
                 [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
           [?e :pki-event/time ?newest]])

(def owners-query
  '[:find (count ?e)
    :in $ [?urbit-id ...]
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-ownership]])

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
                    {:node/sponsor [:node/urbit-id]}
                    {[:node/_sponsor :as :node/kids :default []] [:node/urbit-id]}])
    :where [?e :node/urbit-id]])

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
                    {:node/sponsor [:node/urbit-id]}
                    {[:node/_sponsor :as :node/kids :default []] [:node/urbit-id]}])
    :in $ [?urbit-id ...]
    :where [?e :node/urbit-id ?urbit-id]])

(defn set-lookup-ref [nodes-in-db urbit-id]
  (if (contains? nodes-in-db urbit-id)
    {:db/id [:node/urbit-id urbit-id]}
    {:db/id urbit-id}))

(defn pki-line->nodes [acc l]
  (->> (filter ob/patp? l)
       (apply conj acc)))

(defn node->node-tx [s]
  (merge {:node/urbit-id  s
          :node/point     (ob/patp->biginteger s)
          :node/type      (ob/clan s)}
         (when-not (= :galaxy (ob/clan s))
           {:node/sponsor {:db/id [:node/urbit-id (ob/sein s)]}})))

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
                       `(network-explorer.main/inc-attr ~(second l) :node/num-owners 1)]
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

(def get-client (memoize (fn [] (d/client cfg))))

(defn get-all-nodes [limit offset db]
  (mapcat identity (d/q {:query all-urbit-ids-query
                         :args [db]
                         :limit limit
                         :offset offset})))

(defn get-nodes [urbit-ids limit offset db]
  (mapcat identity (d/q {:query urbit-ids-query
                         :args [db urbit-ids]
                         :limit limit
                         :offset offset})))

(defn get-node* [{:keys [datomic.ion.edn.api-gateway/data]}]
  (let [client (get-client)
        conn (d/connect client {:db-name "network-explorer"})
        db (d/db conn)
        urbit-ids (str/split #"," (get-in data [:queryStringParameters :urbit-id]))
        limit (Integer/parseInt (get (get data :queryStringParameters) :limit "1000"))
        offset  (Integer/parseInt (get (get data :queryStringParameters) :offset "0"))]
    (if-not (every? ob/patp? urbit-ids)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "One or more invalid urbit-ids"})}
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str (if (empty? urbit-ids)
                               (get-all-nodes limit offset db)
                               (get-nodes urbit-ids limit offset db)))})))

(def get-node-lambda-proxy
  (apigw/ionize get-node*))
