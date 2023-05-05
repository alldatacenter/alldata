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
import './calendar.scss';

interface TopPartOfCalendarProps {
  value: string;
  x1Line: number;
  y1Line: number;
  y2Line: number;
  xText: number;
  yText: number;
}

export const TopPartOfCalendar: React.FC<TopPartOfCalendarProps> = ({
  value,
  x1Line,
  y1Line,
  y2Line,
  xText,
  yText,
}) => {
  return (
    <g className="erda-gantt-calendar-top">
      <line
        x1={x1Line}
        y1={y1Line}
        x2={x1Line}
        y2={y2Line}
        className={'erda-gantt-calendar-top-tick'}
        key={`${value}line`}
      />
      <text key={`${value}text`} y={yText} x={xText} className={'erda-gantt-calendar-top-text'}>
        {value}
      </text>
    </g>
  );
};
