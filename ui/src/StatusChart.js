import React from 'react';

import { ResponsiveContainer,
         Area,
         AreaChart,
         CartesianGrid,
         Legend,
         XAxis,
         YAxis,
         Tooltip} from 'recharts';

export function StatusChart(props) {

  const { events, timeRangeText, nodesText } = props;

  const showLockedData = nodesText === 'Stars';

  return(
    <ResponsiveContainer height='75%' className='h-40'>
      <AreaChart
        data={events}>
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
        {showLockedData &&
          <Area dot={false} name='Locked' dataKey='locked' stroke='#2C2C57' fill='#2C2C57' />}
        <Area dot={false} name='Spawned' dataKey='spawned' stroke='#BF421B' fill='#BF421B' />
        <Area dot={false} name='Activated' dataKey='activated' stroke='#DD9C34' fill='#DD9C34' />
        <Area dot={false} name='Set Networking Keys' dataKey='set-networking-keys' stroke='#219DFF' fill='#219DFF'/>
        <Area dot={false} name='Online' dataKey='online' stroke='#00B171' fill='#00B171'/>
      </AreaChart>
    </ResponsiveContainer >
  );
}
