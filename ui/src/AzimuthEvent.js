import React from 'react';
import { Link } from 'react-router-dom';

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
  'escaped': {svg: escapeAccepted, text: 'Escape Accepted'},
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

  if (data.type === 'spawn') {

    let sig = sigil({patp: data['target-node']['urbit-id'],
                     renderer: reactRenderer,
                     size: 16,
                     colors: ['white', 'black']});

    sig.props.style.display = 'inline';
    sig.props.style.verticalAlign = 'middle';

    return <>
             {sig}
             <Link to={'/' + data['target-node']['urbit-id']} style={{textDecoration:'none'}}>
               <Text
                 fontFamily='Source Code Pro !important'
                 ml={1}
                 color='gray'
                 fontSize={0}
                 verticalAlign='middle'
               >
                 {data['target-node']['urbit-id']}
               </Text>
             </Link>
           </>;
  }

  if (data.type === 'escaped' || data.type === 'escape-requested'){
    return <>
             <Text color='gray' fontSize={0}>Sponsor</Text>
             <Text ml={1}
                   pl={1}
                   pr={1}
                   fontSize={0}
                   fontFamily='Source Code Pro !important'
                   color='black'
                   backgroundColor='rgba(0, 0, 0, 0.04)'
                   borderRadius='2px'
             >
               {data['target-node']['urbit-id']}
             </Text>
           </>;
  }

  return <Text title={data.address} color='gray' fontSize={0} fontFamily='Source Code Pro !important'>
           {data.address ? data.address.substring(0, 8) + '...' + data.address.slice(-6) : ''}
         </Text>;
};

export function AzimuthEvent(props) {
  const {type, time, node} = props;

  let sig = sigil({patp: node['urbit-id'],
                     renderer: reactRenderer,
                     size: 16,
                     colors: ['white', 'black']});

  sig.props.style.display = 'inline';
  sig.props.style.verticalAlign = 'middle';

  return (
    <Tr>
      <Td>
        <img src={m[type].svg} style={{verticalAlign: 'middle'}} alt='Icon for Azimuth Event' />
        <Text ml={2} fontSize={0} verticalAlign='middle'>{m[type].text}</Text>
      </Td>
      <Td>
        {sig}
        <Link to={'/' + node['urbit-id']} style={{textDecoration:'none'}}>
          <Text
            fontFamily='Source Code Pro !important'
            ml={1}
            color='gray'
            fontSize={0}
            verticalAlign='middle'
          >
            {node['urbit-id']}
          </Text>
        </Link>
      </Td>
      <Td>
        {formatData(props)}
      </Td>
      <Td>
        <Text
          title={time}
          fontFamily='Source Code Pro !important'
          color='gray'
          fontSize={0}
        >
          {formatTimeAgo(time)}</Text>
      </Td>
    </Tr>
  );
}
