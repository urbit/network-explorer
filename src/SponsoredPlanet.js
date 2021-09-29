import React from 'react';
import { Link } from 'react-router-dom';

import { Tr, Td, Text } from '@tlon/indigo-react';
import { sigil, reactRenderer } from '@tlon/sigil-js';


export function SponsoredPlanet(props) {

  const { revision, continuity, sponsor } = props;
  const urbitId = props['urbit-id'];
  const owners = props['num-owners'];

  let sig = sigil({patp: urbitId,
                   renderer: reactRenderer,
                   size: 16,
                   colors: ['white', 'black']});

  sig.props.style.display = 'inline';
  sig.props.style.verticalAlign = 'middle';


  return (
    <Tr>
      <Td>
        {sig}
        <Link to={'/' + urbitId} style={{textDecoration:'none'}}>
          <Text
            fontFamily='Source Code Pro !important'
            ml={1}
            color='gray'
            fontSize={0}
            verticalAlign='middle'
          >
            {urbitId}
          </Text>
        </Link>
      </Td>
      <Td>
        <Text
          fontFamily='Source Code Pro !important'
          ml={1}
          color='gray'
          fontSize={0}
          verticalAlign='middle'
          borderRadius='2px'
          pl='4px'
          pr='4px'
          background='rgba(0, 0, 0, 0.04)'
        >
          {sponsor}
        </Text>
      </Td>
      <Td>
        <Text
          fontFamily='Source Code Pro !important'
          ml={1}
          color='gray'
          fontSize={0}
          verticalAlign='middle'
        >
          {revision || '0'}
        </Text>
      </Td>
      <Td>
        <Text
          fontFamily='Source Code Pro !important'
          ml={1}
          color='gray'
          fontSize={0}
          verticalAlign='middle'
        >
          {owners}
        </Text>
      </Td>
    </Tr>
  );
}
