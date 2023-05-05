// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import moment from 'moment';
import { monthMap } from 'app/locales/utils';
import './month-uptime.scss';

const MonthUptime = ({ timestamp, data = [] }: { timestamp: number; data?: any[] }) => {
  const momentObj = moment(timestamp);
  // 本月是几月
  const month = momentObj.get('month') + 1;
  // 本月第一天是周几
  const firstDayWeek = momentObj.set('date', 1).isoWeekday();
  // 本月有多少天
  const days = momentObj.daysInMonth();

  // 例如本月第一天是周3，前面就放2个透明的点
  const list = Array(firstDayWeek - 1).fill('opacity');

  const statusMapp = {
    Operational: 'success',
    'Major Outage': 'danger',
    Miss: 'grey',
  };

  for (let index = 0; index < days; index++) {
    list.push(statusMapp[data[index]] || 'default');
  }
  let start = 0;

  return (
    <div className="month-uptime-block">
      {/* <div className='month font-medium'>{month} 月</div> */}
      <ul className="day-point-list" data-month={monthMap[`${month}月`]}>
        {list.map((item, i) => {
          if (item !== 'opacity') start += 1;
          return <li key={String(i)} className={`day-point ${item}`} data-date={item !== 'opacity' ? start : null} />;
        })}
      </ul>
    </div>
  );
};

export default MonthUptime;
