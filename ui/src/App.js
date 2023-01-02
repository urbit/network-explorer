import { useState, useEffect, cloneElement } from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import './App.css';
import './fonts.css';

import { Box,
         Row,
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
import { KidsHashChart } from './KidsHashChart';
import { StatusChart } from './StatusChart';
import { StatusTable } from './StatusTable';

const API_BASE_URL = 'https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com';

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

const setUrlParam = (key, value) => {
  if (window.history.pushState) {
    let searchParams = new URLSearchParams(window.location.search);
    searchParams.set(key, value);
    let newurl = window.location.protocol + '//' + window.location.host + window.location.pathname + '?' + searchParams.toString();
    window.history.pushState({path: newurl}, '', newurl);
  }
};

const fetchKidsHashes = (stateSetter, since) => {
  stateSetter({loading: true});

  let url = `${API_BASE_URL}/get-kids-hashes`;

  if (since) {
    url += '?since=' + since;
  }

  lastApiCall = url;

  fetch(url)
    .then(res => res.json())
    .then(kidsHashes => {
      const kids = kidsHashes.map(e => {
        const {date, ...rest} = e;
        const o = Object.entries(rest).map(([k, v]) => {
          const s = k.split('.');
          return [s[s.length-1], v];
        }).sort((a, b) => a[1] - b[1]).reverse();

        return Object.fromEntries(
          o.slice(0, 4).concat([['date', date],
                                ['others', o.slice(4).reduce((acc, e) => e[1]+acc, 0)]]));
      });

      stateSetter({loading: false, data: kids});
    });
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

const fetchAggregateStatus = (stateSetter, since, until, nodeType) => {
  stateSetter({loading: true});

  let url = nodeType ?
      `${API_BASE_URL}/get-aggregate-status?nodeType=${nodeType}` :
      `${API_BASE_URL}/get-aggregate-status?`;

  if (since) {
    url += '&since=' + since;
  }

  if (until) {
    url += '&until=' + until;
  }

  fetch(url)
    .then(res => res.json())
    .then(es => {
      const events = es.map(e => {
        const d = new Date(e.date);
        return Object.assign(e, {date: e.date.substring(0, 10), unlocked: 65536 - e.locked});
      });

      stateSetter({loading: false, events: events });
    });
};

const fetchAggregateEvents = (eventType, stateSetter, since, nodeType) => {
  stateSetter({loading: true});

  let url = nodeType ?
      `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}&nodeType=${nodeType}` :
      `${API_BASE_URL}/get-aggregate-pki-events?eventType=${eventType}`;

  if (since) {
    url += '&since=' + since;
  }

  fetch(url)
    .then(res => res.json())
    .then(es => {

      const events = es.map(e => {
        const d = new Date(e.date);
        const month = d.toLocaleString('default', {month: 'short'});
        return Object.assign({month: month}, e, {date: e.date.substring(0, 10)});
      });

      stateSetter({loading: false, events: events });
    });
};

function App() {

  const params = new URLSearchParams(window.location.search);

  const nodeMap = {all: 'All', planet: 'Planets', star: 'Stars', galaxy: 'Galaxies'};
  const timeRangeMap = {all: 'All', year: 'Year', sixMonths: '6 Months', month: 'Month', week: 'Week'};

  const [azimuthEvents, setAzimuthEvents] = useState({loading: true, events: []});

  const [aggregateStatus, setAggregateStatus] = useState({loading: true, events: []});

  const [kidsHashes, setKidsHashes] = useState({loading: true, data: []});

  const [transferEvents, setTransferEvents] = useState({loading: true, events: []});

  const [nodesText, setNodesText] = useState(nodeMap[params.get('nodes')] || 'All');

  const [timeRangeText, setTimeRangeText] = useState(timeRangeMap[params.get('timeRange')] || 'Year');

  const [offset, setOffset] = useState(0);

  const [chart, setChart] = useState(params.get('chart') || 'addressSpace');

  useEffect(() => {
    const since = isoStringToDate(timeRangeTextToSince(timeRangeText));
    const until = (timeRangeText === 'All' && nodesText === 'Stars')
          ? undefined :
          isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

    fetchPkiEvents(setAzimuthEvents, nodesTextToNodeType(nodesText), timeRangeTextToSince(timeRangeText));
    fetchKidsHashes(setKidsHashes, since);
    fetchAggregateStatus(setAggregateStatus, since, until, nodesTextToNodeType(nodesText));
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
            const until = (timeRangeText === 'All' && nodesText === 'Stars')
                  ? undefined :
                  isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

            fetchAggregateStatus(setAggregateStatus,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 until,
                                 nodesTextToNodeType(nodesText));

            fetchKidsHashes(setKidsHashes,
                            isoStringToDate(timeRangeTextToSince(timeRangeText)));
          }}
          fetchNodePkiEvents={nodesText => fetchPkiEvents(setAzimuthEvents,
                                                          nodesTextToNodeType(nodesText),
                                                          timeRangeTextToSince(timeRangeText))}
          fetchNodeAggregateEvents={nodesText => {
            const until = (timeRangeText === 'All' && nodesText === 'Stars')
                  ? undefined :
                  isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

            fetchAggregateStatus(setAggregateStatus,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 until,
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
                       className='bodyContainer'
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
              overflowY='scroll'
              flex='1'
              className='bodyContainer'
            >
              <Col
                m={3}
                p={3}
                backgroundColor='white'
                borderRadius='8px'
                flex='1'
                minHeight='250px'
                overflowY='scroll'
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
                  minHeight='250px'
                  className='ml'
                >
                  <Row fontWeight={500} p={3}>
                    <Text
                      fontSize={0}
                      cursor='pointer'
                      color={chart === 'addressSpace' ? '' : 'gray'}
                      onClick={() => {
                        setUrlParam('chart', 'addressSpace');
                        setChart("addressSpace");
                      }}
                    >Address Space Composition</Text>
                    <Text
                      fontSize={0}
                      ml={3}
                      cursor='pointer'
                      color={chart === 'kidsHash' ? '' : 'gray'}
                      onClick={() => {
                        setUrlParam('chart', 'kidsHash');
                        setChart('kidsHash');
                      }}
                    >Kids Hash Composition</Text>
                  </Row>

                  { chart === 'kidsHash' ?
                  <Box flex='1'>
                    {kidsHashes.loading ?
                     <Center height='100%'>
                       <LoadingSpinner
                         width='36px'
                         height='36px'
                         foreground='rgba(0, 0, 0, 0.6)'
                         background='rgba(0, 0, 0, 0.2)'
                       />
                     </Center> :
                     <>
                       <KidsHashChart
                         kidsHashes={kidsHashes.data}
                         timeRangeText={timeRangeText}
                       />
                     </>
                  }
                  </Box> :
                  <Box flex='1'>
                    {aggregateStatus.loading ?
                     <Center height='100%'>
                       <LoadingSpinner
                         width='36px'
                         height='36px'
                         foreground='rgba(0, 0, 0, 0.6)'
                         background='rgba(0, 0, 0, 0.2)'
                       />
                     </Center> :
                     <>
                       <StatusChart
                         events={aggregateStatus.events}
                         timeRangeText={timeRangeText}
                         nodesText={nodesText}
                       />
                       <StatusTable
                         first={aggregateStatus.events[0]}
                         last={aggregateStatus.events[aggregateStatus.events.length - 1]}
                         secondLast={aggregateStatus.events[aggregateStatus.events.length - 2]}
                         nodesText={nodesText}
                       />
                     </>
                  }
                  </Box>
                  }
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
