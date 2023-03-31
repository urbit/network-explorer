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

   {:db/ident :node/kids-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The has of the kids desk of the node"}])

(def pki-event-schema
  [{:db/ident :pki-event/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The increasing integer number of the pki event, line index from azimuth.txt"}

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
    :db/doc "The pki event type, one of :change-networking-keys :change-ownership, :change-spawn-proxy, :change-transfer-proxy :change-management-proxy, :change-voting-proxy, :activate, :invite, :spawn, :escape-requested, :escape-canceled, :escaped, :lost-sponsor or :broke-continuity"}

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
    :db/doc "The revision number of the pki event"}

   {:db/ident :pki-event/dominion
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The dominion of the pki event, one of :l1 or :l2"}])

(def ping-schema
  [{:db/ident :ping/sent
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of when the ping was sent"}
   {:db/ident :ping/received
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of when the ping was received"}
   {:db/ident :ping/urbit-id
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The urbit-id that was pinged"}
   {:db/ident :ping/kids
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The has of the kids desk of the ship that was pinged"}])

(def lsr-schema
  [{:db/ident :lsr/address
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The address that is allowed to withdraw the star from lockup at deposit time, does not mutate on transferBatch"}
   {:db/ident :lsr/star
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The star that is locked up, points to a node"}
   {:db/ident :lsr/deposited-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of when the star was deposited to the lockup contract"}
   {:db/ident :lsr/unlocked-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of when the star was unlocked from the lockup contract"}
   {:db/ident :lsr/withdrawn-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of when the star was withdrawn from the lockup contract"}
   {:db/ident :lsr/star+deposited-at
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:lsr/star :lsr/deposited-at]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Composite of the star and deposit timestamp for uniqueness constraint"}])

(def aggregate-schema
  [{:db/ident :aggregate/day
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The day of aggregation, timestamp at midnight UTC"}
   {:db/ident :aggregate/node-type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The node-type of the aggregate, one of :all, :galaxy, :star or :planet"}
   {:db/ident :aggregate/day+node-type
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:aggregate/day :aggregate/node-type]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Composite of day and node-type for uniqueness constraint"}
   {:db/ident :aggregate/new
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, new ships online on this day"}
   {:db/ident :aggregate/churned
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were online yesterday but not today"}
   {:db/ident :aggregate/retained
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were online yesterday and today"}
   {:db/ident :aggregate/resurrected
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were not online yesterday and are online today"}
   {:db/ident :aggregate/spawned
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/activated
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/set-networking-keys
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/online
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "How many ships were online during the day"}
   {:db/ident :aggregate/locked
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of locked stars"}
   {:db/ident :aggregate/kids-hashes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Kids hash counts for the day"}])

(def aggregate-schema
  [{:db/ident :aggregate/day
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The day of aggregation, timestamp at midnight UTC"}
   {:db/ident :aggregate/node-type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The node-type of the aggregate, one of :all, :galaxy, :star or :planet"}
   {:db/ident :aggregate/day+node-type
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:aggregate/day :aggregate/node-type]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Composite of day and node-type for uniqueness constraint"}
   {:db/ident :aggregate/new
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, new ships online on this day"}
   {:db/ident :aggregate/churned
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were online yesterday but not today"}
   {:db/ident :aggregate/retained
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were online yesterday and today"}
   {:db/ident :aggregate/resurrected
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Growth accounting, ships that were not online yesterday and are online today"}
   {:db/ident :aggregate/spawned
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/activated
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/set-networking-keys
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of ships that have set their networking keys at least once"}
   {:db/ident :aggregate/locked
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Cumulative total of locked stars"}
   {:db/ident :aggregate/kids-hashes
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many
    :db/doc "Kids hash counts for the day"}])

(def hash-schema
  [{:db/ident :hash/kids-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The %cz kids hash"}
   {:db/ident :hash/day
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The day of aggregation, timestamp at midnight UTC. Same as :aggregate/day, needed here for upserts."}
   {:db/ident :hash/node-type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The node-type of the aggregate, one of :all, :galaxy, :star or :planet. Same as :aggregate/node-type, needed here for upserts."}
   {:db/ident :hash/kids-hash+day+node-type
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:hash/kids-hash :hash/day :hash/node-type]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Composite of hash, day and node-type for uniqueness constraint. Need this for upserts."}
   {:db/ident :hash/count
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The number of ships responding with a kids hash for the day"}])

(def urbit-os-schema
  [{:db/ident :urbit-os/version
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The urbit-os version"}
   {:db/ident :urbit-os/hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The hash corresponding to a urbit-os version"}])
