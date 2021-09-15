import React from 'react';

import { Icon,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text } from '@tlon/indigo-react';


export function TimeRangeMenu(props) {
  return (
    <Menu>
      <Text
        color='gray'
        fontWeight={400}
        fontSize={2}
      >
        Time Range
      </Text>
      <MenuButton
        style={{cursor: 'pointer'}}
        border='none'
        height='auto'
        width='auto'
        fontSize={2}
      >
        6 months <Icon ml='10px' icon='ChevronSouth' size={12} />
      </MenuButton>
    </Menu>
  );
}
