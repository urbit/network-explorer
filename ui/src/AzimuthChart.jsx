import React from 'react';

import { ResponsiveContainer,
         BarChart,
         CartesianGrid,
         XAxis,
         Tooltip,
         Bar } from 'recharts';

export function AzimuthChart(props) {

  const { events, fill, name, timeRangeText } = props;

  return(
    <ResponsiveContainer>
      <BarChart
        barCategoryGap={0}
        data={events}>
        <CartesianGrid strokeDasharray="3 3" />
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
        <Tooltip />
        <Bar name={name} dataKey='count' fill={fill}/>
      </BarChart>
    </ResponsiveContainer >
  );

}
