import React from 'react';

import { Icon,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text } from '@tlon/indigo-react';

export function NodeMenu(props) {

  const { fetchPkiEvents,
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

  return (
    <Menu>
      <Text
        color='gray'
        fontWeight={400}
        fontSize={2}
        ml='34px'
      >
        Nodes
      </Text>
      <MenuButton
        style={{cursor: 'pointer'}}
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
