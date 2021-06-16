(ns network-explorer.ob
  (:require [clojure.string :as str])
  (:import [org.apache.commons.codec.digest MurmurHash3]
           [com.google.common.hash Hashing]))

(def ux_1_0000 (BigInteger. "10000" 16))
(def ux_ffff_ffff (BigInteger. "ffffffff" 16))
(def ux_1_0000_0000 (BigInteger. "100000000" 16))
(def ux_ffff_ffff_ffff_ffff (BigInteger. "ffffffffffffffff" 16))
(def ux_ffff_ffff_0000_0000 (BigInteger. "ffffffff00000000" 16))

(def u_65535 (BigInteger. "65535"))
(def u_65536 (BigInteger. "65536"))

(def ux_ff (BigInteger. "ff" 16))
(def ux_ff00 (BigInteger. "ff00" 16))
(def u_256 (BigInteger. "256"))

(def pre "dozmarbinwansamlitsighidfidlissogdirwacsabwissibrigsoldopmodfoglidhopdardorlorhodfolrintogsilmirholpaslacrovlivdalsatlibtabhanticpidtorbolfosdotlosdilforpilramtirwintadbicdifrocwidbisdasmidloprilnardapmolsanlocnovsitnidtipsicropwitnatpanminritpodmottamtolsavposnapnopsomfinfonbanmorworsipronnorbotwicsocwatdolmagpicdavbidbaltimtasmalligsivtagpadsaldivdactansidfabtarmonranniswolmispallasdismaprabtobrollatlonnodnavfignomnibpagsopralbilhaddocridmocpacravripfaltodtiltinhapmicfanpattaclabmogsimsonpinlomrictapfirhasbosbatpochactidhavsaplindibhosdabbitbarracparloddosbortochilmactomdigfilfasmithobharmighinradmashalraglagfadtopmophabnilnosmilfopfamdatnoldinhatnacrisfotribhocnimlarfitwalrapsarnalmoslandondanladdovrivbacpollaptalpitnambonrostonfodponsovnocsorlavmatmipfip")

(def suf "zodnecbudwessevpersutletfulpensytdurwepserwylsunrypsyxdyrnuphebpeglupdepdysputlughecryttyvsydnexlunmeplutseppesdelsulpedtemledtulmetwenbynhexfebpyldulhetmevruttylwydtepbesdexsefwycburderneppurrysrebdennutsubpetrulsynregtydsupsemwynrecmegnetsecmulnymtevwebsummutnyxrextebfushepbenmuswyxsymselrucdecwexsyrwetdylmynmesdetbetbeltuxtugmyrpelsyptermebsetdutdegtexsurfeltudnuxruxrenwytnubmedlytdusnebrumtynseglyxpunresredfunrevrefmectedrusbexlebduxrynnumpyxrygryxfeptyrtustyclegnemfermertenlusnussyltecmexpubrymtucfyllepdebbermughuttunbylsudpemdevlurdefbusbeprunmelpexdytbyttyplevmylwedducfurfexnulluclennerlexrupnedlecrydlydfenwelnydhusrelrudneshesfetdesretdunlernyrsebhulrylludremlysfynwerrycsugnysnyllyndyndemluxfedsedbecmunlyrtesmudnytbyrsenwegfyrmurtelreptegpecnelnevfes")

(def prefixes (map (fn [cs] (apply str cs)) (partition 3 pre)))
(def suffixes (map (fn [cs] (apply str cs)) (partition 3 suf)))

(def prefixes-map (into {} (map-indexed (fn [idx s] [s idx]) prefixes)))
(def suffixes-map (into {} (map-indexed (fn [idx s] [s idx]) suffixes)))

(defn fen [r a b f m]
  (let [ahh (if (not= 0 (mod r 2)) (.divide (biginteger m) (biginteger a)) (mod m a))
        ale (if (not= 0 (mod r 2)) (mod m a) (.divide (biginteger m) (biginteger a)))
        L (if (= ale a) ahh ale)
        R (if (= ale a) ale ahh)]
    (loop [j r
           ell L
           arr R]
      (if (< j 1)
        (+ (* a arr) ell)
        (let [eff (f (dec j) ell)
              tmp (if (not= 0 (mod j 2))
                    (mod (- (+ arr a) (mod eff a)) a)
                    (mod (- (+ arr b) (mod eff b)) b))]
          (recur (dec j) tmp ell))))))

(defn muk [syd len key]
  (let [lo (.and (biginteger key) ux_ff)
        hi (.divide (.and (biginteger key) ux_ff00) u_256)
        kee (byte-array [lo hi])]
    #_(MurmurHash3/hash32x86 kee 0 len syd)
    (Integer/toUnsignedLong (.asInt (.hashBytes (Hashing/murmur3_32 syd) kee 0 len)))))

(defn F [j arg]
  (let [raku (map unchecked-int [0xb76d5eed 0xee281300 0x85bcae01 0x4b387af7])]
    (muk (nth raku j) 2 arg)))

(defn tail [bi]
  (let [Fen (fn [r, a, b, k, f, m]
              (let [c (fen r a b f m)]
                (if (< c k)
                  c
                  (fen r a b f c))))]
    (Fen 4 u_65535 u_65536 ux_ffff_ffff F bi)))

(defn fynd [bi]
  (let [lo (.and bi ux_ffff_ffff)
        hi (.and bi ux_ffff_ffff_0000_0000)]
    (if (and (>= bi ux_1_0000) (<= bi ux_ffff_ffff))
      (+ ux_1_0000 (tail (- bi ux_1_0000)))
      (if (and (>= bi ux_1_0000_0000) (<= bi ux_ffff_ffff_ffff_ffff))
        (.or (biginteger hi) (biginteger (fynd lo)))
        bi))))

(defn syl->bin [idx]
  (clojure.pprint/cl-format nil "~8,'0b" idx))

(defn patp->syls [name]
  (map (fn [cs] (apply str cs)) (partition 3 (str/replace name #"[\^~-]" ""))))

(defn patp->biginteger [name]
  (let [syls (vec (patp->syls name))
        addr (reduce-kv (fn [acc idx syl]
                          (if (or (odd? idx) (= 1 (count syls)))
                            (str acc (syl->bin (get suffixes-map syl)))
                            (str acc (syl->bin (get prefixes-map syl)))
                            )) "" syls)
        bn (BigInteger. addr 2)]
    (fynd bn)))

(defn bex [n]
  (Math/pow 2 n))

(defn rsh [a b c]
  (.divide (biginteger c) (biginteger (bex (* (bex a) b)))))

(defn met
  ([a b]
   (met a b 0))
  ([a b c]
   (if (= b 0)
     c
     (recur a (rsh a 1 b) (inc c)))))

(defn end [a b c]
  (mod c (bex (* (bex a) b))))

(defn clan [name]
  (let [p (patp->biginteger name)
        wid (met 3 p)]
    (cond
      (<= wid 1) :galaxy
      (= wid 2)  :star
      (<= wid 4) :planet
      (<= wid 8) :moon
      :else      :comet)))

(defn fe [r a b f m]
  (let [L (mod m a)
        R (.divide (biginteger m) (biginteger a))]
    (loop [j 1
           ell L
           arr R]
      (if (> j r)
        (cond
          (odd? r)  (+ (* a arr) ell)
          (= arr a) (+ (* a arr) ell)
          :else     (+ (* a ell) arr))
        (let [eff (f (dec j) arr)
              tmp (if (odd? j)
                    (mod (+ ell eff) a)
                    (mod (+ ell eff) b))]
          (recur (inc j) arr tmp))))))

(defn Fe [r a b k f m]
  (let [c (fe r a b f m)]
    (if (< c k)
      c
      (fe r a b f c))))

(defn feis [bn]
  (Fe 4 u_65535 u_65536 ux_ffff_ffff F bn))

(defn fein [bn]
  (let [lo (.and bn ux_ffff_ffff)
        hi (.and bn ux_ffff_ffff_0000_0000)]
    (cond
      (and (>= bn ux_1_0000)      (<= bn ux_ffff_ffff)) (+ ux_1_0000 (feis (- bn ux_1_0000)))
      (and (>= bn ux_1_0000_0000) (<= bn ux_ffff_ffff_ffff_ffff)) (.or hi (fein lo))
      :else                       bn)))

(defn biginteger->patp [bn]
  (let [sxz (fein (biginteger bn))
        dyy (met 4 sxz)
        dyx (met 3 sxz)]
    (str "~"
         (if (<= dyx 1)
           (nth suffixes sxz)
           (loop [tsxz sxz
                  timp 0
                  trep ""]
             (let [log (end 4 1 tsxz)
                   pre (nth prefixes (rsh 3 1 log))
                   suf (nth suffixes (end 3 1 log))
                   etc (if (zero? (mod timp 4))
                         (if (zero? timp) "" "--")
                         "-")
                   res (str pre suf etc trep)]
               (if (= timp dyy)
                 trep
                 (recur (rsh 4 1 tsxz) (inc timp) res))))))))

(defn sein [name]
  (let [who (patp->biginteger name)
        mir (clan name)]
    (biginteger->patp
     (case mir
       :galaxy who
       :star   (end 3 1 who)
       :planet (end 4 1 who)
       :moon   (end 5 1 who)
               0))))

(defn patp? [s]
  (try (patp->biginteger s)
       true
       (catch Exception _ false)))
