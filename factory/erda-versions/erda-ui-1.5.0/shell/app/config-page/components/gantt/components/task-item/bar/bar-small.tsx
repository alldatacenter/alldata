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
import { getProgressPoint } from '../../../helpers/bar-helper';
import { BarDisplay } from './bar-display';
import { BarProgressHandle } from './bar-progress-handle';
import { TaskItemProps } from '../task-item';
import './bar.scss';

export const BarSmall: React.FC<TaskItemProps> = ({
  task,
  isProgressChangeable,
  isDateChangeable,
  onEventStart,
  isSelected,
}) => {
  const progressPoint = getProgressPoint(task.progressWidth + task.x1, task.y, task.height);
  return (
    <g className={'erda-gantt-bar-wrapper'} tabIndex={0}>
      <BarDisplay
        x={task.x1}
        y={task.y}
        width={task.x2 - task.x1}
        height={task.height}
        progressX={task.progressX}
        progressWidth={task.progressWidth}
        barCornerRadius={task.barCornerRadius}
        styles={task.styles}
        isSelected={isSelected}
        onMouseDown={(e) => {
          isDateChangeable && onEventStart('move', task, e);
        }}
      />
      <g className="handleGroup">
        {isProgressChangeable && (
          <BarProgressHandle
            progressPoint={progressPoint}
            onMouseDown={(e) => {
              onEventStart('progress', task, e);
            }}
          />
        )}
      </g>
    </g>
  );
};
