import React from 'react';
import ReactDOM from 'react-dom';
import { Link } from 'react-router-dom';
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
import { OnlineShipsChart } from './OnlineShipsChart';
import { KidsHashChart } from './KidsHashChart';
import { BootedChart } from './BootedChart';
// import { StarLockupChart } from './StarLockupChart';
import { StatusChart } from './StatusChart';
import { StatusTable } from './StatusTable';

const API_BASE_URL = 'https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com';

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

const setUrlParam = (key, value) => {
  if (window.history.pushState) {
    let searchParams = new URLSearchParams(window.location.search);
    searchParams.set(key, value);
    let newurl = window.location.protocol + '//' + window.location.host + window.location.pathname + '?' + searchParams.toString();
    window.history.pushState({path: newurl}, '', newurl);
  }
};

const fetchOnlineStats = (stateSetter, since, nodeType) => {
  stateSetter({loading: true});

  let url = nodeType ?
        `${API_BASE_URL}/get-online-stats?nodeType=${nodeType}` :
        `${API_BASE_URL}/get-online-stats?`;

  if (since) {
    if (Date.parse(since) < 1663632000000) {
      url += '&since=2022-09-20';
    } else {
      url += '&since=' + since;
    }
  } else {
    url += '&since=2022-09-20';
  }

  fetch(url)
    .then(res => res.json())
    .then(onlineShips => {
      onlineShips.forEach(e =>{
        e.churned = -e.churned;
        if (nodeType === undefined) {
          switch (e.day) {
          case "2023-05-05":
            e.missing = 650;
            delete e.churned;
            break;
          case "2023-05-14":
            e.missing = 640;
            delete e.churned;
            break;
          case "2023-05-15":
            e.missing = 640;
            delete e.churned;
            break;
          case "2023-10-16":
            e.missing = 3700;
            delete e.churned;
            break;
          case "2024-06-20":
            e.missing = 850;
            break;
          case "2024-06-23":
            e.missing = 4000;
            delete e.churned;
            break;
          }
        }
      });
      stateSetter({loading: false, data: onlineShips});
    });
};

const fetchKidsHashes = (stateSetter, since, nodeType) => {
  stateSetter({loading: true});

  let url = nodeType ?
        `${API_BASE_URL}/get-kids-hashes?nodeType=${nodeType}` :
        `${API_BASE_URL}/get-kids-hashes?`;

  if (since) {
    url += '&since=' + since;
  }

  fetch(url)
    .then(res => res.json())
    .then(kidsHashes => {
      const kids = kidsHashes.map(e => {
        const { day } = e;
        const o = e['kids-hashes'].map(x => {
          const os = x['urbit-os'] && x['urbit-os'].version;
          const h = x['kids-hash'].split('.');
          const s = os ? os : h[h.length-1];
          return [s, {count: x.count, hash: h[h.length-1]}];
        }).sort((a, b) => a[1].count - b[1].count).reverse();

        return Object.fromEntries(
          o.slice(0, 4).concat([['day', day],
                                ['others',
                                 {count: o.slice(4).reduce((acc, e) => e[1].count+acc, 0),
                                  hash: 'others'}]]));
      });

      stateSetter({loading: false, data: kids});
    });
};

const fetchPkiEvents = (stateSetter, nodeType, offset, since) => {
  stateSetter({loading: true});

  let url = nodeType ?
      `${API_BASE_URL}/get-pki-events?limit=100&nodeType=${nodeType}&offset=${offset}` :
      `${API_BASE_URL}/get-pki-events?limit=100&offset=${offset}`;

  if (since) {
    url += '&since=' + since;
  }

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
        return Object.assign(e, {unlocked: 65536 - e.locked});
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

const modalText = {
  'onlineShips':
  <>
    <Row>
      <Text>
        Online ships are measured using a <a href='https://tribecap.co/a-quantitative-approach-to-product-market-fit/' >growth accounting framework</a>. The partitions are as following:
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        New: Online ships today that we have never seen before.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Resurrected: Ships that were offline yesterday day but are online again.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Retained: Ships that were online yesterday and also today.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Churned: Ships that were online yesterday but are offline now.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Missing: Data missing because of an incident.
      </Text>
    </Row>
    <Row mt={4}>
      <Text fontSize={0}>
        Incidents:
      </Text>
    </Row>
    <Row mt={2}>
      <Text fontSize={0}>
        2023-05-05: The radar ship ran out of memory, partial data loss for the day.
      </Text>
    </Row>
    <Row mt={2}>
      <Text fontSize={0}>
        2023-05-14: The galaxy ~deg was misconfigured after an upgrade, leading to peer discovery problems for radar.
      </Text>
    </Row>
    <Row mt={2}>
      <Text fontSize={0}>
        2023-05-15: The galaxy ~dem was suffering from a regression after an upgrade, causing radar to be unable to contact stars and planets under ~dem.
      </Text>
    </Row>
    <Row mt={2}>
      <Text fontSize={0}>
        2023-10-16: The radar ship was having memory trouble, leading to data loss for the day.
      </Text>
    </Row>
    <Row mt={2}>
      <Text fontSize={0}>
        2024-06-20: The radar was having memory trouble but did not crash, therefore not triggering the monitoring alerts. Unfortunately this happened during Lake Summit, leading to data loss for 4 days.
      </Text>
    </Row>
  </>,
  'addressSpace':
  <>
    <Row>
      <Text>
        Address space composition is tracked via Azimuth L1 and L2. The partitions are as follows:
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Spawned: Cumulative number of ships spawned by date.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Set Networking Keys: Cumulative number of ships that have set their networking keys at least once by date.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Booted: Ships that have been online at least once since ~2022.6.4.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Online: Cumulative number of ships online by date.
      </Text>
    </Row>
  </>,
  'kidsHash':
  <>
    <Row>
      <Text>
        Kids hash composition is useful for roughly measuring how well urbit-os over-the-air updates are propagating across the network.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Every urbit-os version corresponds to a %cz hash. The four last hex characters are shown in this graph.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Note that only the four most common versions are shown, the rest are aggregated under "others."
      </Text>
    </Row>
  </>,
  'booted':
  <>
    <Row>
      <Text>
        This chart shows the cumulative total of Urbit ships that have been online at least once since ~2022.6.4.
      </Text>
    </Row>
    <Row mt={2}>
      <Text>
        Note that this statistic (and all statistics on the network explorer) do not include comets and moons.
      </Text>
    </Row>
  </>,
};

const useModal = () => {
  const [isShowing, setIsShowing] = useState(false);

  function toggle() {
    setIsShowing(!isShowing);
  }

  return {
    isShowing,
    toggle,
  };
};

const Modal = ({ isShowing, hide, tab }) => isShowing ? ReactDOM.createPortal(
  <React.Fragment>
    <div className='modal-overlay' />
    <div className='modal-wrapper' onClick={hide} aria-modal aria-hidden tabIndex={-1} role='dialog'>
      <div className='modal'>
        <div className='modal-header'>
          <Icon
            icon='X'
            className='modal-close-button'
            data-dismiss='modal'
            aria-label='Close'
            cursor='pointer'
            size={16}
          >
            <span aria-hidden='true'>&times;</span>
          </Icon>
        </div>
        <Text fontSize={1}>{modalText[tab]}</Text>
      </div>
    </div>
  </React.Fragment>, document.body
) : null;

function App() {

  const params = new URLSearchParams(window.location.search);

  const nodeMap = {all: 'All', planet: 'Planets', star: 'Stars', galaxy: 'Galaxies'};
  const timeRangeMap = {all: 'All', year: 'Year', sixMonths: '6 Months', month: 'Month', week: 'Week'};

  const [onlineShips, setOnlineShips] = useState({loading: true, data: []});

  const [azimuthEvents, setAzimuthEvents] = useState({loading: true, events: []});

  const [aggregateStatus, setAggregateStatus] = useState({loading: true, events: []});

  const [kidsHashes, setKidsHashes] = useState({loading: true, data: []});

  const [transferEvents, setTransferEvents] = useState({loading: true, events: []});

  const [nodesText, setNodesText] = useState(nodeMap[params.get('nodes')] || 'All');

  const [timeRangeText, setTimeRangeText] = useState(timeRangeMap[params.get('timeRange')] || 'Year');

  const [offset, setOffset] = useState(0);

  const [chart, setChart] = useState(params.get('chart') || 'booted');

  const {isShowing, toggle} = useModal();

  useEffect(() => {
    const since = isoStringToDate(timeRangeTextToSince(timeRangeText));
    const until = (timeRangeText === 'All' && nodesText === 'Stars')
          ? undefined :
          isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

    fetchOnlineStats(setOnlineShips, since, nodesTextToNodeType(nodesText));
    fetchPkiEvents(setAzimuthEvents, nodesTextToNodeType(nodesText), 0, timeRangeTextToSince(timeRangeText));
    fetchKidsHashes(setKidsHashes, since, nodesTextToNodeType(nodesText));
    fetchAggregateStatus(setAggregateStatus, since, until, nodesTextToNodeType(nodesText));
  }, []);

  const menuHeader =
        <MenuHeader
          timeRangeText={timeRangeText}
          setTimeRangeText={setTimeRangeText}
          nodesText={nodesText}
          setNodesText={setNodesText}
          fetchTimeRangePkiEvents={timeRangeText => {
            fetchPkiEvents(setAzimuthEvents,
                           nodesTextToNodeType(nodesText),
                           0,
                           timeRangeTextToSince(timeRangeText));
            setOffset(0);
          }}
          fetchTimeRangeAggregateEvents={timeRangeText => {
            const until = (timeRangeText === 'All' && nodesText === 'Stars')
                  ? undefined :
                  isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

            fetchOnlineStats(setOnlineShips,
                            isoStringToDate(timeRangeTextToSince(timeRangeText)),
                            nodesTextToNodeType(nodesText));

            fetchAggregateStatus(setAggregateStatus,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 until,
                                 nodesTextToNodeType(nodesText));

            fetchKidsHashes(setKidsHashes,
                            isoStringToDate(timeRangeTextToSince(timeRangeText)),
                            nodesTextToNodeType(nodesText));
          }}
          fetchNodePkiEvents={nodesText => {
            fetchPkiEvents(setAzimuthEvents,
                           nodesTextToNodeType(nodesText),
                           offset,
                           timeRangeTextToSince(timeRangeText));
            setOffset(0);
          }}
          fetchNodeAggregateEvents={nodesText => {
            const until = (timeRangeText === 'All' && nodesText === 'Stars')
                  ? undefined :
                  isoStringToDate(new Date(new Date().getTime() + ONE_DAY).toISOString());

            fetchOnlineStats(setOnlineShips,
                             isoStringToDate(timeRangeTextToSince(timeRangeText)),
                             nodesTextToNodeType(nodesText));

            fetchAggregateStatus(setAggregateStatus,
                                 isoStringToDate(timeRangeTextToSince(timeRangeText)),
                                 until,
                                 nodesTextToNodeType(nodesText));

            fetchKidsHashes(setKidsHashes,
                            isoStringToDate(timeRangeTextToSince(timeRangeText)),
                            nodesTextToNodeType(nodesText));
          }}
        />;

  const onlineShipsChart =
        <Box flex='1'>
          {onlineShips.loading ?
           <Center height='100%'>
             <LoadingSpinner
               width='36px'
               height='36px'
               foreground='rgba(0, 0, 0, 0.6)'
               background='rgba(0, 0, 0, 0.2)'
             />
           </Center> :
           <>
             <OnlineShipsChart
               onlineShips={onlineShips.data}
               timeRangeText={timeRangeText}
             />
           </>
          }
        </Box>;


    const kidsHashChart =
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
          </Box>;

    const addressSpaceChart =
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
          </Box>;

    const bootedChart =
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
               <BootedChart
                 events={aggregateStatus.events.filter(e => e.booted !== undefined)}
                 timeRangeText={timeRangeText}
                 nodesText={nodesText}
               />
             </>
            }
          </Box>;


    // const lockupChart =
    //       <Box flex='1'>
    //         <StarLockupChart
    //           timeRangeText={timeRangeText}
    //         />
    //       </Box>;

    let visibleChart;
    if (chart === 'onlineShips') visibleChart = onlineShipsChart;
    if (chart === 'addressSpace') visibleChart = addressSpaceChart;
    if (chart === 'kidsHash') visibleChart = kidsHashChart;
    if (chart === 'booted') visibleChart = bootedChart;
    // if (chart === 'lockup') visibleChart = lockupChart;

  return (
    <Box className='App'
         display='flex'
         flexDirection='column'
         height='100%'
    >
      <Router>
        <Switch>
          <Route path="/:point" render={routeProps => {
            const point = routeProps.match.params.point;
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
                       <Node point={point} key={point} />
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
                         fetchPkiEvents(setAzimuthEvents,
                                        nodesTextToNodeType(nodesText),
                                        offset - 100,
                                        timeRangeTextToSince(timeRangeText));
                         setOffset(offset - 100);
                       }}
                     />
                    }
                    <Icon
                      icon='TriangleEast'
                      cursor='pointer'
                      size={16}
                      onClick={() => {
                         fetchPkiEvents(setAzimuthEvents,
                                        nodesTextToNodeType(nodesText),
                                        offset + 100,
                                        timeRangeTextToSince(timeRangeText));
                         setOffset(offset + 100);
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
                  <Row fontWeight={500} p={3} justifyContent='space-between'>
                    <Box>
                      <Text
                        fontSize={0}
                        cursor='pointer'
                        color={chart === 'booted' ? '' : 'gray'}
                        onClick={() => {
                          setUrlParam('chart', 'booted');
                          setChart('booted');
                        }}
                      >Total Booted Ships</Text>
                      <Text
                        ml={3}
                        fontSize={0}
                        cursor='pointer'
                        color={chart === 'onlineShips' ? '' : 'gray'}
                        onClick={() => {
                          setUrlParam('chart', 'onlineShips');
                          setChart('onlineShips');
                        }}
                      >Online Ships Composition</Text>
                      <Text
                        ml={3}
                        fontSize={0}
                        cursor='pointer'
                        color={chart === 'addressSpace' ? '' : 'gray'}
                        onClick={() => {
                          setUrlParam('chart', 'addressSpace');
                          setChart('addressSpace');
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
                      >OS Version Distribution</Text>
                    </Box>
                    <Icon
                      icon='Info'
                      cursor='pointer'
                      size={16}
                      onClick={() => {
                        toggle();
                      }}
                    />
                    {/* <Text */}
                    {/*   fontSize={0} */}
                    {/*   ml={3} */}
                    {/*   cursor='pointer' */}
                    {/*   color={chart === 'lockup' ? '' : 'gray'} */}
                    {/*   onClick={() => { */}
                    {/*       setUrlParam('chart', 'lockup'); */}
                    {/*       setChart('lockup'); */}
                    {/*   }} */}
                    {/* >Star Lockup Composition</Text> */}
                  </Row>
                  { visibleChart }
                </Box>
              </Col>
            </Row>
            <Modal
              isShowing={isShowing}
              hide={toggle}
              tab={chart}
            />
          </Route>
        </Switch>
      </Router>
    </Box>
  );
}

export default App;
