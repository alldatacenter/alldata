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

import React, { useEffect, useRef } from 'react';
import { BarTask } from '../../types/bar-task';
import { Task } from '../../types/public-types';
import './task-list.scss';

export type TaskListProps = {
  headerHeight: number;
  rowWidth: string;
  fontFamily: string;
  fontSize: string;
  rowHeight: number;
  ganttHeight: number;
  locale: string;
  tasks: Task[];
  taskListRef: React.RefObject<HTMLDivElement>;
  selectedTask: BarTask | undefined;
  setSelectedTask: (task: string) => void;
  onExpanderClick: (task: Task) => void;
  TaskListHeader: React.FC<{
    headerHeight: number;
    rowWidth: string;
    fontFamily: string;
    fontSize: string;
  }>;
  TaskListTable: React.FC<{
    rowHeight: number;
    rowWidth: string;
    fontFamily: string;
    fontSize: string;
    locale: string;
    tasks: Task[];
    selectedTaskId: string;
    setSelectedTask: (taskId: string) => void;
    onExpanderClick: (task: Task) => void;
  }>;
};

export const TaskList: React.FC<TaskListProps> = ({
  headerHeight,
  verticalRange,
  fontFamily,
  fontSize,
  rowWidth,
  rowHeight,
  tasks,
  selectedTask,
  setSelectedTask,
  onExpanderClick,
  locale,
  ganttHeight,
  scrollX,
  taskListRef,
  TaskListHeader,
  TaskListTable,
}) => {
  const headerProps = {
    headerHeight,
    fontFamily,
    fontSize,
    rowWidth,
  };
  const selectedTaskId = selectedTask ? selectedTask.id : '';
  const tableProps = {
    rowHeight,
    rowWidth,
    fontFamily,
    fontSize,
    tasks,
    locale,
    selectedTaskId,
    setSelectedTask,
    onExpanderClick,
    verticalRange,
  };

  return (
    <div ref={taskListRef} className={`erda-gantt-task-list-box ${scrollX > 1 ? 'shadow-card' : ''}`}>
      <TaskListHeader {...headerProps} />
      <TaskListTable {...tableProps} />
    </div>
  );
};
