import React from 'react';

import { Icon,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text } from '@tlon/indigo-react';


const setUrlParam = (key, value) => {
  if (window.history.pushState) {
    let searchParams = new URLSearchParams(window.location.search);
    searchParams.set(key, value);
    let newurl = window.location.protocol + '//' + window.location.host + window.location.pathname + '?' + searchParams.toString();
    window.history.pushState({path: newurl}, '', newurl);
  }
};


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

    const nodeMap = {'All': 'all', 'Planets': 'planet', 'Stars': 'star', 'Galaxies': 'galaxy'};
    setUrlParam('nodes', nodeMap[nodes]);

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
