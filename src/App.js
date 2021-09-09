import { useState, useEffect } from 'react';

import './App.css';
import './fonts.css';
import { Box,
         Row,
         StatelessTextInput,
         Icon,
         Table,
         Tr,
         Td,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text,
         Col,
         Button,
         Center } from '@tlon/indigo-react';

import { AzimuthEvent } from './AzimuthEvent';

import { ResponsiveContainer,
         BarChart,
         ReferenceLine,
         CartesianGrid,
         XAxis,
         YAxis,
         Tooltip,
         Legend,
         Bar
       } from 'recharts';


function App() {

  const [azimuthEvents, setAzimuthEvents] = useState([]);

  const [spawnEvents, setSpawnEvents] = useState([]);

  const [transferEvents, setTransferEvents] = useState([]);

  let months = spawnEvents.reduce((acc, e) => acc.add(e.month), new Set());

  useEffect(() => {
    fetch('https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com/get-pki-events?limit=100')
      .then(res => res.json())
      .then(events => setAzimuthEvents(events));
  }, []);


  useEffect(() => {
    fetch('https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com/get-aggregate-pki-events?type=spawn&since=2021-03-01')
      .then(res => res.json())
      .then(events => setSpawnEvents(events.map(e => {
        const d = new Date(e.date);
        const month = d.toLocaleString('default', {month: 'long'});
        return Object.assign({month: month},
                             e,
                             {date: e.date.substring(0, 10)});
      })));
  }, []);


  useEffect(() => {
    fetch('https://j6lpjx5nhf.execute-api.us-west-2.amazonaws.com/get-aggregate-pki-events?type=change-ownership&since=2021-03-01')
      .then(res => res.json())
      .then(events => setTransferEvents(events.map(e => {
        const d = new Date(e.date);
        const month = d.toLocaleString('default', {month: 'long'});
        return Object.assign({month: month},
                             e,
                             {date: e.date.substring(0, 10)});
      })));
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
            <Menu>
              <Text
                color='gray'
                fontWeight={400}
                fontSize={2}
              >
                Time Range
              </Text>
              <MenuButton
                style={{cursor: 'pointer'}}
                border='none'
                height='auto'
                width='auto'
                fontSize={2}
              >
                6 months <Icon ml='10px' icon='ChevronSouth' size={12} />
              </MenuButton>
            </Menu>
            <Menu>
              <Text
                color='gray'
                fontWeight={400}
                fontSize={2}
                ml='34px'
              >
                Nodes
              </Text>
              <MenuButton
                style={{cursor: 'pointer'}}
                border='none'
                height='auto'
                width='auto'
                fontSize={2}
              >
                All <Icon ml='10px' icon='ChevronSouth' size={12} />
              </MenuButton>
            </Menu>
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
            {azimuthEvents.map(e => <AzimuthEvent {...e}/>)}
          </Table>
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
              <ResponsiveContainer>
                <BarChart
                  barCategoryGap={0}
                  data={spawnEvents}>
                  <XAxis
                    hide={true}
                    xAxisId='0'
                    dataKey='date'
                  />
                  <XAxis
                    xAxisId='1'
                    dataKey='month'
                    allowDuplicatedCategory={false}
                  />
                  <Tooltip />
                  <Legend align='left' iconType='circle' />
                  <Bar name='Spawn Events' dataKey='count' fill='#BF421B' />
                  {[...months.values()].map(month => {
                    return <ReferenceLine xAxisId='1' x={month} stroke='rgba(0, 0, 0, 0.2)' />;
                  })}
                </BarChart>
              </ResponsiveContainer >
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
              <ResponsiveContainer>
                <BarChart
                  barCategoryGap={0}
                  data={transferEvents}>
                  <XAxis
                    hide={true}
                    xAxisId='0'
                    dataKey='date'
                  />
                  <XAxis
                    xAxisId='1'
                    dataKey='month'
                    allowDuplicatedCategory={false}
                  />
                  <Tooltip />
                  <Legend align='left' iconType='circle' />
                  <Bar name='Transfer Events' dataKey='count' fill='#093C09' />
                  {[...months.values()].map(month => {
                    return <ReferenceLine xAxisId='1' x={month} stroke='rgba(0, 0, 0, 0.2)' />;
                  })}
                </BarChart>
              </ResponsiveContainer >
            </Box>
          </Box>
        </Col>
      </Row>
    </Box>
  );
}

export default App;
