(ns network-explorer.schema)

(def node-schema
  [{:db/ident :node/urbit-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The @p of the node"}

   {:db/ident :node/sponsor
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The sponsor of the node, a ref to another node"}

   {:db/ident :node/status
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The status of the node, one of :locked, :unlocked, :spawned or :activated"}

   {:db/ident :node/point
    :db/valueType :db.type/bigint
    :db/cardinality :db.cardinality/one
    :db/doc "The numerical point of the node"}

   {:db/ident :node/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The type of the node, one of :galaxy, :star, :planet, :comet or :moon"}

   {:db/ident :node/continuity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The continuity of the node"}

   {:db/ident :node/revision
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The revision number of the node"}

   {:db/ident :node/num-owners
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The number of owners of the node"}

   {:db/ident :node/ownership-address
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The ownership address address of the node"}

   {:db/ident :node/spawn-proxy
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The spawn proxy address of the node"}

   {:db/ident :node/transfer-proxy
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The transfer proxy address of the node"}

   {:db/ident :node/management-proxy
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The management proxy address of the node"}

   {:db/ident :node/voting-proxy
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The voting proxy address of the node"}

   {:db/ident :node/online
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether a node is online or not"}

   {:db/ident :node/response-time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The radar response time of the node"}

   {:db/ident :node/ping-time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the node was pinged"}
   ])

(def pki-event-schema
  [{:db/ident :pki-event/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The increasing integer number of the pki event"}

   {:db/ident :pki-event/node
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The node that the pki event concerns"}

   {:db/ident :pki-event/target-node
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Target node for spawns, escapes and invites"}

   {:db/ident :pki-event/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The pki event type, one of :change-networking-keys :change-ownership, :change-spawn-proxy, :change-transfer-proxy :change-management-proxy, :change-voting-proxy, :activate, :invite, :spawn, :escape-requested, :escape-canceled,
:lost-sponsor or :broke-continuity"}

   {:db/ident :pki-event/time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the pki event happened"}

   {:db/ident :pki-event/address
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The address of the pki event"}

   {:db/ident :pki-event/continuity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The continuity number of the pki event"}

   {:db/ident :pki-event/revision
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The revision number of the pki event"}])

(def ping-schema
  [{:db/ident :ping/time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp when the ping was sent"}
   {:db/ident :ping/response
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp when the ping response was received"}
   {:db/ident :ping/urbit-id
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The urbit-id that was pinged"}
   {:db/ident :ping/result
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The base hash of the pinged ship"}
   {:db/ident :ping/time+urbit-id
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:ping/time :ping/urbit-id]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Composite of the ping send time and urbit-id for uniqueness constraint"}])
