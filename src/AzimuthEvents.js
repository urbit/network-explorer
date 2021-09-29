import React from 'react';

import { Row,
         LoadingSpinner,
         Center,
         Icon,
         Table,
         Tr,
         Text} from '@tlon/indigo-react';

import { AzimuthEvent } from './AzimuthEvent';

export function AzimuthEvents(props) {

  const { header, loading, events } = props;

  return (
    <>
      <Row justifyContent='space-between'>
        <Text fontSize={0} fontWeight={500}>{header}</Text>
        <Icon icon='Info' size={16} cursor='pointer' />
      </Row>
      {loading ?
       <Center height='100%'>
         <LoadingSpinner
           width='36px'
           height='36px'
           foreground='rgba(0, 0, 0, 0.6)'
           background='rgba(0, 0, 0, 0.2)'
         />
       </Center> :
       <>
         <Table mt={2} border='0' width='100%'>
           <thead>
             <Tr textAlign='left' pb={2} >
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Event Type</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Node</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Data</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Time</Text>
               </th>
             </Tr>
           </thead>
           <tbody>
             {events.length > 0 && events.map(e =>
               <AzimuthEvent key={e.node['urbit-id'] + e.time + e.type + e.address} {...e}/>)}
           </tbody>
         </Table>
         {(events.length === 0) &&
          <Center height='75%'>
            <Text color='gray' fontSize={0}>No Events Yet</Text>
          </Center>}
       </>
      }
    </>
  );
}
