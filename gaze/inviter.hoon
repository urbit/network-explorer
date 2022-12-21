/+  default-agent, naive
|%
+$  card  card:agent:gall
--
::
^-  agent:gall
=<
|_  =bowl:gall
+*  this  .
    do    ~(. +> bowl)
    def   ~(. (default-agent this %.n) bowl)
::
++  on-poke  on-poke:def
::
++  on-arvo  on-arvo:def
::
++  on-init
  ^-  (quip card _this)
  :_  this
  :_  ~
  [%pass /watch-azimuth %agent [our.bowl %azimuth] %watch /event]
::
++  on-agent
  |=  [=wire =sign:agent:gall]
  ^-  (quip card _this)
  ?+  -.sign  (on-agent:def wire sign)
      %kick
    ?.  =(/watch-azimuth wire)  [~ this]
    :_  this
    :_  ~
    [%pass /watch-azimuth %agent [our.bowl %azimuth] %watch /event]
  ::
      %fact
    ?.  =(/watch-azimuth wire)  (on-agent:def wire sign)
      ?.  ?=(%naive-diffs p.cage.sign)
        [~ this]
      :_  this
      (handle-azimuth-diff:do !<(diff:naive q.cage.sign))
  ==
::
::
++  on-save   on-save:def
++  on-load   on-load:def
++  on-watch  on-watch:def
++  on-leave  on-leave:def
++  on-peek   on-peek:def
++  on-fail   on-fail:def
--
::
|_  =bowl:gall
++  handle-azimuth-diff
  |=  =diff:naive
  ^-  (list card)
  ?.  ?=(%point -.diff)  ~
  ?.  ?=(%keys -.+.+.diff)  ~
  ?.  |(=(1 life.keys.diff) =(2 life.keys.diff))  ~
  =/  invite-store-action
  :*  %invite
      %groups
      (shaf %group-uid eny.bowl)
      our.bowl
      %group-push-hook
      [our.bowl %getting-started]
      ship.diff
      'Welcome to Urbit! This group will help orient you to what\'s here on the network.'
  ==
  :_  ~
  [%pass /inviter %agent [our.bowl %invite-hook] %poke %invite-action !>(invite-store-action)]
--
