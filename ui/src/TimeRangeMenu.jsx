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

export function TimeRangeMenu(props) {

  const { disabled,
          fetchPkiEvents,
          fetchAggregateEvents,
          timeRangeText,
          setTimeRangeText } = props;

  const onSelect = timeRange => {
    if (timeRange === timeRangeText) {
      return;
    }
    const timeRangeMap = {'All': 'all', 'Year': 'year', '6 Months': 'sixMonths', 'Month': 'month', 'Week': 'week'};
    setUrlParam('timeRange', timeRangeMap[timeRange]);

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
