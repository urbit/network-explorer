import React from 'react';

import { Icon,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text } from '@tlon/indigo-react';

export function NodeMenu(props) {

  const { disabled,
          fetchPkiEvents,
          fetchAggregateEvents,
          nodesText,
          setNodesText } = props;

  const onSelect = nodes => {
    if (nodes === nodesText) {
      return;
    }
    setNodesText(nodes);
    fetchPkiEvents(nodes);
    fetchAggregateEvents(nodes);
  };

  const buttonStyle = disabled ? {} : {cursor: 'pointer'};

  return (
    <Menu>
      <Text
        color={disabled ? 'lightGray' : 'gray'}
        fontWeight={400}
        fontSize={2}
        className='headerMargin'
        ml='34px'
      >
        Nodes
      </Text>
      <MenuButton
        disabled={disabled}
        style={buttonStyle}
        backgroundColor='white'
        border='none'
        height='auto'
        width='auto'
        fontSize={2}
      >
        {nodesText} <Icon ml='10px' icon='ChevronSouth' size={12} />
      </MenuButton>
      <MenuList>
        <MenuItem onSelect={() => onSelect('All')}>All</MenuItem>
        <MenuItem onSelect={() => onSelect('Planets')}>Planets</MenuItem>
        <MenuItem onSelect={() => onSelect('Stars')}>Stars</MenuItem>
        <MenuItem onSelect={() => onSelect('Galaxies')}>Galaxies</MenuItem>
      </MenuList>
    </Menu>
  );
}
