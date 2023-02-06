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

import { max } from 'lodash';
import React, { useRef } from 'react';
import { Calendar, CalendarProps } from '../calendar/calendar';
import { Grid, GridProps } from '../grid/grid';
import { TaskGanttContent, TaskGanttContentProps } from './task-gantt-content';
import './gantt.scss';

export interface TaskGanttProps {
  gridProps: GridProps;
  calendarProps: CalendarProps;
  barProps: TaskGanttContentProps;
  ganttHeight: number;
  BarContentRender: React.ReactNode;
  rootWrapper: React.ReactElement;
}
export const TaskGantt: React.FC<TaskGanttProps> = ({ gridProps, calendarProps, barProps, BarContentRender }) => {
  const ganttSVGRef = useRef<SVGSVGElement>(null);
  const newBarProps = { ...barProps, svg: ganttSVGRef };
  const verticalGanttContainerRef = useRef<HTMLDivElement>(null);
  const offsetWidth = verticalGanttContainerRef?.current?.offsetWidth;
  const [mousePos, setMousePos] = React.useState<null | number[]>(null);

  const onMouseMove = (e: React.MouseEvent) => {
    const gridPos = e.currentTarget.getBoundingClientRect();
    const mouseY = max([e.clientY - gridPos.y, 0]);
    const mouseX = max([e.clientX - gridPos.x]);
    setMousePos([Math.floor(mouseX / gridProps.columnWidth), Math.floor(mouseY / gridProps.rowHeight)]);
  };

  const mouseUnFocus = () => {
    setMousePos(null);
  };

  return (
    <div className={'erda-gantt-vertical-container'} dir="ltr" ref={verticalGanttContainerRef}>
      <Calendar
        {...calendarProps}
        width={max([calendarProps.width, offsetWidth])}
        displayWidth={offsetWidth}
        mousePos={mousePos}
      />
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width={max([gridProps.svgWidth, offsetWidth])}
        height={barProps.rowHeight * barProps.tasks.length}
        fontFamily={barProps.fontFamily}
        style={{ overflow: 'visible' }}
        ref={ganttSVGRef}
      >
        <g onMouseMove={onMouseMove} onMouseLeave={mouseUnFocus}>
          <Grid
            {...gridProps}
            svgWidth={max([gridProps.svgWidth, offsetWidth])}
            displayWidth={offsetWidth}
            onMouseMove={onMouseMove}
            mouseUnFocus={mouseUnFocus}
            mousePos={mousePos}
          />
        </g>
        <TaskGanttContent {...newBarProps} displayWidth={offsetWidth} BarContentRender={BarContentRender} />
      </svg>
    </div>
  );
};
