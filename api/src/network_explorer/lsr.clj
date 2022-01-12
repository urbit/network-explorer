(ns network-explorer.lsr
  (:require [clj-ob.ob :as ob]
            [clojure.data.json :as json]
            [hato.client :as http]))

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
      ;; etherscan api rate limit
      (Thread/sleep 1000)
      (if (< (count res) 10000)
        (concat r res)
        (recur (concat r (take-while (fn [e] (not= block (get e "blockNumber"))) res)) block)))))
