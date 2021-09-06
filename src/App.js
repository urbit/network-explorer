import './App.css';
import './fonts.css';
import { Box,
         Row,
         StatelessTextInput,
         Icon,
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
        height='100%'
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
                flexShrink='0'
                color='gray'
                fontWeight={400}
                fontSize={2}
              >
                Time Range
              </Text>
              <MenuButton
                flexShrink='0'
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
                flexShrink='0'
                color='gray'
                fontWeight={400}
                fontSize={2}
                ml='34px'
              >
                Nodes
              </Text>
              <MenuButton
                flexShrink='0'
                border='none'
                height='auto'
                width='auto'
                fontSize={2}
              >
                All <Icon ml='10px' icon='ChevronSouth' size={12} />
              </MenuButton>
            </Menu>
            {/* <Text color='gray' fontSize={2}> */}
            {/*   Time Range */}
            {/* </Text> */}
            {/* <Text color='gray' ml={1} fontSize={2}> */}
            {/*   Nodes */}
            {/* </Text> */}
          </Box>
        </Box>
      </Row>
    </div>
  );
}

export default App;
