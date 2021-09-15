import { useState, useEffect } from 'react';

import './App.css';
import './fonts.css';

import { Box,
         Row,
         StatelessTextInput,
         LoadingSpinner,
         Center,
         Icon,
         Table,
         Tr,
         Text,
         Col } from '@tlon/indigo-react';

import { AzimuthEvent } from './AzimuthEvent';
import { AzimuthChart } from './AzimuthChart';
import { TimeRangeMenu } from './TimeRangeMenu';
import { NodeMenu } from './NodeMenu';

const API_BASE_URL = 'https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com';


const fetchPkiEvents = (stateSetter, nodeType) => {
  stateSetter({loading: true});

  const url = nodeType ?
        `${API_BASE_URL}/get-pki-events?limit=1000&nodeType=${nodeType}` :
        `${API_BASE_URL}/get-pki-events?limit=1000`;

  fetch(url)
    .then(res => res.json())
    .then(events => stateSetter({loading: false, events: events}));
};

const fetchAggregateEvents = (eventType, stateSetter, since, nodeType) => {
  stateSetter({loading: true, months: new Set()});

  const url = nodeType ?
        `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}&since=${since}&nodeType=${nodeType}` :
        `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}&since=${since}`;

  fetch(url)
    .then(res => res.json())
    .then(es => {
      let months = new Set();

      const events = es.map(e => {
        const d = new Date(e.date);
        const month = d.toLocaleString('default', {month: 'long'});
        months.add(month);
        return Object.assign({month: month}, e, {date: e.date.substring(0, 10)});
      });

      stateSetter({loading: false, events: events, months: months});
    });
};

function App() {

  const [azimuthEvents, setAzimuthEvents] = useState({loading: true, events: []});

  const [spawnEvents, setSpawnEvents] = useState({loading: true, months: new Set(), events: []});

  const [transferEvents, setTransferEvents] = useState({loading: true, months: new Set(), events: []});

  useEffect(() => {
    fetchPkiEvents(setAzimuthEvents);
    fetchAggregateEvents('spawn', setSpawnEvents, '2021-03-01');
    fetchAggregateEvents('change-ownership', setTransferEvents, '2021-03-01');
  }, []);

  return (
    <Box className='App'
         display='flex'
         flexDirection='column'
         height='100%'
    >
      <Row
        justifyContent='space-between'
        alignItems='center'
        borderBottom='1px solid rgba(0, 0, 0, 0.1)'
      >
        <Box
          p={3}
        >
          <Box>
            <Text color='gray' fontSize={2}>
              Urbit
            </Text>
            <Text ml={1} fontSize={2}>
              / Network explorer
            </Text>
          </Box>
        </Box>
        <Box
          p='12px'
        >
          <StatelessTextInput
            placeholder='Search for a node...'
            backgroundColor='rgba(0, 0, 0, 0.04)'
            borderRadius='4px'
            fontWeight={400}
            height={40}
            width={256}
          />
        </Box>
      </Row>
      <Row
        width='100%'
        justifyContent='space-between'
        alignItems='center'
      >
        <Box
          p={3}
        >
          <Box>
            <Text cursor='pointer' color='gray' fontSize={2} mr={3}>
              Address space
            </Text>
            <Text cursor='pointer' ml={3} fontSize={2}>
              Azimuth activity
            </Text>
          </Box>
        </Box>
        <Box
          p={3}
        >
          <Box
            display='flex'
            alignItems='center'
          >
            <TimeRangeMenu />
            <NodeMenu
              fetchPkiEvents={nodeType => fetchPkiEvents(setAzimuthEvents, nodeType)}
              fetchAggregateEvents={nodeType => {
                fetchAggregateEvents('spawn', setSpawnEvents, '2021-03-01', nodeType);
                fetchAggregateEvents('change-ownership', setTransferEvents, '2021-03-01', nodeType);
              }}
            />
          </Box>
        </Box>
      </Row>
      <Row
        backgroundColor='#E9E9E9'
        display='flex'
        overflowY='auto'
        flex='1'
      >
        <Col
          m={2}
          p={2}
          backgroundColor='white'
          borderRadius='8px'
          width='50%'
          flex='1'
          overflowY='auto'
        >
          <Row justifyContent='space-between'>
            <Text fontSize={0} fontWeight={500}>Global Azimuth Event Stream</Text>
            <Icon icon='Info' size={16} cursor='pointer' />
          </Row>
          {azimuthEvents.loading ?
           <Center height='100%'>
             <LoadingSpinner
               width='36px'
               height='36px'
               foreground='rgba(0, 0, 0, 0.6)'
               background='rgba(0, 0, 0, 0.2)'
             />
           </Center> :
           <Table border='0'>
             <Tr textAlign='left' pb={2} mt={4}>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontSize={0} color='gray'>Event Type</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontSize={0} color='gray'>Node</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontSize={0} color='gray'>Data</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontSize={0} color='gray'>Time</Text>
               </th>
             </Tr>
             {azimuthEvents.events.map(e => <AzimuthEvent {...e}/>)}
           </Table>
          }
        </Col>
        <Col
          m={2}
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
            <Row fontWeight={500} p={2} justifyContent='space-between'>
              <Text fontSize={0}>Spawn Events</Text>
              <Icon icon='Info' size={16} cursor='pointer' />
            </Row>
            <Box flex='1' m={2}>
              {spawnEvents.loading ?
               <Center height='100%'>
                 <LoadingSpinner
                   width='36px'
                   height='36px'
                   foreground='rgba(0, 0, 0, 0.6)'
                   background='rgba(0, 0, 0, 0.2)'
                 />
               </Center> :
               <AzimuthChart
                 name='Spawn Events'
                 fill='#BF421B'
                 events={spawnEvents.events}
                 months={spawnEvents.months}
               />}
            </Box>
          </Box>
          <Box
            mt={2}
            backgroundColor='white'
            borderRadius='8px'
            flex='1'
            display='flex'
            flexDirection='column'
          >
            <Row fontWeight={500} p={2} justifyContent='space-between'>
              <Text fontSize={0}>Transfer Events</Text>
              <Icon icon='Info' size={16} cursor='pointer' />
            </Row>
            <Box flex='1' m={2}>
              {transferEvents.loading ?
               <Center height='100%'>
                 <LoadingSpinner
                   width='36px'
                   height='36px'
                   foreground='rgba(0, 0, 0, 0.6)'
                   background='rgba(0, 0, 0, 0.2)'
                 />
               </Center> :
               <AzimuthChart
                 name='Transfer Events'
                 fill='#093C09'
                 events={transferEvents.events}
                 months={transferEvents.months}
               />}
            </Box>
          </Box>
        </Col>
      </Row>
    </Box>
  );
}

export default App;
