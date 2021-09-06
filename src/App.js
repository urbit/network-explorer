import './App.css';
import './fonts.css';
import { Box,
         Row,
         StatelessTextInput,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text,
         Col,
         Button,
         Center } from '@tlon/indigo-react';

function App() {
  return (
    <div className='App'>
      <Row
        justifyContent='center'
        alignItems='center'
        borderBottom='1px solid rgba(0, 0, 0, 0.1)'
      >
        <Box
          p={3}
          flex='auto'
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
        height='100%'
        justifyContent='center'
        alignItems='center'
      >
        <Box
          p={3}
          flex='auto'
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
          <Box>
            <Text color='gray' fontSize={2}>
              Time Range
            </Text>
            <Text color='gray' ml={1} fontSize={2}>
              Nodes
            </Text>
            <Menu>
              <MenuButton>
                Actions <span aria-hidden>▾</span>
              </MenuButton>
            </Menu>
          </Box>
        </Box>
      </Row>
    </div>
  );
}

export default App;
