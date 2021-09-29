import React from 'react';

import { Link } from 'react-router-dom';

import { Box,
         Row,
         Text} from '@tlon/indigo-react';

import { TimeRangeMenu } from './TimeRangeMenu';
import { NodeMenu } from './NodeMenu';

export function MenuHeader(props) {
  const { disabled,
          timeRangeText,
          setTimeRangeText,
          nodesText,
          setNodesText,
          fetchTimeRangePkiEvents,
          fetchTimeRangeAggregateEvents,
          fetchNodePkiEvents,
          fetchNodeAggregateEvents,
        } = props;

  return(
    <Row
      width='100%'
      justifyContent='space-between'
      alignItems='center'
    >
      <Box
        p={3}
      >
        <Box>
          <Link to='/' style={{textDecoration:'none'}}>
            <Text cursor='pointer' ml={3} fontSize={2}>
              Azimuth activity
            </Text>
          </Link>
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
            disabled={disabled}
            timeRangeText={timeRangeText}
            setTimeRangeText={setTimeRangeText}
            fetchPkiEvents={fetchTimeRangePkiEvents}
            fetchAggregateEvents={fetchTimeRangeAggregateEvents}
          />
          <NodeMenu
            disabled={disabled}
            nodesText={nodesText}
            setNodesText={setNodesText}
            fetchPkiEvents={fetchNodePkiEvents}
            fetchAggregateEvents={fetchNodeAggregateEvents}
          />
        </Box>
      </Box>
    </Row>
  );
}
