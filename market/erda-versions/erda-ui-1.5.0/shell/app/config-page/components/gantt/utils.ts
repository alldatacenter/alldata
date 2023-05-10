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

import { groupBy, set, findIndex } from 'lodash';
import moment from 'moment';

export const convertDataForGantt = (
  data: { expandList: CP_GANTT.IData[]; updateList: CP_GANTT.IData[]; refresh?: boolean },
  prevList: CP_GANTT.IGanttData[],
) => {
  const { expandList, updateList, refresh } = data;
  let ganttData: CP_GANTT.IGanttData[] = refresh ? [] : [...prevList];

  const timeConvert = (start: number, end: number) => {
    let _start = start && moment(start).startOf('day');
    let _end = end && moment(end).endOf('day');
    if (!start && end) {
      // no start time
      _start = moment(_end).startOf('day');
    } else if (!end && start) {
      _end = moment(_start).endOf('day');
    }
    return {
      start: _start && new Date(_start.valueOf()),
      end: _end && new Date(_end.valueOf()),
    };
  };

  const prevDataGroup = { ...groupBy(ganttData, 'pId'), ...expandList };

  const convert = (dataTemp: CP_GANTT.IData[], level = 0, pId?: string) => {
    dataTemp.forEach((item) => {
      const { key, title, start, end, isLeaf = true, hideChildren, ...rest } = item;
      const validTime = timeConvert(start, end);

      const curData = {
        type: !isLeaf ? 'project' : ('task' as CP_GANTT.TaskType),
        id: key,
        name: title,
        start: validTime.start,
        end: validTime.end,
        progress: 0,
        isLeaf,
        hideChildren: hideChildren === undefined ? (!isLeaf ? !prevDataGroup[key]?.length : undefined) : hideChildren,
        level,
        pId: pId || 0,
        ...(pId ? { project: pId } : {}),
        ...rest,
      };
      ganttData.push(curData);
      if (prevDataGroup[curData.id]) {
        convert(prevDataGroup[curData.id], level + 1, curData.id);
      }
    });
  };

  if (expandList) {
    ganttData = [];
    convert(prevDataGroup['0']); // root: 0
  }
  if (updateList?.length) {
    updateList.forEach((item) => {
      const curDataIndex = findIndex(ganttData, (gItem) => gItem.id === item.key);
      if (curDataIndex !== -1) {
        const { key, title, start, end, isLeaf = true, hideChildren, ...rest } = item;

        set(ganttData, `[${curDataIndex}]`, {
          ...ganttData[curDataIndex],
          ...rest,
          isLeaf,
          hideChildren: hideChildren === undefined ? (!isLeaf ? !prevDataGroup[key]?.length : undefined) : hideChildren,
          id: key,
          name: title,
          start: start && new Date(start),
          end: end && new Date(end),
          type: !isLeaf ? 'project' : 'task',
        });
      }
    });
  }
  return ganttData;
};
