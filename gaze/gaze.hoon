::  gaze: azimuth statistics
::
::    general flow:
::    - receive events
::    - process events whose timestamp is known
::    - request timestamps for unknown block numbers (if not already running)
::    - receive timestamps, process events
::
/-  eth-watcher
/+  *ethereum, *azimuth, default-agent, verb, naive
::
=>  |%
    +$  state-0
      $:  %0
          ::  qued: tagged-diffs waiting on block timestamp, oldest first
          ::  time: timestamps of block numbers
          ::  seen: events sorted by timestamp, newest first
          ::  days: stats by day, newest first
          ::
          running=(unit @ta)
          qued=(list tagged-diff)
          time=(map @ud @da)
          seen=(list [wen=@da wat=event])
          days=(list [day=@da sat=stats])
      ==
    ::
    +$  loglist  loglist:eth-watcher
    +$  event
      $%  [%azimuth who=ship dominion=?(%l1 %l2) dif=diff-point]
          [%invite by=ship of=ship gift=ship to=address]
      ==
    ::
    +$  stats
      $:  spawned=(list @p)
          activated=(list @p)
          transfer-p=(list @p)
          transferred=(list @p)
          configured=(list @p)
          breached=(list @p)
          request=(list @p)
          sponsor=(list @p)
          management-p=(list @p)
          voting-p=(list @p)
          spawn-p=(list @p)
          invites-senders=(list @p)
      ==
    ::
    +$  card  card:agent:gall
    ::
    +$  tagged-diff  [=id:block:jael =diff:naive]
    ::
    ++  node-url  'http://eth-mainnet.urbit.org:8545'
    ++  refresh-rate  ~h1
    ++  timeout-time  ~h2
    --
::
=|  state-0
=*  state  -
::
%+  verb  |
^-  agent:gall
=<
  |_  =bowl:gall
  +*  this  .
      do    ~(. +> bowl)
      def   ~(. (default-agent this %|) bowl)
      bec   byk.bowl(r da+now.bowl)
  ::
  ++  on-init
    ^-  (quip card _this)
    [setup-cards:do this]
  ::
  ++  on-save  !>(state)
  ++  on-load
    |=  old=vase
    ^-  (quip card _this)
    [~ this(state !<(state-0 old))]
  ::
  ++  on-poke
    |=  [=mark =vase]
    ^-  (quip card _this)
    ?>  ?=(%noun mark)
    =/  =noun  !<(noun vase)
    |-  ^-  [cards=(list card) =_this]
    ?+  noun  ~|([dap.bowl %unknown-poke noun] !!)
        %reconnect
      :_  this
      :~  leave-eth-watcher:do
          watch-eth-watcher:do
      ==
    ::
        %reload
      :-  cards:$(noun %reconnect)
      this(qued ~, seen ~, days ~)
    ::
        %rewatch
      :_  this:$(noun %reset)
      :~  leave-eth-watcher:do
          clear-eth-watcher:do
          setup-eth-watcher:do
          await-eth-watcher:do
      ==
    ::
        %export
      [export:do this]
    ::
        %clear-running
      [~ this(running ~)]
    ::
        %request-timestamps
      =^  cards  state
        request-timestamps:do
      [cards this]
    ::
        %debug
      ~&  latest=(turn (scag 5 seen) head)
      ~&  oldest=(turn (slag (sub (max 5 (lent seen)) 5) seen) head)
      ~&  :-  'order is'
          =-  ?:(sane 'sane' 'insane')
          %+  roll  seen
          |=  [[this=@da *] last=@da sane=?]
          :-  this
          ?:  =(*@da last)  &
          (lte this last)
      ~&  time=~(wyt by time)
      ~&  qued=(lent qued)
      ~&  days=(lent days)
      ~&  running=running
      [~ this]
    ==
  ::
  ++  on-agent
    |=  [=wire =sign:agent:gall]
    ^-  (quip card _this)
    ?+  -.sign  (on-agent:def wire sign)
        %kick
      ?.  =(/watcher wire)  [~ this]
      [[watch-eth-watcher:do]~ this]
    ::
        %fact
      ?+  wire  (on-agent:def wire sign)
          [%watch-azimuth ~]
        ?.  ?=(%naive-diffs p.cage.sign)
          [~ this]
          ::  (on-agent:def wire sign)
        =^  cards  state
          %-  handle-azimuth-tagged-diff:do
          !<(tagged-diff q.cage.sign)
        [cards this]
      ::
          [%timestamps @ ~]
        ?+  p.cage.sign  (on-agent:def wire sign)
            %thread-fail
          =+  !<([=term =tang] q.cage.sign)
          =/  =tank  leaf+"{(trip dap.bowl)} thread failed; will retry"
          %-  (slog tank leaf+<term> tang)
          =^  cards  state
            request-timestamps:do
          [cards this]
        ::
            %thread-done
          =^  cards  state
            %-  save-timestamps:do
            !<((list [@ud @da]) q.cage.sign)
          [cards this]
        ==
      ==
    ==
  ::
  ++  on-arvo
    |=  [=wire =sign-arvo]
    ^-  (quip card _this)
    ?+  +<.sign-arvo  ~|([dap.bowl %strange-arvo-sign +<.sign-arvo] !!)
        %wake
      ?:  =(/export wire)
        [[wait-export:do export:do] this]
      ?:  =(/watch wire)
        [[watch-eth-watcher:do]~ this]
      ~&  [dap.bowl %strange-wake wire]
      [~ this]
    ==
  ::
  ::  +on-peek: accept gall scry
  ::
  ::    %/days/txt:   per day, digest stats
  ::    %/months/txt: per month, digest stats
  ::    %/raw/txt:    all observed events
  ::
  ++  on-peek  ::TODO
    |=  pax=path
    ^-  (unit (unit cage))
    ?+  pax  ~
      [%x %days ~]
        :^  ~  ~  %txt
        !>((export-days days))
      [%x %months ~]
        :^  ~  ~  %txt
        !>((export-months days))
      [%x %raw ~]
        ``txt+!>(export-raw)
    ==
  ::
  ++  on-watch  on-watch:def
  ++  on-leave  on-leave:def
  ++  on-fail   on-fail:def
  --
::
|_  =bowl:gall
++  bec  byk.bowl(r da+now.bowl)
++  setup-cards
  ^-  (list card)
  [setup-azimuth ~]
  ::  :~  setup-eth-watcher
  ::      ::  we punt on subscribing to the eth-watcher for a little while.
  ::      ::  this way we get a %history diff containing all past events,
  ::      ::  instead of so many individual %log diffs that we bail meme.
  ::      ::  (to repro, replace this with `watch-eth-watcher`)
  ::      ::
  ::      await-eth-watcher
  ::  ==
::
++  wait
  |=  [=wire =@dr]
  ^-  card
  [%pass wire %arvo %b %wait (add now.bowl dr)]
::
++  wait-export  (wait /export refresh-rate)
::
++  setup-azimuth
  ^-  card
  [%pass /watch-azimuth %agent [our.bowl %azimuth] %watch /event]
::
++  to-eth-watcher
  |=  [=wire =task:agent:gall]
  ^-  card
  [%pass wire %agent [our.bowl %eth-watcher] task]
::
++  setup-eth-watcher
  %+  to-eth-watcher  /setup
  :+  %poke   %eth-watcher-poke
  !>  ^-  poke:eth-watcher
  :+  %watch  /[dap.bowl]
  :*  node-url
      |
      refresh-rate
      timeout-time
      11.565.019
      ~
      ~[azimuth delegated-sending]:mainnet-contracts
      ~
      ~
  ==
::
::  see also comment in +setup-cards
++  await-eth-watcher  (wait /watch ~m30)
::
++  watch-eth-watcher
  %+  to-eth-watcher  /watcher
  [%watch /logs/[dap.bowl]]
::
++  leave-eth-watcher
  %+  to-eth-watcher  /watcher
  [%leave ~]
::
++  clear-eth-watcher
  %+  to-eth-watcher  /clear
  :+  %poke  %eth-watcher-poke
  !>  ^-  poke:eth-watcher
  [%clear /logs/[dap.bowl]]
::
++  poke-spider
  |=  [=wire =cage]
  ^-  card
  [%pass wire %agent [our.bowl %spider] %poke cage]
::
++  watch-spider
  |=  [=wire =sub=path]
  ^-  card
  [%pass wire %agent [our.bowl %spider] %watch sub-path]
::
++  handle-azimuth-tagged-diff
  |=  tag=tagged-diff
  ^-  (quip card _state)
  ?.  ?=(%point -.diff.tag)  [~ state]
  (process-tagged-diff tag)
::
::  +request-timestamps: request block timestamps for the logs as necessary
::
::    will come back as a thread result
::
++  request-timestamps
  ^-  (quip card _state)
  ?~  qued  [~ state]
  ?^  running  [~ state]
  =/  tid=@ta
    %+  scot  %ta
    :((cury cat 3) dap.bowl '_' (scot %uv eny.bowl))
  :_  state(running `tid)
  :~  (watch-spider /timestamps/[tid] /thread-result/[tid])
    ::
      %+  poke-spider  /timestamps/[tid]
      :-  %spider-start
      =-  !>([~ `tid bec %eth-get-timestamps -])
      !>  ^-  [@t (list @ud)]
      :-  node-url
      =-  ~(tap in -)
      %-  ~(gas in *(set @ud))
      ^-  (list @ud)
      %+  turn  qued
      |=  log=tagged-diff
      number.id.log
  ==
::
::  +save-timestamps: store timestamps into state
::
++  save-timestamps
  |=  timestamps=(list [@ud @da])
  ^-  (quip card _state)
  =.  time     (~(gas by time) timestamps)
  =.  running   ~
  =-  %_  request-timestamps
        qued  (flop rest)  ::  oldest first
        seen  (weld logs seen)  ::  newest first
        ::  days  (count-events (flop new))  ::  oldest first
      ==
  %+  roll  `(list tagged-diff)`qued
  |=  [log=tagged-diff [rest=(list tagged-diff) logs=(list [wen=@da wat=event])]]
  ::  to ensure logs are processed in sane order,
  ::  stop processing as soon as we skipped one
  ::
  ?^  rest  [[log rest] logs]
  =/  tim=(unit @da)
    %-  ~(get by time)
    number.id.log
  ?~  tim  [[log rest] logs]
  :-  rest
  =+  ven=(tagged-diff-to-event log)
  ?~  ven  logs
  [[u.tim u.ven] logs]
::
::  +process-tagged-diff: handle new incoming tagged-diff
::
++  process-tagged-diff
  |=  new=tagged-diff  ::  oldest first
  ^-  (quip card _state)
  =.  qued  (snoc qued new)
  ?~  qued  [~ state]
  =-  %_  request-timestamps
        qued  (flop rest)  ::  oldest first
        seen  (weld logs seen)  ::  newest first
        ::  days  (count-events (flop new))  ::  oldest first
      ==
  %+  roll  `(list tagged-diff)`qued
  |=  [log=tagged-diff [rest=(list tagged-diff) logs=(list [wen=@da wat=event])]]
  ::  to ensure logs are processed in sane order,
  ::  stop processing as soon as we skipped one
  ::
  ?^  rest  [[log rest] logs]
  =/  tim=(unit @da)
    %-  ~(get by time)
    number.id.log
  ?~  tim  [[log rest] logs]
  :-  rest
  =+  ven=(tagged-diff-to-event log)
  ?~  ven  logs
  [[u.tim u.ven] logs]
::
++  tagged-diff-to-event
  |=  tag=tagged-diff
  ^-  (unit event)
  ?.  ?=(%point -.diff.tag)  ~
  =/  who=ship  ship.diff.tag
  ?:  ?=(%dominion -.+.+.+.diff.tag)  ~
  %-  some
  ^-  event
  ?-  -.+.+.+.diff.tag
    %rift  [%azimuth who dominion.diff.tag %continuity rift.diff.tag]
    %keys  [%azimuth who dominion.diff.tag %keys life.keys.diff.tag auth.keys.diff.tag]
    %sponsor  [%azimuth who dominion.diff.tag %sponsor ?~(sponsor.diff.tag %.n %.y) ?~(sponsor.diff.tag ~zod u.sponsor.diff.tag)]
    %escape  [%azimuth who dominion.diff.tag %escape to.diff.tag]
    %owner  [%azimuth who dominion.diff.tag %owner address.diff.tag]
    %spawn-proxy  [%azimuth who dominion.diff.tag %spawn-proxy address.diff.tag]
    %management-proxy  [%azimuth who dominion.diff.tag %management-proxy address.diff.tag]
    %voting-proxy  [%azimuth who dominion.diff.tag %voting-proxy address.diff.tag]
    %transfer-proxy  [%azimuth who dominion.diff.tag %transfer-proxy address.diff.tag]
    %activated  [%azimuth who dominion.diff.tag %activated who]
    %spawned  [%azimuth who dominion.diff.tag %spawned who.diff.tag]
  ==
::  +count-events: add events to the daily stats
::
++  count-events
  |=  logs=_seen  ::  oldest first
  ^+  days
  =/  head=[day=@da sat=stats]
    ?^  days  i.days
    *[@da stats]
  =+  tail=?~(days ~ t.days)
  |-
  ::  when done, store updated head, but only if it's set
  ::
  ?~  logs
    ?:  =(*[@da stats] head)  tail
    [head tail]
  =*  log  i.logs
  ::  calculate day for current event, set head if unset
  ::
  =/  day=@da
    (sub wen.log (mod wen.log ~d1))
  =?  day.head  =(*@da day.head)  day
  ::  same day as head, so add to it
  ::
  ?:  =(day day.head)
    %_  $
      sat.head  (count-event wat.log sat.head)
      logs      t.logs
    ==
  ~|  [%weird-new-day old=day.head new=day]
  ?>  (gth day day.head)
  ::  newer day than head of days, so start new head
  ::
  %_  $
    tail  [head tail]
    head  [day *stats]
  ==
::
::  +count-event: add event to the stats, if it's relevant
::
++  count-event
  |=  [eve=event sat=stats]
  ^-  stats
  ?-  -.eve
    %invite  sat(invites-senders [by.eve invites-senders.sat])
  ::
      %azimuth
    ?+  -.dif.eve  sat
      %spawned           sat(spawned [who.dif.eve spawned.sat])
      %activated         sat(activated [who.eve activated.sat])
      %transfer-proxy    ?:  =(0x0 new.dif.eve)  sat
                         sat(transfer-p [who.eve transfer-p.sat])
      %owner             sat(transferred [who.eve transferred.sat])
      %keys              sat(configured [who.eve configured.sat])
      %continuity        sat(breached [who.eve breached.sat])
      %escape            ?~  new.dif.eve  sat
                         sat(request [who.eve request.sat])
      %sponsor           ?.  has.new.dif.eve  sat
                         sat(sponsor [who.eve sponsor.sat])
      %management-proxy  sat(management-p [who.eve management-p.sat])
      %voting-proxy      sat(voting-p [who.eve voting-p.sat])
      %spawn-proxy       sat(spawn-p [who.eve spawn-p.sat])
    ==
  ==
::
::
::  +export: periodically export data
::
++  export
  ^-  (list card)
  :~  (export-move %days (export-days days))
      (export-move %months (export-months days))
      (export-move %events export-raw)
  ==
::
::  +export-move: %info move to write exported .txt
::
++  export-move
  |=  [nom=@t dat=(list @t)]
  ^-  card
  =-  [%pass /export/[nom] %arvo %c %info -]
  %+  foal:space:userlib
    /(scot %p our.bowl)/base/(scot %da now.bowl)/gaze-exports/[nom]/txt
  [%txt !>(dat)]
::
::  +export-months: generate a csv of stats per month
::
++  export-months
  |=  =_days
  %-  export-days
  ^+  days
  %+  roll  (flop days)
  |=  [[day=@da sat=stats] mos=(list [mod=@da sat=stats])]
  ^+  mos
  =/  mod=@da
    %-  year
    =+  (yore day)
    -(d.t 1)
  ?~  mos  [mod sat]~
  ?:  !=(mod mod.i.mos)
    [[mod sat] mos]
  :_  t.mos
  :-  mod
  ::TODO  this is hideous. can we make a wet gate do this?
  :*  (weld spawned.sat spawned.sat.i.mos)
      (weld activated.sat activated.sat.i.mos)
      (weld transfer-p.sat transfer-p.sat.i.mos)
      (weld transferred.sat transferred.sat.i.mos)
      (weld configured.sat configured.sat.i.mos)
      (weld breached.sat breached.sat.i.mos)
      (weld request.sat request.sat.i.mos)
      (weld sponsor.sat sponsor.sat.i.mos)
      (weld management-p.sat management-p.sat.i.mos)
      (weld voting-p.sat voting-p.sat.i.mos)
      (weld spawn-p.sat spawn-p.sat.i.mos)
      (weld invites-senders.sat invites-senders.sat.i.mos)
  ==
::
::  +export-days: generate a csv of stats per day
::
++  export-days
  |=  =_days
  :-  %-  crip
      ;:  weld
        "date,"
        "spawned,"
        "activated,"
        "transfer proxy,"
        "transferred,"
        "transferred (unique),"
        "configured,"
        "configured (unique),"
        "escape request,"
        "sponsor change,"
        "invites,"
        "invites (unique senders)"
      ==
  |^  ^-  (list @t)
      %+  turn  days
      |=  [day=@da stats]
      %-  crip
      ;:  weld
        (scow %da day)            ","
        (count spawned)           ","
        (count activated)         ","
        (count transfer-p)        ","
        (unique transferred)      ","
        (unique configured)       ","
        (count request)           ","
        (count sponsor)           ","
        (unique invites-senders)
      ==
  ::
  ++  count
    |*  l=(list)
    (num (lent l))
  ::
  ++  unique
    |*  l=(list)
    ;:  weld
      (count l)
      ","
      (num ~(wyt in (~(gas in *(set)) l)))
    ==
  ::
  ++  num  (d-co:co 1)
  --
::
::  +export-raw: generate a csv of individual transactions
::
++  export-raw
  :-  %-  crip
      ;:  weld
        "date,"
        "point,"
        "dominion,"
        "event,"
        "field1,field2,field3"
      ==
  |^  ^-  (list @t)
      %+  turn  seen
      :: (cork tail event-to-row crip)
      |=  [wen=@da =event]
      (crip "{(scow %da wen)},{(event-to-row event)}")
  ::
  ++  event-to-row
    |=  =event
    ?-  -.event
      %azimuth  (point-diff-to-row +.event)
      %invite   (invite-to-row +.event)
    ==
  ::
  ++  point-diff-to-row
    |=  [who=ship dominion=?(%l1 %l2) dif=diff-point]
    ^-  tape
    %+  weld  "{(pon who)},{(dom dominion)},"
    ?-  -.dif
      %full               "full,"
      %owner              "owner,{(adr new.dif)}"
      %activated          "activated,"
      %spawned            "spawned,{(pon who.dif)}"
      %keys               "keys,{(num life.dif)}"
      %continuity         "breached,{(num new.dif)}"
      %sponsor            "sponsor,{(spo has.new.dif)},{(pon who.new.dif)}"
      %escape             "escape-req,{(req new.dif)}"
      %management-proxy   "management-p,{(adr new.dif)}"
      %voting-proxy       "voting-p,{(adr new.dif)}"
      %spawn-proxy        "spawn-p,{(adr new.dif)}"
      %transfer-proxy     "transfer-p,{(adr new.dif)}"
    ==
  ::
  ++  invite-to-row
    |=  [by=ship of=ship ship to=address]
    "{(pon by)},invite,{(pon of)},{(adr to)}"
  ::
  ++  num  (d-co:co 1)
  ++  dom  (cury scow %tas)
  ++  pon  (cury scow %p)
  ++  adr  |=(a=@ ['0' 'x' ((x-co:co (mul 2 20)) a)])
  ++  spo  |=(h=? ?:(h "escaped to" "detached from"))
  ++  req  |=(r=(unit @p) ?~(r "canceled" (pon u.r)))
  --
--
