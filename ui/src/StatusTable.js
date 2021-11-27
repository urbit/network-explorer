import React from 'react';

import { Table,
         Tr,
         Td,
         Icon,
         Text } from '@tlon/indigo-react';

export function StatusTable(props) {

  const {last, first} = props;

  const hasLockedData = first.locked !== undefined;

  const lastLocked = last.locked || 0;
  const firstLocked = first.locked || 0;

  return (
    <Table ml={3} mt={2} border='0' width='100%' flex='1'>
      <thead>
        <Tr textAlign='left' pb={2} >
          <th>
            <Text fontWeight={400} fontSize={0} color='gray'>Metric</Text>
          </th>
          <th>
            <Text fontWeight={400} fontSize={0} color='gray'>Share</Text>
            <Text ml={1}
                  pl={1}
                  pr={1}
                  fontWeight={400}
                  fontSize={0}
                  color='rgba(0, 0, 0, 0.6)'
                  backgroundColor='rgba(0, 0, 0, 0.04)'
                  borderRadius='2px'
            >
              %
            </Text>
          </th>
          <th>
            <Text fontWeight={400} fontSize={0} color='gray'>Share</Text>
            <Text ml={1}
                  pl={1}
                  pr={1}
                  fontWeight={400}
                  fontSize={0}
                  color='rgba(0, 0, 0, 0.6)'
                  backgroundColor='rgba(0, 0, 0, 0.04)'
                  borderRadius='2px'
            >
              Real
            </Text>
          </th>
          <th>
            <Text fontWeight={400} fontSize={0} color='gray'>Change</Text>
            <Text ml={1}
                  pl={1}
                  pr={1}
                  fontWeight={400}
                  fontSize={0}
                  color='rgba(0, 0, 0, 0.6)'
                  backgroundColor='rgba(0, 0, 0, 0.04)'
                  borderRadius='2px'
            >
              %
            </Text>
          </th>
          <th>
            <Text fontWeight={400} fontSize={0} color='gray'>Change</Text>
            <Text ml={1}
                  pl={1}
                  pr={1}
                  fontWeight={400}
                  fontSize={0}
                  color='rgba(0, 0, 0, 0.6)'
                  backgroundColor='rgba(0, 0, 0, 0.04)'
                  borderRadius='2px'
            >
              Real
            </Text>
          </th>
        </Tr>
      </thead>
      <tbody>
        { hasLockedData &&
          <Tr>
            <Td>
              <svg style={{verticalAlign: 'middle'}}
                   width='16'
                   height='16'
                   viewBox='0 0 16 16'
                   fill='none'
                   xmlns='http://www.w3.org/2000/svg'>
                <circle cx='8' cy='8' r='8' fill='#2C2C57' />
              </svg>
              <Text color='#2C2C57' fontSize={0} ml={1}>Locked</Text>
            </Td>
            <Td>
              <Text fontSize={0}>
                {(100 * (lastLocked / (lastLocked + last.spawned + last.activated + last['set-networking-keys']))).toFixed(2)}
              </Text>
            </Td>
            <Td>
              <Text fontSize={0}>
                {lastLocked}
              </Text>
            </Td>
            <Td>
              <Text fontSize={0}>
                {((100 * ((lastLocked / firstLocked) - 1)).toFixed(2))}
              </Text>
            </Td>
            <Td>
              <Text fontSize={0}>
                {lastLocked - firstLocked}
              </Text>
            </Td>
          </Tr>}
        <Tr>
          <Td>
            <svg style={{verticalAlign: 'middle'}}
                 width='16'
                 height='16'
                 viewBox='0 0 16 16'
                 fill='none'
                 xmlns='http://www.w3.org/2000/svg'>
              <circle cx='8' cy='8' r='8' fill='#BF421B' />
            </svg>
            <Text color='#BF421B' fontSize={0} ml={1}>Spawned</Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {(100 * (last.spawned / (lastLocked + last.spawned + last.activated + last['set-networking-keys']))).toFixed(2)}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last.spawned}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {((100 * ((last.spawned / first.spawned) - 1)).toFixed(2))}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last.spawned - first.spawned}
            </Text>
          </Td>
        </Tr>
        <Tr>
          <Td>
            <svg style={{verticalAlign: 'middle'}}
                 width='16'
                 height='16'
                 viewBox='0 0 16 16'
                 fill='none'
                 xmlns='http://www.w3.org/2000/svg'>
              <circle cx='8' cy='8' r='8' fill='#DD9C34' />
            </svg>
            <Text color='#DD9C34' fontSize={0} ml={1}>Activated</Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {(100 * (last.activated / (lastLocked + last.spawned + last.activated + last['set-networking-keys']))).toFixed(2)}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last.activated}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {((100 * ((last.activated / first.activated) - 1)).toFixed(2))}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last.activated - first.activated}
            </Text>
          </Td>
        </Tr>
        <Tr>
          <Td>
            <svg style={{verticalAlign: 'middle'}}
                 width='16'
                 height='16'
                 viewBox='0 0 16 16'
                 fill='none'
                 xmlns='http://www.w3.org/2000/svg'>
              <circle cx='8' cy='8' r='8' fill='#219DFF' />
            </svg>
            <Text color='#219DFF' fontSize={0} ml={1}>Set Networking Keys</Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {(100 * (last['set-networking-keys'] / (lastLocked + last.spawned + last.activated + last['set-networking-keys']))).toFixed(2)}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last['set-networking-keys']}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {((100 * ((last['set-networking-keys'] / first['set-networking-keys']) - 1)).toFixed(2))}
            </Text>
          </Td>
          <Td>
            <Text fontSize={0}>
              {last['set-networking-keys'] - first['set-networking-keys']}
            </Text>
          </Td>
        </Tr>
      </tbody>
    </Table>
  );
}
