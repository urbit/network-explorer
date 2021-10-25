import React from 'react';

import { Row,
         LoadingSpinner,
         Center,
         Icon,
         Table,
         Tr,
         Text} from '@tlon/indigo-react';

import { SponsoredNode } from './SponsoredNode';

export function SponsoredNodes(props) {

  const { loading, kids, sponsor, header } = props;

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
                 <Text fontWeight={400} fontSize={0} color='gray'>Node</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Sponsor</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Key Revision</Text>
               </th>
               <th style={{borderBottom: '1px solid rgba(0, 0, 0, 0.04)'}}>
                 <Text fontWeight={400} fontSize={0} color='gray'>Owners</Text>
               </th>
             </Tr>
           </thead>
           <tbody>
             {kids.map(e => <SponsoredNode key={e['urbit-id']} sponsor={sponsor} {...e}/>)}
           </tbody>
         </Table>
       </>
      }
    </>
  );
}
