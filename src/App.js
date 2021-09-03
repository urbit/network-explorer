import './App.css';
import './fonts.css';
import { Box,
         Row,
         StatelessTextInput,
         Text,
         Col,
         Button,
         Center } from '@tlon/indigo-react';

function App() {
  return (
    <div className='App'>
      <Box
        width='100%'
        height='100%'
        display='flex'
        flexWrap='wrap'
        justifyContent='center'
        alignItems='center'
      >
        <Col
          p={3}
          backgroundColor='white'
          color='washedGray'
          borderRadius='2'
          ml={3}
          border={1}
          cursor='text'
          flex='1 1'
        >
          <Box>
            <Text color='gray' fontSize={2}>
              Urbit
            </Text>
            <Text ml={1} fontSize={2}>
              / Network explorer
            </Text>
          </Box>
        </Col>
        <Col
          p={3}
          backgroundColor='white'
          color='washedGray'
          borderRadius={2}
          ml={3}
          border={1}
          cursor='text'
          flex='1 1'
          alignItems='flex-end'
        >
          <StatelessTextInput
            placeholder='Search for a node...'
            backgroundColor='rgba(0, 0, 0, 0.04)'
            fontWeight={400}
            height={40}
            width={256}
          />
        </Col>
      </Box>
    </div>
  );
}

export default App;
