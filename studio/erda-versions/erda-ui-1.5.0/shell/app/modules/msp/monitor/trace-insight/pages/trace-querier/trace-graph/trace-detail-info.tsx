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
import { displayTimeString } from './utils';

interface IProps {
  dataSource: MONITOR_TRACE.ITrace;
}

export const TraceDetailInfo = ({ dataSource }: IProps) => {
  const { duration, serviceCount = 0, depth = 0, spanCount = 0 } = dataSource;
  const arr = [
    { text: displayTimeString(duration), subText: 'Duration' },
    { text: serviceCount, subText: 'Services' },
    { text: depth, subText: 'Depth' },
    { text: spanCount, subText: 'Total Spans' },
  ];

  return (
    <div className="bg-grey flex justify-between items-center py-2 my-4">
      {arr.map(({ text, subText }) => (
        <div className="flex flex-col flex-1 items-center justify-center" key={subText}>
          <div className="text-xl text-sub font-semibold">{text}</div>
          <div className="text-xs text-darkgray">{subText}</div>
        </div>
      ))}
    </div>
  );
};
