/+  default-agent, *server, dbug
::
|%
+$  card  card:agent:gall
::
+$  state-1
  $:  =ship-info
      logs=(list log)
  ==
::
+$  log          [who=@p sent=@da recv=@da kids=@uv]
+$  ship-info    (map @p info)
+$  info         status=ship-status
+$  ship-status
  $%  [%await-resp sent=@da]
      [%await-wake wake=@da]
  ==
::
++  jael-delay  ~m10
++  poke-delay  ~h12
--
::
=|  [%1 state-1]
=*  state  -
^-  agent:gall
%-  agent:dbug
::
=>  |%
    ++  deeded-ships
      |=  bowl:gall
      .^((set @p) %j /(scot %p our)/ships-with-deeds/(scot %da now))
    ::
    ++  request-kids-cz
      |=  [who=ship case=?([%ud @] [%da @da])]
      ^-  card:agent:gall
      :+  %pass  /clay-read/(scot %p who)
      [%arvo %c %warp who %kids ~ %sing %z case /]
    ::
    ++  request-new
      |=  [woz=(set ship) =^ship-info wen=@da]
      %-  ~(rep in woz)
      |=  [who=@p [cad=(list card) dat=_ship-info]]
      :-  [(request-kids-cz who [%ud 1]) cad]
      %+  ~(put by dat)  who
      [%await-resp wen]
    ::
    ++  data-as-csv
      :-  %-  crip
          ;:  weld
            "received,"
            "sent,"
            "point,"
            "kids"
          ==
      ^-  (list @t)
      %+  turn  (skim logs |=(=log (gth recv.log ~2022.10.5)))
      |=  =log
      (crip "{(scow %da recv.log)},{(scow %da sent.log)},{(scow %p who.log)},{(scow %uv kids.log)}")
    ::
    ::
    --
|_  =bowl:gall
+*  this  .
    def  ~(. (default-agent this %|) bowl)
::
++  on-init
  ^-  (quip card _this)
  =^  pokes  ship-info
    (request-new (deeded-ships bowl) ship-info now.bowl)
  :_  this
  %+  welp  pokes
  :_  ~
  [%pass /jael-scry %arvo %b %wait (add now.bowl jael-delay)]
::
++  on-save   !>(state)
::
++  on-load
  |=  old=vase
  ^-  (quip card _this)
  =/  old-state  !<([%1 state-1] old)
  [~ this(state old-state)]
::
++  on-arvo
  |=  [wir=wire sin=sign-arvo]
  ^-  (quip card _this)
  ~|  wir
  ?+  wir  !!
  ::
      [%bind ~]
    [~ this]
  ::
      [%jael-scry ~]
    ?>  ?=(%wake +<.sin)
    =/  new-ships  (deeded-ships bowl)
    =/  old-ships  ~(key by ship-info)
    =/  added      (~(dif in new-ships) old-ships)
    ::  removed should always be empty? do nothing with it for now
    =/  removed    (~(dif in old-ships) new-ships)
    ~?  (gth ~(wyt in added) 0)
      [%added added]
    ~?  (gth ~(wyt in removed) 0)
      [%removed removed]
    =^  pokes  ship-info
      (request-new added ship-info now.bowl)
    :_  this
    :_  pokes
    [%pass /jael-scry %arvo %b %wait (add now.bowl jael-delay)]
  ::
      [%delay @ ~]
    ?>  ?=(%wake +<.sin)
    =/  who=ship  (slav %p i.t.wir)
    =/  info  (~(got by ship-info) who)
    ?>  ?=(%await-wake -.status.info)
    =.  ship-info
      (~(put by ship-info) who [%await-resp now.bowl])
    :_  this
    [(request-kids-cz who [%da now.bowl])]~
  ::
      [%clay-read @ ~]
    ~|  [%sign +<.sin]
    ?>  ?=(%writ +<.sin)
    =/  res=@uv
      ::NOTE  we don't *really* care about the result, only the ping,
      ::      but if we get it, it's a nice side-effect
      ::
      ?~  p.sin  0v0
      ~|  p.r.u.p.sin
      !<(@uvI q.r.u.p.sin)
    =/  who=ship  (slav %p i.t.wir)
    =/  info      (~(got by ship-info) who)
    ~|  [%status -.status.info]
    ?>  ?=(%await-resp -.status.info)
    =/  wake-time=@da  (add now.bowl poke-delay)
    =.  ship-info  (~(put by ship-info) who [%await-wake wake-time])
    =.  logs  [[who sent.status.info now.bowl res] logs]
    :_  this
    [%pass /delay/(scot %p who) %arvo %b %wait wake-time]~
  ==
::
++  on-poke  on-poke:def
::
++  on-watch
  |=  pax=path
  ^-  (quip card _this)
  ?+  pax  (on-watch:def pax)
    [%http-response *]  [~ this]
  ==
::
++  on-agent  on-agent:def
++  on-fail   on-fail:def
++  on-leave  on-leave:def
++  on-peek
  |=  pax=path
  ^-  (unit (unit cage))
  ?+  pax  ~
    [%x %raw ~]
      ``txt+!>(data-as-csv)
  ==
--
