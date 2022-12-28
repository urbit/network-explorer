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
import { SponsoredNodes } from './SponsoredNodes';

const capitalizeFirstLetter = s => {
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const API_BASE_URL = 'https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com';

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
    'voting-proxy': 'Not Set',
    'kids-hash': '0v0'
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
          backgroundColor='white'
          minHeight='50%'
          borderRadius='8px'
          overflow='hidden'
          display='flex'
          flexDirection='column'
          className='height'
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
              <Row className='col' mt={3}>
                <Row flexBasis='50%'>
                  <Col flexBasis='50%'>
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
                  <Col flexBasis='50%'>
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
                </Row>
                <Row flexBasis='50%' className='mt-3'>
                  <Col flexBasis='50%'>
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
                  <Col flexBasis='50%'>
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
              </Row>
              <Row
                mt={3}
                pt={3}
                pb={3}
                borderTop='1px solid rgba(0, 0, 0, 0.1)'
                borderBottom='1px solid rgba(0, 0, 0, 0.1)'
                alignItems='center'
              >
                <Text fontSize={0} color='gray' fontWeight={500} flexBasis='25%' className='hide'>
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
                <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                  Ownership Address
                </Text>
                <Text
                  fontFamily='Source Code Pro !important'
                  fontSize={0}
                  flexBasis='75%'
                  className='fb-50'
                  style={{wordBreak:'break-all'}}
                >
                  {node['ownership-address']}
                </Text>
              </Row>
              {node['kids-hash'] !== '0v0' &&
               <Row mt={3}>
                 <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                   Kids Desk Hash
                 </Text>
                 <Text
                   fontFamily='Source Code Pro !important'
                   fontSize={0}
                   flexBasis='75%'
                   className='fb-50'
                   style={{wordBreak:'break-all'}}
                 >
                   {node['kids-hash']}
                 </Text>
               </Row>
              }
              <Row mt={3}>
                <Text fontSize={0} color='gray' fontWeight={500}>
                  Proxy Addresses
                </Text>
              </Row>
              <Col mt={3}>
                <Row>
                  <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                    Management Proxy
                  </Text>
                  <Text
                    fontFamily='Source Code Pro !important'
                    fontSize={0}
                    flexBasis='75%'
                    className='fb-50'
                    style={{wordBreak:'break-all'}}
                  >
                    {node['management-proxy']}
                  </Text>
                </Row>
                <Row mt={3}>
                  <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                    Spawn Proxy
                  </Text>
                  <Text
                    fontFamily='Source Code Pro !important'
                    fontSize={0}
                    flexBasis='75%'
                    className='fb-50'
                    style={{wordBreak:'break-all'}}
                  >
                    {node['spawn-proxy']}
                  </Text>
                </Row>
                <Row mt={3}>
                  <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                    Transfer Proxy
                  </Text>
                  <Text
                    fontFamily='Source Code Pro !important'
                    fontSize={0}
                    flexBasis='75%'
                    className='fb-50'
                    style={{wordBreak:'break-all'}}
                  >
                    {node['transfer-proxy']}
                  </Text>
                </Row>
                {clan(point) === 'galaxy' &&
                 <Row mt={3}>
                   <Text fontSize={0} color='gray' flexBasis='25%' className='fb-50'>
                     Voting Proxy
                   </Text>
                   <Text
                     fontFamily='Source Code Pro !important'
                     fontSize={0}
                     flexBasis='75%'
                     className='fb-50'
                     style={{wordBreak:'break-all'}}
                   >
                     {node['voting-proxy']}
                   </Text>
                 </Row>
                }
              </Col>
            </Box>
          </Box>
        </Box>
      </Col>
      <Col
        mt={3}
        mr={3}
        mb={3}
        flex='1'
        className='ml mt'
        overflowY='auto'
      >
        <Box
          backgroundColor='white'
          overflowY='auto'
          borderRadius='8px'
          height='50%'
        >
          <Box
            p={3}
            height='75%'
          >
            <Row justifyContent='space-between'>
              <Text fontSize={0} fontWeight={500}>Azimuth Event Stream</Text>
            </Row>
            <AzimuthEvents
              loading={azimuthEvents.loading}
              events={azimuthEvents.events} />
          </Box>
        </Box>
        { (clan(point) !== 'planet' && (node.loading || node.kids.length > 0)) &&
          <Box
            backgroundColor='white'
            overflowY='auto'
            borderRadius='8px'
            flex='1'
            mt={3}
            mb={3}
          >
            <Box
              p={3}
              height='75%'
            >
              <SponsoredNodes
                header={clan(point) === 'star' ? 'Sponsored Planets' : 'Sponsored Stars'}
                loading={node.loading}
                kids={node.kids}
                sponsor={point}
              />
            </Box>
          </Box>
        }
      </Col>
    </>
  );
}
