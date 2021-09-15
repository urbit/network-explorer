import React from 'react';

import { ResponsiveContainer,
         BarChart,
         ReferenceLine,
         XAxis,
         Tooltip,
         Bar } from 'recharts';

export function AzimuthChart(props) {

  const { events, months, fill, name } = props;

  return(
    <ResponsiveContainer>
      <BarChart
        barCategoryGap={0}
        data={events}>
        <XAxis
          hide={true}
          xAxisId='0'
          dataKey='date'
        />
        <XAxis
          xAxisId='1'
          dataKey='month'
          allowDuplicatedCategory={false}
        />
        <Tooltip />
        <Bar name={name} dataKey='count' fill={fill}/>
        {[...months.values()].map(month => {
          return <ReferenceLine xAxisId='1' x={month} stroke='rgba(0, 0, 0, 0.2)' />;
        })}
      </BarChart>
    </ResponsiveContainer >
  );

}
