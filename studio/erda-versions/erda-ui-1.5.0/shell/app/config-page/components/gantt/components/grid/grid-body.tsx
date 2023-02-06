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

import React, { ReactChild } from 'react';
import { Task } from '../../types/public-types';
import { max, min, findIndex, debounce } from 'lodash';
import moment from 'moment';
import './grid.scss';
import { ErdaIcon } from 'common';

export interface GridBodyProps {
  tasks: Task[];
  dates: Date[];
  svgWidth: number;
  rowHeight: number;
  columnWidth: number;
  todayColor: string;
  rtl: boolean;
  ganttEvent: Obj;
}

const getDateFormX = (x1 = -1, x2 = -1, dateDelta: number, columnWidth: number, firstDate: number) => {
  if (x1 === -1 || x2 === -1) return [];
  const unit = dateDelta / columnWidth;
  const start = x1 * unit + firstDate;
  const end = x2 * unit + firstDate - 1;
  return [start, end].sort();
};

export const GridBody: React.FC<GridBodyProps> = ({
  tasks: originTasks,
  dates,
  barTasks: tasks,
  rowHeight,
  svgWidth,
  columnWidth,
  todayColor,
  selectedTask,
  ganttHeight,
  setSelectedTask,
  rtl,
  onDateChange,
  ganttEvent,
  setRangeAddTime,
  horizontalRange,
  displayWidth,
  onMouseMove: propsOnMouseMove,
  mouseUnFocus: propsMouseUnFocus,
  mousePos,
}) => {
  let y = 0;
  const gridRows: ReactChild[] = [];
  const today = new Date();
  const dateDelta =
    dates[1].getTime() -
    dates[0].getTime() -
    dates[1].getTimezoneOffset() * 60 * 1000 +
    dates[0].getTimezoneOffset() * 60 * 1000;

  const [startPos, setStartPos] = React.useState<null | number[]>(null);
  const [endPos, setEndPos] = React.useState<null | number[]>(null);
  const [chosenTask, setChosenTask] = React.useState<Obj | null>(null);

  React.useEffect(() => {
    if (startPos && endPos) {
      setRangeAddTime({ x1: startPos[0], x2: endPos[0] });
    } else {
      setRangeAddTime(null);
    }
  }, [startPos, endPos]);

  const onMouseDown = (e: React.MouseEvent) => {
    const gridPos = e.currentTarget.getBoundingClientRect();
    const clickY = e.clientY - gridPos.y;
    const clickPos = Math.floor(clickY / rowHeight);
    const curTask = tasks[clickPos];

    if (!curTask.start || !curTask.end) {
      setSelectedTask(curTask.id);
      setChosenTask(curTask);

      setStartPos([
        Math.floor((e.clientX - gridPos.x) / columnWidth) * columnWidth,
        clickPos * rowHeight + 8,
        e.clientX - gridPos.x,
      ]);
    }
  };
  const mouseUnFocus = () => {
    propsMouseUnFocus();
    setStartPos(null);
    setEndPos(null);
    setChosenTask(null);
  };
  const curDates = dates.slice(...horizontalRange);
  const todayIndex = findIndex(curDates, (item) => moment(item).isSame(today, 'day'));
  const addTime = getDateFormX(startPos?.[0], endPos?.[0], dateDelta, columnWidth, curDates[0]?.getTime());

  const onMouseUp = () => {
    if (addTime.length && addTime[1] - addTime[0] >= dateDelta * 0.6 && chosenTask) {
      onDateChange({ ...chosenTask, start: new Date(addTime[0]), end: new Date(addTime[1]) });
    }
    mouseUnFocus();
  };
  const onMouseMove = (e: React.MouseEvent) => {
    const gridPos = e.currentTarget.getBoundingClientRect();
    propsOnMouseMove(e);
    const curEndPod = e.clientX - gridPos.x;

    if (startPos) {
      setEndPos(
        curEndPod - startPos[2] > 10
          ? [
              (Math.floor((e.clientX - gridPos.x + 1) / columnWidth) + 1) * columnWidth,
              startPos[1] + rowHeight - 16,
              curEndPod,
            ]
          : null,
      );
    }
  };

  tasks?.forEach((task: Task, idx: number) => {
    const validTask = task.start && task.end;
    let PointIcon = null;
    if (validTask) {
      const displayPos = Math.floor(displayWidth / columnWidth);
      if (curDates?.[0] && task.end < curDates[0]) {
        PointIcon = (
          <div className="text-default-2 hover:text-default-4 erda-gantt-grid-arrow-box flex items-center">
            <ErdaIcon className="cursor-pointer " type="zuo" size={20} onClick={() => setSelectedTask(task.id)} />
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
    }
    gridRows.push(
      <foreignObject key={`Row${task.id}`} x="0" y={y} width={svgWidth} height={rowHeight}>
        <div
          className={`flex erda-gantt-grid-row h-full ${
            selectedTask?.id === task.id ? 'erda-gantt-grid-row-selected' : ''
          } ${!validTask ? 'on-add' : ''} ${mousePos?.[1] === idx ? 'on-hover' : ''}`}
        />
      </foreignObject>,
    );
    y += rowHeight;
  });

  const { changedTask } = ganttEvent || {};
  const realHeight = tasks.length * rowHeight;

  const getAddRangePos = () => {
    if (startPos && endPos) {
      return {
        transform: `translate(${min([startPos[0], endPos[0]])},${min([startPos[1], endPos[1]])})`,
        width: Math.abs(startPos[0] - endPos[0]),
        height: Math.abs(startPos[1] - endPos[1]),
      };
    }
    return null;
  };

  const getRangePos = () => {
    if (changedTask) {
      return {
        transform: `translate(${changedTask.x1},0)`,
        width: changedTask.x2 - changedTask.x1,
        height: max([ganttHeight, realHeight]),
      };
    } else if (startPos && endPos) {
      return {
        transform: `translate(${min([startPos[0], endPos[0]])},0)`,
        width: Math.abs(endPos[0] - startPos[0]),
        height: max([ganttHeight, realHeight]),
      };
    }
    return null;
  };

  const getMouseBlockPos = () => {
    if (mousePos) {
      const curTask = tasks[mousePos[1]];
      if (curTask && !curTask.start && !curTask.end) {
        return {
          width: columnWidth,
          height: rowHeight - 16,
          transform: `translate(${mousePos[0] * columnWidth},${mousePos[1] * rowHeight + 8})`,
        };
      }
    }
    return null;
  };

  const getMouseHoverPos = () => {
    if (mousePos) {
      return {
        width: 8,
        height: max([ganttHeight, realHeight]),
        transform: `translate(${mousePos[0] * columnWidth + columnWidth / 2 - 4},0)`,
      };
    }
    return null;
  };

  const rangePos = getRangePos();
  const mouseBlockPos = getMouseBlockPos();
  const addRangePos = getAddRangePos();
  const mouseHoverPos = getMouseHoverPos();
  const todayStartPos = todayIndex * columnWidth + columnWidth / 2 - 1;
  return (
    <g
      className="gridBody"
      onMouseDown={onMouseDown}
      onMouseUp={() => {
        onMouseUp();
      }}
      onMouseMove={onMouseMove}
      onMouseLeave={mouseUnFocus}
    >
      {rangePos ? (
        <rect {...rangePos} className="erda-gantt-grid-changed-range" />
      ) : mouseHoverPos ? (
        <foreignObject {...mouseHoverPos}>
          <div className="h-full w-full erda-gantt-grid-hover-box">
            <div className="erda-gantt-grid-hover-arrow" />
            <div className="erda-gantt-grid-hover-range h-full w-full" />
          </div>
        </foreignObject>
      ) : null}

      <g className="rows">{gridRows}</g>
      {addRangePos ? (
        <g>
          <foreignObject {...addRangePos}>
            <div className="erda-gantt-grid-add-rect text-sm text-desc  bg-white bg-opacity-100 w-full h-full">{`${moment(
              addTime[0],
            ).format('MM-DD')}~${moment(addTime[1]).format('MM-DD')}`}</div>
          </foreignObject>
        </g>
      ) : mouseBlockPos ? (
        <g>
          <foreignObject {...mouseBlockPos}>
            <div className="erda-gantt-grid-add-rect bg-white bg-opacity-100 w-full h-full" />
          </foreignObject>
        </g>
      ) : null}
      {todayIndex > -1 ? (
        <polyline
          points={`${todayStartPos + columnWidth / 2},4 ${todayStartPos + columnWidth / 2},${max([
            ganttHeight,
            realHeight,
          ])}`}
          className="erda-gantt-grid-today"
        />
      ) : null}
    </g>
  );
};
