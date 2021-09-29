import React from 'react';
import { Link } from 'react-router-dom';

import { useState, useEffect } from 'react';

import { Box,
         Row,
         Icon,
         Text,
         Col } from '@tlon/indigo-react';

import { sigil, reactRenderer } from '@tlon/sigil-js';

import { clan, sein, patp2dec } from 'urbit-ob';

import { AzimuthEvents } from './AzimuthEvents';
import { SponsoredPlanets } from './SponsoredPlanets';

const capitalizeFirstLetter = s => {
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const API_BASE_URL = 'https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com';

const defaultNodeData = point => {
  return {
    'num-owners': '-',
    'revision': '-',
    'continuity': '-',
    'type': clan(point),
    'sponsor': clan(point) === 'star' ?
      {'urbit-id': sein(point)} :
    {'urbit-id': sein(point), 'sponsor': {'urbit-id': sein(sein(point))}},
    'kids': [],
    'ownership-address': 'Not Set',
    'transfer-proxy': 'Not Set',
    'management-proxy': 'Not Set',
    'spawn-proxy': 'Not Set',
    'voting-proxy': 'Not Set'
  };
};

export function Node(props) {

  const { point } = props.match.params;

  const [node, setNode] = useState(
    {'num-owners': '-',
     'revision': '-',
     'continuity': '-',
     'type': clan(point),
     'sponsor': clan(point) === 'star' ?
                {'urbit-id': sein(point)} :
                {'urbit-id': sein(point), 'sponsor': {'urbit-id': sein(sein(point))}},
     'kids': [],
     'ownership-address': 'Not Set',
     'transfer-proxy': 'Not Set',
     'management-proxy': 'Not Set',
     'spawn-proxy': 'Not Set',
     'voting-proxy': 'Not Set',
     'loading': true,
    }
  );

  const [azimuthEvents, setAzimuthEvents] = useState({loading: true, events: []});

  const fetchNodeInfo = (urbitId) => {
    let url = `${API_BASE_URL}/get-node?urbit-id=${urbitId}`;

    fetch(url)
      .then(res => res.json())
      .then(nodeInfo => setNode(Object.assign({}, defaultNodeData(urbitId), nodeInfo, {loading: false})));
  };

  const fetchPkiEvents = (urbitId) => {
    setAzimuthEvents({loading: true});

    let url = `${API_BASE_URL}/get-pki-events?urbit-id=${urbitId}`;

    fetch(url)
      .then(res => res.json())
      .then(events => setAzimuthEvents({loading: false, events: events}));
  };

  useEffect(() => {
    fetchNodeInfo(point);
    fetchPkiEvents(point);
  }, [point]);

  return (
    <>
      <Col
        m={3}
        flex='1'
      >
        <Box
          flex='1'
          backgroundColor='white'
          borderRadius='8px'
          overflow='hidden'
          display='flex'
          flexDirection='column'
        >
          <Box display='flex' m={3} flex='1'>
            {
              sigil({patp: point,
                     renderer: reactRenderer,
                     size: 64,
                     colors: ['white', 'black']})
            }
            <Box ml={4} display='flex' flex='1' flexDirection='column'>
              <Text fontSize={0} color='gray' fontWeight={500}>
                Node Information
              </Text>
              <Row mt={3}>
                <Col flexBasis='25%'>
                  <Text fontSize={0} color='gray'>
                    Urbit ID
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Node Type
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Point Number
                  </Text>
                </Col>
                <Col flexBasis='25%'>
                  <Text fontFamily='Source Code Pro !important' fontSize={0}>
                    {point}
                  </Text>
                  <Text fontSize={0} mt={3}>
                    {capitalizeFirstLetter(node['type'])}
                  </Text>
                  <Text fontSize={0} mt={3}>
                    {new Intl.NumberFormat('de-DE').format(patp2dec(point))}
                  </Text>
                </Col>
                <Col flexBasis='25%'>
                  <Text fontSize={0} color='gray'>
                    Continuity
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Key Revision
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Owners
                  </Text>
                </Col>
                <Col flexBasis='25%'>
                  <Text fontSize={0}>
                    {node['continuity']}
                  </Text>
                  <Text fontSize={0} mt={3}>
                    {node['revision']}
                  </Text>
                  <Text fontSize={0} mt={3}>
                    {node['num-owners']}
                  </Text>
                </Col>
              </Row>
              <Row
                mt={3}
                pt={3}
                pb={3}
                borderTop='1px solid rgba(0, 0, 0, 0.1)'
                borderBottom='1px solid rgba(0, 0, 0, 0.1)'
                alignItems='center'
              >
                <Text fontSize={0} color='gray' fontWeight={500} flexBasis='25%'>
                  Sponsor Chain
                </Text>
                <Row flexBasis='25%' alignItems='center'>
                  {clan(point) === 'planet' &&
                   <>
                     <Link
                       to={'/' + node['sponsor']['sponsor']['urbit-id']}
                       style={{display:'flex', textDecoration: 'none', alignItems: 'center'}}
                     >
                       <Text fontFamily='Source Code Pro !important' fontSize={0} color='gray'>
                         {node['sponsor']['sponsor']['urbit-id']}
                       </Text>
                     </Link>
                     <Icon ml={1} icon='ChevronEast' size={12} />
                   </>
                  }
                  {(clan(point) === 'planet' || clan(point) === 'star') &&
                    <>
                      <Link
                        to={'/' + node['sponsor']['urbit-id']}
                        style={{display:'flex', textDecoration: 'none', alignItems: 'center'}}
                      >
                        <Text fontFamily='Source Code Pro !important' ml={1} fontSize={0} color='gray'>
                          {node['sponsor']['urbit-id']}
                        </Text>
                      </Link>
                      <Icon ml={1} icon='ChevronEast' size={12} />
                    </>
                  }
                  <Text fontFamily='Source Code Pro !important' ml={1} fontSize={0} whiteSpace='nowrap'>
                    {point}
                  </Text>
                </Row>
              </Row>
              <Row mt={3}>
                <Text fontSize={0} color='gray' fontWeight={500}>
                  Proxy Addresses
                </Text>
              </Row>
              <Row mt={3}>
                <Col flexBasis='25%'>
                  <Text fontSize={0} color='gray'>
                    Ownership Proxy
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Spawn Proxy
                  </Text>
                  <Text fontSize={0} color='gray' mt={3}>
                    Transfer Proxy
                  </Text>
                  {clan(point) === 'galaxy' &&
                   <Text fontSize={0} color='gray' mt={3}>
                     Voting Proxy
                   </Text>
                  }
                </Col>
                <Col flexBasis='25%'>
                  <Text fontFamily='Source Code Pro !important' fontSize={0}>
                    {node['management-proxy']}
                  </Text>
                  <Text fontFamily='Source Code Pro !important' fontSize={0} mt={3}>
                    {node['spawn-proxy']}
                  </Text>
                  <Text fontFamily='Source Code Pro !important' fontSize={0} mt={3}>
                    {node['transfer-proxy']}
                  </Text>
                  {clan(point) === 'galaxy' &&
                   <Text fontFamily='Source Code Pro !important' fontSize={0} mt={3}>
                     {node['voting-proxy']}
                   </Text>
                  }
                </Col>
              </Row>
            </Box>
          </Box>
        </Box>
        <Box flex='1' mb={3} />
      </Col>
      <Col
        flex='1'
      >
        <Box
          mt={3}
          mr={3}
          backgroundColor='white'
          overflowY='auto'
          borderRadius='8px'
          flex='1'
        >
          <Box
            p={3}
            height='75%'
          >
            <AzimuthEvents
              header='Azimuth Event Stream'
              loading={azimuthEvents.loading}
              events={azimuthEvents.events} />
          </Box>
        </Box>
        { (clan(point) !== 'planet' && (node.loading || node.kids.length > 0)) ?
          <Box
            backgroundColor='white'
            overflowY='auto'
            borderRadius='8px'
            flex='1'
            mt={3}
            mr={3}
            mb={3}
          >
            <Box
              p={3}
              height='75%'
            >
              <SponsoredPlanets
                header='Azimuth Event Stream'
                loading={node.loading}
                kids={node.kids}
                sponsor={point}
              />
            </Box>
          </Box> :
          <Box flex='1' mb={3} mt={3} />
        }
      </Col>
    </>
  );
}
