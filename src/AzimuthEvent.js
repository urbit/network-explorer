import React from 'react';

import { Tr, Td, Text } from '@tlon/indigo-react';

import { sigil, reactRenderer } from '@tlon/sigil-js';

import spawned from './spawned.svg';
import brokeContinuity from './broke_continuity.svg';
import activated from './activated.svg';
import changedKeys from './changed_keys.svg';
import escapeRequested from './escape_requested.svg';
import escapeAccepted from './escape_accepted.svg';
import escapeCanceled from './escape_canceled.svg';
import lostSponsor from './lost_sponsor.svg';
import ownershipChanged from './ownership_changed.svg';

const m = {
  'change-ownership': {svg: ownershipChanged, text: 'Ownership Changed'},
  'activate': {svg: activated, text: 'Activated'},
  'spawn': {svg: spawned, text: 'Spawned'},
  'escape-requested': {svg: escapeRequested, text: 'Escape Requested'},
  'escape-canceled': {svg: escapeCanceled, text: 'Escape Canceled'},
  'escaped': {svg: escapeCanceled, text: 'Escape Accepted'},
  'lost-sponsor': {svg: lostSponsor, text: 'Lost Sponsor'},
  'change-networking-keys': {svg: changedKeys, text: 'Changed Keys'},
  'broke-continuity': {svg: brokeContinuity, text: 'Broke Continuity'},
  'change-spawn-proxy': {svg: changedKeys, text: 'Changed Spawn Proxy'},
  'change-transfer-proxy': {svg: changedKeys, text: 'Changed Transfer Proxy'},
  'change-management-proxy': {svg: changedKeys, text: 'Changed Mgmt. Proxy'},
  'change-voting-proxy': {svg: changedKeys, text: 'Changed Voting Proxy'},
  'invite': {svg: ownershipChanged, text: 'Invited'}
};

const formatTimeAgo = dateString => {

  const formatter = new Intl.RelativeTimeFormat(undefined, {
    numeric: 'auto',
    style: 'short'
  });

  const DIVISIONS = [
    { amount: 60, name: 'seconds' },
    { amount: 60, name: 'minutes' },
    { amount: 24, name: 'hours' },
    { amount: 7, name: 'days' },
    { amount: 4.34524, name: 'weeks' },
    { amount: 12, name: 'months' },
    { amount: Number.POSITIVE_INFINITY, name: 'years' }
  ];

  const date = new Date(dateString);
  let duration = (date - new Date()) / 1000;

  for (let i = 0; i <= DIVISIONS.length; i++) {
    const division = DIVISIONS[i];
    if (Math.abs(duration) < division.amount) {
      return formatter.format(Math.round(duration), division.name);
    }
    duration /= division.amount;
  }
};

const formatData = data => {
  if (data.type === 'change-networking-keys') {
    return <>
             <Text color='gray' fontSize={0}>Revision Number</Text>
             <Text ml={1}
                   pl={1}
                   pr={1}
                   fontSize={0}
                   color='black'
                   backgroundColor='rgba(0, 0, 0, 0.04)'
                   borderRadius='2px'
             >
               {data.revision}
             </Text>
           </>;
  }
  if (data.type === 'broke-continuity') {
    return <>
             <Text color='gray' fontSize={0}>Continuity Number</Text>
             <Text ml={1}
                   pl={1}
                   pr={1}
                   fontSize={0}
                   color='black'
                   backgroundColor='rgba(0, 0, 0, 0.04)'
                   borderRadius='2px'
             >
               {data.continuity}
             </Text>
           </>;
  }

  if (data.type === 'escaped' || data.type === 'escape-requested' || data.type === 'escape-canceled'){
    return <>
             <Text color='gray' fontSize={0}>Sponsor</Text>
             <Text ml={1}
                   pl={1}
                   pr={1}
                   fontSize={0}
                   color='black'
                   backgroundColor='rgba(0, 0, 0, 0.04)'
                   borderRadius='2px'
             >
               {data['target-node']['urbit-id']}
             </Text>
           </>;
  }

  return <Text color='gray' fontSize={0}>{data.address ? data.address.substring(0, 10) + '...' : ''}</Text>;
}

export function AzimuthEvent(props) {
  const {type, time, address, node} = props;

  const sig = sigil({patp: node['urbit-id'],
                     renderer: reactRenderer,
                     size: 16,
                     colors: ['white', 'black']});

  sig.props.style.display = 'inline';
  sig.props.style.verticalAlign = 'middle';

  if (m[type] === undefined) {
    console.log(type);
  }

  return (
    <Tr>
      <Td>
        <img src={m[type].svg} style={{verticalAlign: 'middle'}} />
        <Text ml={2} fontSize={0} verticalAlign='middle'>{m[type].text}</Text>
      </Td>
      <Td>
        {sig} <Text color='gray' fontSize={0} verticalAlign='middle'>{node['urbit-id']}</Text>
      </Td>
      <Td>
        {formatData(props)}
      </Td>
      <Td>
        <Text color='gray' fontSize={0}>{formatTimeAgo(time)}</Text>
      </Td>
    </Tr>
  );
}
