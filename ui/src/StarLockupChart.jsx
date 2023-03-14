import React from 'react';

import { ResponsiveContainer,
         Bar,
         CartesianGrid,
         BarChart,
         XAxis,
         YAxis,
         Tooltip} from 'recharts';

import { lockupData } from './LockupData';


const ONE_HOUR = 1000 * 60 * 60;
const ONE_DAY = ONE_HOUR * 24;
const ONE_WEEK = ONE_DAY * 7;
const ONE_MONTH = ONE_DAY * 30;
const SIX_MONTHS = ONE_MONTH * 6;
const ONE_YEAR = ONE_MONTH * 12;

const filterData = (lockupData, timeRangeText) => {
    const now = new Date();
    const m = {
        'Day': new Date(now - ONE_DAY),
        'Week': new Date(now - ONE_WEEK),
        'Month': new Date(now - ONE_MONTH),
        '6 Months': new Date(now - SIX_MONTHS),
        'Year': new Date(now - ONE_YEAR),
    };

    return lockupData.filter(e => (new Date(e.date) > m[timeRangeText]) && (new Date(e.date) < now) );

};

export function StarLockupChart(props) {

  const { timeRangeText } = props;

    const data = timeRangeText === 'All' ? lockupData : filterData(lockupData, timeRangeText);

  return(
    <ResponsiveContainer height='100%'>
      <BarChart
        data={data}>
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
        <Bar dataKey='tlon' fill='#BF421B' stackId='a'/>
        <Bar dataKey='uf' fill='#DD9C34' stackId='a'/>
        <Bar dataKey='others' fill='#00B171' stackId='a'/>
      </BarChart>
    </ResponsiveContainer >
  );
}
