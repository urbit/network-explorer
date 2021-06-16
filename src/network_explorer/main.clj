(ns network-explorer.main
  (:require [datomic.client.api :as d]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [network-explorer.ob :as ob]
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

(def ownership-query
  '[:find (pull ?e [:pki-event/address])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
    [?e :pki-event/node ?p]
    [?e :pki-event/type :change-ownership]
    [(q '[:find (max ?t)
          :in $ ?urbit-id
          :where
          [?p :node/urbit-id ?urbit-id]
          [?e :pki-event/node ?p]
          [?e :pki-event/type :change-ownership]
          [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
    [?e :pki-event/time ?newest]])

(def management-proxy-query
  '[:find (pull ?e [:pki-event/address])
    :in $ ?urbit-id
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

(def voting-proxy-query
  '[:find (pull ?e [:pki-event/address])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-voting-proxy]
           [(q '[:find (max ?t)
                 :in $ ?urbit-id
                 :where
                 [?p :node/urbit-id ?urbit-id]
                 [?e :pki-event/node ?p]
                 [?e :pki-event/type :change-voting-proxy]
                 [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
           [?e :pki-event/time ?newest]])

(def transfer-proxy-query
  '[:find (pull ?e [:pki-event/address])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-transfer-proxy]
           [(q '[:find (max ?t)
                 :in $ ?urbit-id
                 :where
                 [?p :node/urbit-id ?urbit-id]
                 [?e :pki-event/node ?p]
                 [?e :pki-event/type :change-transfer-proxy]
                 [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
           [?e :pki-event/time ?newest]])

(def spawn-proxy-query
  '[:find (pull ?e [:pki-event/address]) ?newest
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-spawn-proxy]
           [(q '[:find (max ?t)
                 :in $ ?urbit-id
                 :where
                 [?p :node/urbit-id ?urbit-id]
                 [?e :pki-event/node ?p]
                 [?e :pki-event/type :change-spawn-proxy]
                 [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
           [?e :pki-event/time ?newest]])

(def sponsor-query
  '[:find (pull ?e [:pki-event/type]) (pull ?s [:node/urbit-id])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           (or [?e :pki-event/type :lost-sponsor]
               [?e :pki-event/type :escaped])
           [?e :pki-event/target-node ?s]
           [(q '[:find (max ?t)
                 :in $ ?urbit-id
                 :where
                 [?p :node/urbit-id ?urbit-id]
                 [?e :pki-event/node ?p]
                 (or [?e :pki-event/type :lost-sponsor]
                     [?e :pki-event/type :escaped])
                 [?e :pki-event/time ?t]] $ ?urbit-id) [[?newest]]]
           [?e :pki-event/time ?newest]])

(def owners-query
  '[:find (count ?e)
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-ownership]])

(def revision-query
  '[:find (max ?r)
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :change-networking-keys]
           [?e :pki-event/revision ?r]])

(def continuity-query
  '[:find (max ?c)
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :broke-continuity]
           [?e :pki-event/continuity ?c]])

(def spawned-query
  '[:find (pull ?e [*])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/target-node ?p]
           [?e :pki-event/type :spawn]])

(def activated-query
  '[:find (pull ?e [*])
    :in $ ?urbit-id
    :where [?p :node/urbit-id ?urbit-id]
           [?e :pki-event/node ?p]
           [?e :pki-event/type :activate]])

(def urbit-id-query
  '[:find (pull ?e [:node/urbit-id
                    :node/type
                    [:node/online :default false]
                    {:node/sponsor [:node/urbit-id]}
                    {[:node/_sponsor :as :node/kids :default []] [:node/urbit-id]}])
    :in $ ?urbit-id
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
  (let [pki [(merge {:pki-event/id   idx
                     :pki-event/time (parse-pki-time (first l))
                     :pki-event/node {:db/id [:node/urbit-id (second l)]}}
                    (case (nth l 2)
                      "keys"         {:pki-event/type :change-networking-keys
                                      :pki-event/revision (Long/valueOf (nth l 3))}
                      "breached"     {:pki-event/type :broke-continuity
                                      :pki-event/continuity (Long/valueOf (nth l 3))}
                      "management-p" {:pki-event/type :change-management-proxy
                                      :pki-event/address (nth l 3)}
                      "transfer-p"   {:pki-event/type :change-transfer-proxy
                                      :pki-event/address (nth l 3)}
                      "spawn-p"      {:pki-event/type :change-spawn-proxy
                                      :pki-event/address (nth l 3)}
                      "voting-p"     {:pki-event/type :change-voting-proxy
                                      :pki-event/address (nth l 3)}
                      "owner"        {:pki-event/type :change-ownership
                                      :pki-event/address (nth l 3)}
                      "activated"    {:pki-event/type :activate}
                      "spawned"      {:pki-event/type :spawn
                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}}
                      "invite"       {:pki-event/type :invite
                                      :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}
                                      :pki-event/address (nth l 4)}
                      "sponsor"      (case (nth l 3)
                                       "escaped to"    {:pki-event/type :escaped
                                                        :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}}
                                       "detached from" {:pki-event/type :lost-sponsor
                                                        :pki-event/target-node {:db/id [:node/urbit-id (nth l 4)]}})
                      "escape-req"   (case (nth l 3)
                                       "canceled"   {:pki-event/type :escape-canceled}
                                       {:pki-event/type :escape-requested
                                        :pki-event/target-node {:db/id [:node/urbit-id (nth l 3)]}})))]]
    (case (nth l 2)
      "sponsor"
      (case (nth l 3)
        "escaped to" (conj pki [:db/add [:node/urbit-id (second l)] :node/sponsor [:node/urbit-id (nth l 4)]])
        "detached from" (conj pki [:db/retract [:node/urbit-id (second l)] :node/sponsor [:node/urbit-id (nth l 4)]]))
      pki)))

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

(defn get-node [{:keys [queryStringParameters]}]
  (let [client (get-client)
        conn (d/connect client {:db-name "network-explorer"})
        db (d/db conn)
        urbit-id (get queryStringParameters "urbit-id")]
    (if-not (ob/patp? urbit-id)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Invalid urbit-id"})}
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str (ffirst (d/q urbit-id-query db (get queryStringParameters "urbit-id"))))})))

(def get-node-lambda-proxy
  (apigw/ionize get-node))
