import React from 'react';

import { ResponsiveContainer,
         Area,
         AreaChart,
         CartesianGrid,
         Legend,
         XAxis,
         YAxis,
         Tooltip} from 'recharts';

export function BootedChart(props) {

  const { events, timeRangeText, nodesText } = props;

  const showLockedData = nodesText === 'Stars';

  return(
    <ResponsiveContainer height='100%' className='h-40'>
      <AreaChart
        data={events}>
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
          domain={[0,
                   dataMax => Math.ceil(dataMax / 100) * 100,]} />
        <Tooltip />
        <Area dot={false} name='Booted' dataKey='booted' stroke='#00B171' fill='#00B171'/>
      </AreaChart>
    </ResponsiveContainer >
  );
}
