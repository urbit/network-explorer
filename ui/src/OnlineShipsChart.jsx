import React from 'react';

import { ResponsiveContainer,
         Bar,
         CartesianGrid,
         BarChart,
         XAxis,
         YAxis,
         Tooltip} from 'recharts';

export function OnlineShipsChart(props) {

  const { onlineShips, timeRangeText } = props;

  return(
    <ResponsiveContainer height='100%'>
      <BarChart
        data={onlineShips}>
        <CartesianGrid strokeDasharray='3 3' />
        <XAxis
          xAxisId='0'
          dataKey='date'
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
        <Bar dot={false} name='Retained' dataKey='retained' stroke='#219DFF' fill='#219DFF' stackId='a' />
        <Bar dot={false} name='Churned' dataKey='churned' stroke='#BF421B' fill='#BF421B' stackId='a' />
        <Bar dot={false} name='New' dataKey='new' stroke='#00B171' fill='#00B171' stackId='a' />
        <Bar dot={false} name='Resurrected' dataKey='resurrected' stroke='#DD9C34' fill='#DD9C34' stackId='a'  />
      </BarChart>
    </ResponsiveContainer >
  );
}
