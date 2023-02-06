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
import { ErdaIcon } from 'common';
import { useUpdateEffect } from 'react-use';
import moment from 'moment';
import { TaskItemProps } from '../task-item';
import './bar.scss';

export const Bar: React.FC<TaskItemProps> = ({
  task,
  isDateChangeable,
  onEventStart,
  isSelected,
  BarContentRender,
  ganttEvent,
  isMoving,
}) => {
  const [curPos, setCurPos] = React.useState({
    x2: task.x2,
    x1: task.x1,
    height: task.height,
    y: task.y,
    start: task.start,
    end: task.end,
  });

  useUpdateEffect(() => {
    if (!(isSelected && isMoving)) {
      setCurPos({
        x2: task.x2,
        x1: task.x1,
        height: task.height,
        y: task.y,
        start: task.start,
        end: task.end,
      });
    }
  }, [isSelected, task, isSelected, isMoving]);

  const { changedTask } = ganttEvent || {};

  const handleHeight = task.height;
  const taskWidth = task.x2 - task.x1;

  return (
    <g className={'erda-gantt-bar-wrapper'} tabIndex={0}>
      {isDateChangeable && (
        <foreignObject
          transform={`translate(${task.x1 - 14},${task.y})`}
          onMouseDown={(e) => {
            isDateChangeable && onEventStart('move', task, e);
          }}
          width={taskWidth + 28}
          className="overflow-visible"
          height={handleHeight}
        >
          <div
            className="erda-gantt-bar-container relative"
            onMouseDown={(e) => {
              isDateChangeable && onEventStart('move', task, e);
            }}
          >
            <div
              style={{
                transform: `translate(${curPos.x1 - task.x1}px,${-2}px)`,
                width: curPos.x2 - curPos.x1 + 4,
                height: curPos.height + 4,
                left: 12,
              }}
              className={`erda-gantt-bar-preview-box absolute text-sm text-desc bg-white bg-opacity-100 truncate ${
                isMoving && changedTask && task.id === changedTask.id ? 'visible' : 'invisible'
              }`}
            >
              {moment(curPos.start).format('MM-DD')}~{moment(curPos.end).format('MM-DD')}
            </div>
            <div
              className={`relative rounded erda-gantt-bar-box text-default-8 ${
                changedTask?.id === task.id ? 'on-hover' : ''
              }`}
              style={{
                left: 14,
                top: 0,
                width: taskWidth,
                height: handleHeight,
              }}
            >
              {BarContentRender}
            </div>
            <div
              className="erda-gantt-bar-handle-box absolute"
              style={{
                width: taskWidth / 2 + 14,
                height: handleHeight,
                top: 0,
                left: 0,
              }}
            >
              <span
                className="erda-gantt-bar-handle left-handle -ml-0.5"
                onMouseDown={(e) => {
                  e.stopPropagation();
                  onEventStart('start', task, e);
                }}
              >
                <ErdaIcon className="erda-gantt-bar-handle-icon" type={'xiangzuolashen'} />
              </span>
            </div>
            <div
              className="erda-gantt-bar-handle-box absolute text-right"
              style={{
                width: taskWidth / 2 + 14,
                height: handleHeight,
                left: 14 + taskWidth / 2,
                top: 0,
              }}
            >
              <span
                className="erda-gantt-bar-handle right-handle -mr-0.5"
                onMouseDown={(e) => {
                  e.stopPropagation();
                  onEventStart('end', task, e);
                }}
              >
                <ErdaIcon className="erda-gantt-bar-handle-icon" type={'xiangyoulashen'} />
              </span>
            </div>
          </div>
        </foreignObject>
      )}
    </g>
  );
};
