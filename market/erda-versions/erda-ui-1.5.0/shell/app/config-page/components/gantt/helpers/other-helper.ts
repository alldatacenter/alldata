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

import { BarTask } from '../types/bar-task';
import { Task } from '../types/public-types';

export function isKeyboardEvent(
  event: React.MouseEvent | React.KeyboardEvent | React.FocusEvent,
): event is React.KeyboardEvent {
  return (event as React.KeyboardEvent).key !== undefined;
}

export function isMouseEvent(
  event: React.MouseEvent | React.KeyboardEvent | React.FocusEvent,
): event is React.MouseEvent {
  return (event as React.MouseEvent).clientX !== undefined;
}

export function isBarTask(task: Task | BarTask): task is BarTask {
  return (task as BarTask).x1 !== undefined;
}

export function removeHiddenTasks(tasks: Task[]) {
  const groupedTasks = tasks.filter((t) => t.hideChildren && t.type === 'project');
  if (groupedTasks.length > 0) {
    for (let i = 0; groupedTasks.length > i; i++) {
      const groupedTask = groupedTasks[i];
      const children = getChildren(tasks, groupedTask);
      tasks = tasks.filter((t) => children.indexOf(t) === -1);
    }
  }
  return tasks;
}

function getChildren(taskList: Task[], task: Task) {
  let tasks: Task[] = [];
  if (task.type !== 'project') {
    tasks = taskList.filter((t) => t.dependencies && t.dependencies.indexOf(task.id) !== -1);
  } else {
    tasks = taskList.filter((t) => t.project && t.project === task.id);
  }
  const taskChildren = tasks.reduce((children: Task[], t) => children.concat(children, getChildren(taskList, t)), []);
  tasks = tasks.concat(tasks, taskChildren);
  return tasks;
}
