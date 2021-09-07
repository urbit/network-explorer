import React from 'react';

import { Row, Col, Text } from '@tlon/indigo-react';

export function AzimuthEvent(props) {
  const {type, time, address, node} = props;
  return (
    <Row justifyContent='space-between'>
      <Col>
        <Text fontSize={0}>{type}</Text>
      </Col>
      <Col>
        <Text fontSize={0}>{node['urbit-id']}</Text>
      </Col>
      <Col>
        <Text fontSize={0}>{address}</Text>
      </Col>
      <Col>
        <Text fontSize={0}>{time}</Text>
      </Col>
    </Row>
  );
}
