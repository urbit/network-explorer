import { useState, useEffect, cloneElement } from 'react';
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

import  SearchHeader from './SearchHeader';
import { MenuHeader } from './MenuHeader';
import { Node } from './Node';
import { AzimuthEvents } from './AzimuthEvents';
import { AzimuthChart } from './AzimuthChart';

const API_BASE_URL = 'https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com';

const ONE_HOUR = 1000 * 60 * 60;
const ONE_DAY = ONE_HOUR * 24;
const ONE_WEEK = ONE_DAY * 7;
const ONE_MONTH = ONE_DAY * 30;
const SIX_MONTHS = ONE_MONTH * 6;
const ONE_YEAR = ONE_MONTH * 12;

var lastApiCall;

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
      `${API_BASE_URL}/get-pki-events?limit=100&nodeType=${nodeType}` :
      `${API_BASE_URL}/get-pki-events?limit=100`;

  if (since) {
    url += '&since=' + since;
  }

  lastApiCall = url;

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
        const month = d.toLocaleString('default', {month: 'short'});
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

  const [timeRangeText, setTimeRangeText] = useState('6 Months');

  const [offset, setOffset] = useState(0);

  useEffect(() => {
    fetchPkiEvents(setAzimuthEvents);
    fetchAggregateEvents('spawn', setSpawnEvents, '2021-03-01');
    fetchAggregateEvents('change-ownership', setTransferEvents, '2021-03-01');
  }, []);

  const menuHeader =
        <MenuHeader
          timeRangeText={timeRangeText}
          setTimeRangeText={setTimeRangeText}
          nodesText={nodesText}
          setNodesText={setNodesText}
          fetchTimeRangePkiEvents={timeRangeText => fetchPkiEvents(setAzimuthEvents,
                                                                   nodesTextToNodeType(nodesText),
                                                                   timeRangeTextToSince(timeRangeText))}
          fetchTimeRangeAggregateEvents={timeRangeText => {
            fetchAggregateEvents('spawn',
                                 setSpawnEvents,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 nodesTextToNodeType(nodesText));
            fetchAggregateEvents('change-ownership',
                                 setTransferEvents,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 nodesTextToNodeType(nodesText));
          }}
          fetchNodePkiEvents={nodesText => fetchPkiEvents(setAzimuthEvents,
                                                          nodesTextToNodeType(nodesText),
                                                          timeRangeTextToSince(timeRangeText))}
          fetchNodeAggregateEvents={nodesText => {
            fetchAggregateEvents('spawn',
                                 setSpawnEvents,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 nodesTextToNodeType(nodesText));
            fetchAggregateEvents('change-ownership',
                                 setTransferEvents,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 nodesTextToNodeType(nodesText));
          }}
        />;

  return (
    <Box className='App'
         display='flex'
         flexDirection='column'
         height='100%'
    >
      <Router>
        <Switch>
          <Route path="/:point" render={routeProps => {
            return <>
                     <SearchHeader />
                     {cloneElement(menuHeader, {disabled: true})}
                     <Row
                       backgroundColor='#E9E9E9'
                       display='flex'
                       overflowY='auto'
                       flex='1'
                     >
                       <Node {...routeProps} />
                     </Row>
                   </>;
          }} />
          <Route exact path="/">
            <SearchHeader />
            {cloneElement(menuHeader, {disabled: false})}
            <Row
              backgroundColor='#E9E9E9'
              display='flex'
              overflowY='auto'
              flex='1'
            >
              <Col
                m={3}
                p={3}
                backgroundColor='white'
                borderRadius='8px'
                width='50%'
                flex='1'
                overflowY='auto'
              >
                <Row justifyContent='space-between' alignItems='center'>
                  <Text fontSize={0} fontWeight={500}>Global Azimuth Event Stream</Text>
                  <Box>
                    {offset !== 0 &&
                     <Icon
                       icon='TriangleWest'
                       cursor='pointer'
                       size={16}
                       onClick={() => {
                         setAzimuthEvents({loading: true});
                         fetch(lastApiCall + '&offset=' + (offset - 100))
                           .then(res => res.json())
                           .then(events => {
                             setAzimuthEvents({loading: false, events: events});
                             setOffset(offset - 100);
                           });
                       }}
                     />
                    }
                    <Icon
                      icon='TriangleEast'
                      cursor='pointer'
                      size={16}
                      onClick={() => {
                        setAzimuthEvents({loading: true});
                        fetch(lastApiCall + '&offset=' + (offset + 100))
                          .then(res => res.json())
                          .then(events => {
                            setAzimuthEvents({loading: false, events: events});
                            setOffset(offset + 100);
                          });
                      }}
                    />
                  </Box>
                </Row>
                <AzimuthEvents
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
                  <Box flex='1' backgroundColor='#F5F5F5'>
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
                  <Box flex='1' backgroundColor='#F5F5F5'>
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
          </Route>
        </Switch>
      </Router>
    </Box>
  );
}

export default App;
