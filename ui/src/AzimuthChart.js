import React from 'react';

import { ResponsiveContainer,
         BarChart,
         CartesianGrid,
         XAxis,
         Tooltip,
         Bar } from 'recharts';

export function AzimuthChart(props) {

  const { events, fill, name } = props;

  return(
    <ResponsiveContainer>
      <BarChart
        barCategoryGap={0}
        data={events}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          hide={true}
          xAxisId='0'
          dataKey='date'
        />
        <XAxis
          xAxisId='1'
          dataKey='month'
          interval={30}
          padding={{left: 10}}
        />
        <Tooltip />
        <Bar name={name} dataKey='count' fill={fill}/>
      </BarChart>
    </ResponsiveContainer >
  );

}
