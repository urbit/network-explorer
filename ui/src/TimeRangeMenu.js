import React from 'react';

import { Icon,
         Menu,
         MenuList,
         MenuItem,
         MenuButton,
         Text } from '@tlon/indigo-react';

export function TimeRangeMenu(props) {

  const { disabled,
          fetchPkiEvents,
          fetchAggregateEvents,
          timeRangeText,
          setTimeRangeText} = props;

  const onSelect = timeRange => {
    if (timeRange === timeRangeText) {
      return;
    }
    setTimeRangeText(timeRange);
    fetchPkiEvents(timeRange);
    fetchAggregateEvents(timeRange);
  };

  const buttonStyle = disabled ? {} : {cursor: 'pointer'};

  return (
    <Menu>
      <Text
        color={disabled ? 'lightGray' : 'gray'}
        fontWeight={400}
        fontSize={2}
      >
        Time Range
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
        {timeRangeText} <Icon ml='10px' icon='ChevronSouth' size={12} />
      </MenuButton>
      <MenuList>
        {/* <MenuItem onSelect={() => onSelect('Day')}>Day</MenuItem> */}
        <MenuItem onSelect={() => onSelect('Week')}>Week</MenuItem>
        <MenuItem onSelect={() => onSelect('Month')}>Month</MenuItem>
        <MenuItem onSelect={() => onSelect('6 Months')}>6 Months</MenuItem>
        <MenuItem onSelect={() => onSelect('Year')}>Year</MenuItem>
        <MenuItem onSelect={() => onSelect('All')}>All</MenuItem>
      </MenuList>
    </Menu>
  );
}
