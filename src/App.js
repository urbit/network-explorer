import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import './App.css';
import './fonts.css';

import { Box,
         Row,
         StatelessTextInput,
         LoadingSpinner,
         Center,
         Icon,
         Text,
         Col } from '@tlon/indigo-react';

import { Node } from './Node';
import { AzimuthEvents } from './AzimuthEvents';
import { AzimuthChart } from './AzimuthChart';
import { TimeRangeMenu } from './TimeRangeMenu';
import { NodeMenu } from './NodeMenu';

const API_BASE_URL = 'https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com';

const ONE_HOUR = 1000 * 60 * 60;
const ONE_DAY = ONE_HOUR * 24;
const ONE_WEEK = ONE_DAY * 7;
const ONE_MONTH = ONE_DAY * 30;
const SIX_MONTHS = ONE_MONTH * 6;
const ONE_YEAR = ONE_MONTH * 12;

const isoStringToDate = isoString => {
  return (isoString === undefined) ? undefined : isoString.substring(0, 10);
};

const timeRangeTextToSince = timeRangeText => {
  const now = new Date();
  const m = {
    'Day': now.toISOString().substring(0, 11) + '00:00:00.000Z',
    'Week': new Date(now - ONE_WEEK).toISOString(),
    'Month': new Date(now - ONE_MONTH).toISOString(),
    '6 Months': new Date(now - SIX_MONTHS).toISOString(),
    'Year': new Date(now - ONE_YEAR).toISOString()
  };

  return m[timeRangeText];

};

const nodesTextToNodeType = nodesText => {
  const m = {
    'All': undefined,
    'Planets': 'planet',
    'Stars': 'star',
    'Galaxies': 'galaxy'
  };

  return m[nodesText];
};

const fetchPkiEvents = (stateSetter, nodeType, since) => {
  stateSetter({loading: true});

  let url = nodeType ?
      `${API_BASE_URL}/get-pki-events?limit=1000&nodeType=${nodeType}` :
      `${API_BASE_URL}/get-pki-events?limit=1000`;

  if (since) {
    url += '&since=' + since;
  }

  fetch(url)
    .then(res => res.json())
    .then(events => stateSetter({loading: false, events: events}));
};

const fetchAggregateEvents = (eventType, stateSetter, since, nodeType) => {
  stateSetter({loading: true, months: new Set()});

  let url = nodeType ?
      `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}&nodeType=${nodeType}` :
      `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}`;

  if (since) {
    url += '&since=' + since;
  }

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

  const [nodesText, setNodesText] = useState('All');

  const [timeRangeText, setTimeRangeText] = useState('6 months');

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
            <TimeRangeMenu
              timeRangeText={timeRangeText}
              setTimeRangeText={setTimeRangeText}
              fetchPkiEvents={timeRangeText => fetchPkiEvents(setAzimuthEvents,
                                                              nodesTextToNodeType(nodesText),
                                                              timeRangeTextToSince(timeRangeText))}
              fetchAggregateEvents={timeRangeText => {
                fetchAggregateEvents('spawn',
                                     setSpawnEvents,
                                     isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                     nodesTextToNodeType(nodesText));
                fetchAggregateEvents('change-ownership',
                                     setTransferEvents,
                                     isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                     nodesTextToNodeType(nodesText));
              }}
            />
            <NodeMenu
              nodesText={nodesText}
              setNodesText={setNodesText}
              fetchPkiEvents={nodesText => fetchPkiEvents(setAzimuthEvents,
                                                          nodesTextToNodeType(nodesText),
                                                          timeRangeTextToSince(timeRangeText))}
              fetchAggregateEvents={nodesText => {
                fetchAggregateEvents('spawn',
                                     setSpawnEvents,
                                     isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                     nodesTextToNodeType(nodesText));
                fetchAggregateEvents('change-ownership',
                                     setTransferEvents,
                                     isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                     nodesTextToNodeType(nodesText));
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
        <Router>
          <Switch>
            <Route path="/:point" component={Node} />
            <Route exact path="/">
              <Col
                m={3}
                p={3}
                backgroundColor='white'
                borderRadius='8px'
                width='50%'
                flex='1'
                overflowY='auto'
              >
                <AzimuthEvents
                  header='Global Azimuth Event Stream'
                  loading={azimuthEvents.loading}
                  events={azimuthEvents.events} />
              </Col>
              <Col
                mt={3}
                mb={3}
                mr={3}
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
                  <Row fontWeight={500} p={3} justifyContent='space-between'>
                    <Text fontSize={0}>Spawn Events</Text>
                    <Icon icon='Info' size={16} cursor='pointer' />
                  </Row>
                  <Box flex='1' m={3}>
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
                  mt={3}
                  backgroundColor='white'
                  borderRadius='8px'
                  flex='1'
                  display='flex'
                  flexDirection='column'
                >
                  <Row fontWeight={500} p={3} justifyContent='space-between'>
                    <Text fontSize={0}>Transfer Events</Text>
                    <Icon icon='Info' size={16} cursor='pointer' />
                  </Row>
                  <Box flex='1' m={3}>
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
            </Route>
          </Switch>
        </Router>
      </Row>
    </Box>
  );
}

export default App;
