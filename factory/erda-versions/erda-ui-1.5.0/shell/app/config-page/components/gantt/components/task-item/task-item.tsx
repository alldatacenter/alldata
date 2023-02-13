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

import React, { useEffect, useState } from 'react';
import { BarTask } from '../../types/bar-task';
import { GanttContentMoveAction } from '../../types/gantt-task-actions';
import { Bar } from './bar/bar';
import { BarSmall } from './bar/bar-small';
import { Milestone } from './milestone/milestone';
import { ErdaIcon } from 'common';
import moment from 'moment';
import { Project } from './project/project';
import './task-list.scss';

export type TaskItemProps = {
  task: BarTask;
  arrowIndent: number;
  taskHeight: number;
  isProgressChangeable: boolean;
  isDateChangeable: boolean;
  isDelete: boolean;
  isSelected: boolean;
  rtl: boolean;
  ganttEvent: Obj;
  isMoving: boolean;
  BarContentRender?: React.ReactNode;
  displayWidth: number;
  dates: Date[];
  svgWidth: number;
  rowHeight: number;
  horizontalRange: number[];
  setSelectedTask: (id: string) => void;
  columnWidth: number;
  onEventStart: (
    action: GanttContentMoveAction,
    selectedTask: BarTask,
    event?: React.MouseEvent | React.KeyboardEvent,
  ) => any;
};

export const TaskItem: React.FC<TaskItemProps> = (props) => {
  const {
    task,
    isSelected,
    onEventStart,
    BarContentRender,
    displayWidth,
    setSelectedTask,
    columnWidth,
    dates,
    horizontalRange,
    rowHeight,
    svgWidth,
  } = {
    ...props,
  };
  const [taskItem, setTaskItem] = useState<JSX.Element | null>(null);
  const [isHover, setIsHover] = useState(false);

  useEffect(() => {
    switch (task.typeInternal) {
      case 'milestone':
        setTaskItem(<Milestone {...props} />);
        break;
      case 'project':
        setTaskItem(<Project {...props} />);
        break;
      case 'smalltask':
        setTaskItem(<BarSmall {...props} />);
        break;
      default:
        setTaskItem(<Bar {...props} />);
        break;
    }
  }, [task, isSelected]);

  if (task.start && task.end) {
    let PointIcon = null;
    const curDates = dates.slice(...horizontalRange);

    const displayPos = Math.floor(displayWidth / columnWidth);
    if (curDates?.[0] && task.end < curDates[0]) {
      PointIcon = (
        <div className="text-default-2 hover:text-default-4 erda-gantt-grid-arrow-box flex items-center">
          <ErdaIcon
            className="cursor-pointer "
            type="zuo"
            size={20}
            onClick={() => {
              setSelectedTask(task.id);
            }}
          />
          <div className="erda-gantt-grid-arrow text-default-6">
            {moment(task.start).format('MM-DD')} ~ {moment(task.end).format('MM-DD')}
          </div>
        </div>
      );
    } else if (curDates?.[displayPos] && task.start > curDates[displayPos]) {
      PointIcon = (
        <div
          className="text-default-2 hover:text-default-4 erda-gantt-grid-arrow-box flex items-center"
          style={{ marginLeft: displayWidth - 20 - 80 }}
        >
          <div className="erda-gantt-grid-arrow text-default-6">
            {moment(task.start).format('MM-DD')} ~ {moment(task.end).format('MM-DD')}
          </div>
          <ErdaIcon className="cursor-pointer" onClick={() => setSelectedTask(task.id)} type="you" size={20} />
        </div>
      );
    }
    return (
      <g
        onKeyDown={(e) => {
          e.stopPropagation();
        }}
        onMouseEnter={(e) => {
          onEventStart('mouseenter', task, e);
          setIsHover(true);
        }}
        onMouseLeave={(e) => {
          onEventStart('mouseleave', task, e);
          setIsHover(false);
        }}
        onDoubleClick={(e) => {
          onEventStart('dblclick', task, e);
        }}
        onFocus={() => {
          onEventStart('select', task);
        }}
      >
        {PointIcon ? (
          <foreignObject x="0" y={task.y - 8} width={svgWidth} height={rowHeight}>
            <div className={`flex h-full`}>{PointIcon}</div>
          </foreignObject>
        ) : (
          taskItem &&
          React.cloneElement(taskItem, {
            BarContentRender: BarContentRender ? (
              <BarContentRender task={task} isHover={isHover} />
            ) : (
              <div>{task.name}</div>
            ),
          })
        )}
      </g>
    );
  } else {
    return null;
  }
};
