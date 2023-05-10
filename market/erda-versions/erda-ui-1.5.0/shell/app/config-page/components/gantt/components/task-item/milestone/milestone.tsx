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
import { TaskItemProps } from '../task-item';
import './milestone.scss';

export const Milestone: React.FC<TaskItemProps> = ({ task, isDateChangeable, onEventStart, isSelected }) => {
  const transform = `rotate(45 ${task.x1 + task.height * 0.356}
    ${task.y + task.height * 0.85});translate(${task.x1},${task.y})`;
  const getBarColor = () => {
    return isSelected ? task.styles.backgroundSelectedColor : task.styles.backgroundColor;
  };

  return (
    <g tabIndex={0} className={'erda-gantt-milestone-wrapper'}>
      <rect
        fill={getBarColor()}
        // transform={`translate(${task.x1},${task.y})`}
        // x={task.x1}
        // y={task.y}
        width={task.height}
        height={task.height}
        rx={task.barCornerRadius}
        ry={task.barCornerRadius}
        transform={transform}
        className={'erda-gantt-milestone-background'}
        onMouseDown={(e) => {
          isDateChangeable && onEventStart('move', task, e);
        }}
      />
    </g>
  );
};
