import React from 'react';
import { useState } from 'react';

import { withRouter } from 'react-router-dom';

import { Box,
         Row,
         StatelessTextInput,
         Text} from '@tlon/indigo-react';

import { isValidPatp } from 'urbit-ob';

function SearchHeader(props) {

  const { history } = props;

  const [nodeSearch, setNodeSearch] = useState('');

  const [hasError, setHasError] = useState(false);

  return (
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
          value={nodeSearch}
          placeholder='Search for a node...'
          backgroundColor='rgba(0, 0, 0, 0.04)'
          borderRadius='4px'
          fontWeight={400}
          height={40}
          width={256}
          hasError={hasError}
          onChange={e => {
            setNodeSearch(e.target.value);
            setHasError(false);
          }}
          onKeyPress={e => {
            if (e.key === 'Enter') {
              if (isValidPatp(nodeSearch)) {
                history.push('/'+ nodeSearch);
              } else {
                setHasError(true);
              }
            }
          }}
        />
      </Box>
    </Row>
  );
}

export default withRouter(SearchHeader);
