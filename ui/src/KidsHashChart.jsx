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
    Object.entries(rest).forEach(([k, v]) =>{
      acc[k] = v.hash;
    });
    return acc;
  }, {});

  const bars = Object.entries(bs).map( ([k, v]) => {
    return <Bar dataKey={k} fill={stringToColor(v)} stackId='a'/>;
  });

  let data = [];
  kidsHashes.forEach(e => {
    let o = {};
    for (const p in e) {
      if (p === 'day') {
        o[p] = e['day'];
      } else {
        o[p] = e[p].count;
      }
    }
    data.push(o);
  });

  return(
    <ResponsiveContainer height='100%'>
      <BarChart
        data={data}>
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
        <YAxis
          type='number'
          orientation='right'
          tick={{fontSize: 10}}
          tickCount={7}
          domain={[0, dataMax => Math.ceil(dataMax / 100) * 100,]} />
        <Tooltip />
        { bars }
      </BarChart>
    </ResponsiveContainer >
  );
}
