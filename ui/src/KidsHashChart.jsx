import React from 'react';

import { ResponsiveContainer,
         Bar,
         CartesianGrid,
         BarChart,
         XAxis,
         YAxis,
         Tooltip} from 'recharts';

const stringToColor = str => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  let color = '#';
  for (let i = 0; i < 3; i++) {
    let value = (hash >> (i * 8)) & 0xFF;
    color += ('00' + value.toString(16)).substr(-2);
  }
  return color;
};

export function KidsHashChart(props) {

  const { kidsHashes, timeRangeText } = props;

  const bs = kidsHashes.reduce((acc, e) => {
    const { day, ...rest} = e;
    Object.keys(rest).forEach(x => acc.add(x));
    return acc;
  }, new Set());

  const bars = [...bs].map( e => <Bar dataKey={e} fill={stringToColor(e)} stackId='a'/>);

  return(
    <ResponsiveContainer height='100%'>
      <BarChart
        data={kidsHashes}>
        <CartesianGrid strokeDasharray='3 3' />
        <XAxis
          xAxisId='0'
          dataKey='day'
          minTickGap={50}
          tickFormatter={e => {
            if (timeRangeText === 'Year' || timeRangeText === 'All') {
              return (new Date(e)).toLocaleString('default', {month: 'short', 'year': '2-digit'});
            }
            return (new Date(e)).toLocaleString('default', {month: 'short'});
          }}
        />
        <YAxis type='number' hide={true}  />
        <Tooltip />
        { bars }
      </BarChart>
    </ResponsiveContainer >
  );
}
