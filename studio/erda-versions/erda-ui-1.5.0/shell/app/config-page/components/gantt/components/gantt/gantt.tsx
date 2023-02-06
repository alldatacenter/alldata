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

import React, { useState, SyntheticEvent, useRef, useEffect, useLayoutEffect } from 'react';
import i18n from 'i18n';
import { ViewMode, GanttProps, Task } from '../../types/public-types';
import { GridProps } from '../grid/grid';
import { calcDateDurationByHNumber, ganttDateRange, seedDates } from '../../helpers/date-helper';
import { CalendarProps } from '../calendar/calendar';
import { TaskGanttContentProps } from './task-gantt-content';
import { TaskListHeaderDefault } from '../task-list/task-list-header';
import { TaskListTableDefault } from '../task-list/task-list-table';
// import { StandardTooltipContent, Tooltip } from '../other/tooltip';
import { VerticalScroll, HorizontalScroll } from '../other/scroll';
import { TaskListProps, TaskList } from '../task-list/task-list';
import { TaskGantt } from './task-gantt';
import { BarTask } from '../../types/bar-task';
import { convertToBarTasks } from '../../helpers/bar-helper';
import { GanttEvent } from '../../types/gantt-task-actions';
import { DateSetup } from '../../types/date-setup';
import { removeHiddenTasks } from '../../helpers/other-helper';
import { useFullScreen } from 'app/common/use-hooks';
import { ErdaIcon } from 'common';
import './gantt.scss';

const extra_H_Number = 2;
export const Gantt: React.FunctionComponent<GanttProps> = ({
  tasks,
  headerHeight = 76,
  columnWidth = 40,
  listCellWidth = '100px',
  rowHeight = 50,
  ganttHeight = 0,
  viewMode = ViewMode.Day,
  locale = 'en-GB',
  barFill = 60,
  barCornerRadius = 3,
  barProgressColor = '#a3a3ff',
  barProgressSelectedColor = '#8282f5',
  barBackgroundColor = '#424CA6',
  barBackgroundSelectedColor = '#1E2059',
  projectProgressColor = '#7db59a',
  projectProgressSelectedColor = '#59a985',
  projectBackgroundColor = '#d1dcff',
  projectBackgroundSelectedColor = '#d1dcff',
  milestoneBackgroundColor = '#f1c453',
  milestoneBackgroundSelectedColor = '#f29e4c',
  rtl = false,
  handleWidth = 10,
  timeStep = 1000 * 60 * 60 * 24,
  arrowColor = 'grey',
  fontFamily = 'Roboto, Arial, Oxygen, Ubuntu, Cantarell, Fira Sans, Droid Sans, Helvetica Neue',
  fontSize = '14px',
  arrowIndent = 20,
  todayColor = 'rgba(252, 248, 227, 0.5)',
  // TooltipContent = StandardTooltipContent,
  TaskListHeader = TaskListHeaderDefault,
  TaskListTable = TaskListTableDefault,
  BarContentRender = null,
  rootWrapper,
  onDateChange,
  onProgressChange,
  onDoubleClick,
  onDelete,
  onSelect,
  onExpanderClick,
  onScreenChange,
}) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const taskListRef = useRef<HTMLDivElement>(null);

  const [dateSetup, setDateSetup] = useState<DateSetup>(() => {
    const [startDate, endDate] = ganttDateRange(tasks, viewMode);
    return { viewMode, dates: seedDates(startDate, endDate, viewMode) };
  });

  const horizontalRef = React.useRef<HTMLDivElement>(null);

  const [taskHeight, setTaskHeight] = useState((rowHeight * barFill) / 100);
  const [taskListWidth, setTaskListWidth] = useState(0);
  // const [svgContainerWidth, setSvgContainerWidth] = useState(0);
  // const [svgContainerHeight, setSvgContainerHeight] = useState(ganttHeight);
  const [rangeAddTime, setRangeAddTime] = useState<null | { x1: number; x2: number }>(null);

  const [barTasks, setBarTasks] = useState<BarTask[]>([]);
  const [ganttEvent, setGanttEvent] = useState<GanttEvent>({
    action: '',
  });

  const [selectedTask, setSelectedTask] = useState<BarTask>();
  const [failedTask, setFailedTask] = useState<BarTask | null>(null);

  const svgWidth = dateSetup.dates.length * columnWidth;
  const ganttFullHeight = barTasks.length * rowHeight;

  const [scrollY, setScrollY] = useState(0);
  const [scrollX, setScrollX] = useState(-1);

  const h_start = Math.abs(Math.ceil(scrollX / columnWidth));
  const h_number = Math.floor((horizontalRef.current?.clientWidth || 0) / columnWidth) + extra_H_Number;
  const horizontalRange = [h_start, h_start + h_number];
  // console.log('横', horizontalRef.current?.clientWidth, scrollX, horizontalRange, h_number);

  const v_start = Math.abs(Math.ceil(scrollY / rowHeight));
  const v_number = Math.floor((ganttHeight || 0) / rowHeight) + 1;
  const verticalRange = [v_start, v_start + v_number];
  // console.log('纵', ganttHeight, scrollY, verticalRange, v_number)
  const ignoreScrollEventRef = useRef(false);
  const positionToTodayAtInit = useRef(false);

  // task change events
  useEffect(() => {
    let filteredTasks: Task[];
    if (onExpanderClick) {
      filteredTasks = removeHiddenTasks(tasks);
    } else {
      filteredTasks = tasks;
    }
    const [startDate, endDate] = ganttDateRange(filteredTasks, viewMode);
    const [newStartDate, newEndDate] = calcDateDurationByHNumber(h_number, startDate, endDate);
    const newDates = seedDates(newStartDate, newEndDate, viewMode);
    // if (rtl) {
    //   newDates = newDates.reverse();
    //   if (scrollX === -1) {
    //     setScrollX(newDates.length * columnWidth);
    //   }
    // }
    setDateSetup({ dates: newDates, viewMode });

    setBarTasks(
      convertToBarTasks(
        filteredTasks,
        newDates,
        columnWidth,
        rowHeight,
        taskHeight,
        barCornerRadius,
        handleWidth,
        rtl,
        barProgressColor,
        barProgressSelectedColor,
        barBackgroundColor,
        barBackgroundSelectedColor,
        projectProgressColor,
        projectProgressSelectedColor,
        projectBackgroundColor,
        projectBackgroundSelectedColor,
        milestoneBackgroundColor,
        milestoneBackgroundSelectedColor,
        0,
      ),
    );
  }, [
    tasks,
    viewMode,
    rowHeight,
    barCornerRadius,
    columnWidth,
    taskHeight,
    handleWidth,
    barProgressColor,
    barProgressSelectedColor,
    barBackgroundColor,
    barBackgroundSelectedColor,
    projectProgressColor,
    projectProgressSelectedColor,
    projectBackgroundColor,
    projectBackgroundSelectedColor,
    milestoneBackgroundColor,
    milestoneBackgroundSelectedColor,
    rtl,
    onExpanderClick,
    h_number,
  ]);

  useEffect(() => {
    const { changedTask, action } = ganttEvent;
    if (changedTask) {
      if (action === 'delete') {
        setGanttEvent({ action: '' });
        setBarTasks(barTasks.filter((t) => t.id !== changedTask.id));
      } else if (action === 'move' || action === 'end' || action === 'start' || action === 'progress') {
        const prevStateTask = barTasks.find((t) => t.id === changedTask.id);
        if (
          prevStateTask &&
          (prevStateTask.start.getTime() !== changedTask.start.getTime() ||
            prevStateTask.end.getTime() !== changedTask.end.getTime() ||
            prevStateTask.progress !== changedTask.progress)
        ) {
          // actions for change
          const newTaskList = barTasks.map((t) => (t.id === changedTask.id ? changedTask : t));
          setBarTasks(newTaskList);
        }
      }
    }
  }, [ganttEvent, barTasks]);

  useEffect(() => {
    if (failedTask) {
      setBarTasks(barTasks.map((t) => (t.id !== failedTask.id ? t : failedTask)));
      setFailedTask(null);
    }
  }, [failedTask, barTasks]);

  useEffect(() => {
    const newTaskHeight = (rowHeight * barFill) / 100;
    if (newTaskHeight !== taskHeight) {
      setTaskHeight(newTaskHeight);
    }
  }, [rowHeight, barFill, taskHeight]);

  useEffect(() => {
    if (!listCellWidth) {
      setTaskListWidth(0);
    }
    if (taskListRef.current) {
      setTaskListWidth(taskListRef.current.offsetWidth);
    }
  }, [taskListRef, listCellWidth]);

  const scrollToToday = React.useCallback(() => {
    const today = new Date();
    const [startDate] = calcDateDurationByHNumber(h_number, dateSetup.dates[0], dateSetup.dates[1]);
    const duration = Math.floor((today.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24) - h_number / 2) + 2;
    setScrollX(duration * columnWidth);
  }, [columnWidth, dateSetup.dates, h_number]);

  useLayoutEffect(() => {
    if (h_number !== extra_H_Number && !positionToTodayAtInit.current) {
      scrollToToday();
      positionToTodayAtInit.current = true;
    }
  }, [h_number, scrollToToday]);

  // useEffect(() => {
  //   if (wrapperRef.current) {
  //     setSvgContainerWidth(wrapperRef.current.offsetWidth - taskListWidth);
  //   }
  // }, [wrapperRef, taskListWidth]);

  // useEffect(() => {
  //   if (ganttHeight) {
  //     setSvgContainerHeight(ganttHeight + headerHeight);
  //   } else {
  //     setSvgContainerHeight(tasks.length * rowHeight + headerHeight);
  //   }
  // }, [ganttHeight, headerHeight, rowHeight, tasks]);

  const wheelValueRef = useRef<number[]>();
  // scroll events
  useEffect(() => {
    const handleWheel = (event: WheelEvent) => {
      event.preventDefault();
      ignoreScrollEventRef.current = true;
      // do not use async event data, so use ref to get the last data
      wheelValueRef.current = [event.deltaX, event.deltaY];
      window.requestAnimationFrame(() => {
        // console.log('wheel', wheelValueRef.current)
        if (Math.abs(wheelValueRef.current[0]) > 2) {
          // const scrollMove = wheelValueRef.current[0] ? wheelValueRef.current[0] : event.deltaY;
          let newScrollX = scrollX + wheelValueRef.current[0];
          const scrollDis = svgWidth - horizontalRef.current.clientWidth;
          if (newScrollX < 0) {
            newScrollX = 0;
          } else if (newScrollX > scrollDis) {
            newScrollX = scrollDis;
          }
          if (newScrollX !== scrollX) {
            setScrollX(newScrollX);
            // console.log('newScrollX', newScrollX);
          }
        }
        if (Math.abs(wheelValueRef.current[1]) > 2) {
          let newScrollY = scrollY + wheelValueRef.current[1];
          if (newScrollY < 0) {
            newScrollY = 0;
          } else if (newScrollY > ganttFullHeight - ganttHeight) {
            newScrollY = ganttFullHeight - ganttHeight;
          }
          if (newScrollY !== scrollY) {
            setScrollY(newScrollY);
          }
        }
      });
    };

    // subscribe if scroll is necessary
    if (wrapperRef.current) {
      wrapperRef.current.addEventListener('wheel', handleWheel, {
        passive: false,
      });
    }
    return () => {
      if (wrapperRef.current) {
        wrapperRef.current.removeEventListener('wheel', handleWheel);
      }
    };
  }, [ganttHeight, svgWidth, rtl, ganttFullHeight, scrollX, scrollY]);

  const handleScrollY = (event: SyntheticEvent<HTMLDivElement>) => {
    if (scrollY !== event.currentTarget.scrollTop && !ignoreScrollEventRef.current) {
      setScrollY(event.currentTarget.scrollTop);
    }
    ignoreScrollEventRef.current = false;
  };

  const handleScrollX = (event: SyntheticEvent<HTMLDivElement>) => {
    if (scrollX !== event.currentTarget.scrollLeft && !ignoreScrollEventRef.current) {
      setScrollX(event.currentTarget.scrollLeft);
    }
    ignoreScrollEventRef.current = false;
  };

  /**
   * Handles arrow keys events and transform it to new scroll
   */
  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    event.preventDefault();
    let newScrollY = scrollY;
    let newScrollX = scrollX;
    let isX = true;
    switch (event.key) {
      case 'Down': // IE/Edge specific value
      case 'ArrowDown':
        newScrollY += rowHeight;
        isX = false;
        break;
      case 'Up': // IE/Edge specific value
      case 'ArrowUp':
        newScrollY -= rowHeight;
        isX = false;
        break;
      case 'Left':
      case 'ArrowLeft':
        newScrollX -= columnWidth;
        break;
      case 'Right': // IE/Edge specific value
      case 'ArrowRight':
        newScrollX += columnWidth;
        break;
      default:
        break;
    }
    if (isX) {
      if (newScrollX < 0) {
        newScrollX = 0;
      } else if (newScrollX > svgWidth) {
        newScrollX = svgWidth;
      }
      setScrollX(newScrollX);
    } else {
      if (newScrollY < 0) {
        newScrollY = 0;
      } else if (newScrollY > ganttFullHeight - ganttHeight) {
        newScrollY = ganttFullHeight - ganttHeight;
      }
      setScrollY(newScrollY);
    }
    ignoreScrollEventRef.current = true;
  };

  /**
   * Task select event
   */
  const handleSelectedTask = (taskId: string) => {
    const newSelectedTask = barTasks.find((t) => t.id === taskId);
    const oldSelectedTask = barTasks.find((t) => !!selectedTask && t.id === selectedTask.id);
    if (onSelect) {
      if (oldSelectedTask) {
        onSelect(oldSelectedTask, false);
      }
      if (newSelectedTask) {
        onSelect(newSelectedTask, true);
      }
    }
    if (newSelectedTask?.x1) {
      if (scrollX + 40 > newSelectedTask.x2) {
        setScrollX(newSelectedTask.x1 - 40);
      } else if (scrollX + horizontalRef.current?.offsetWidth < newSelectedTask.x1) {
        setScrollX(newSelectedTask.x2 + 40 - horizontalRef.current.offsetWidth);
      }
    }
    setSelectedTask(newSelectedTask);
  };
  const handleExpanderClick = (task: Task) => {
    if (onExpanderClick && task.hideChildren !== undefined) {
      onExpanderClick({ ...task, hideChildren: !task.hideChildren });
    }
  };

  const [isFullScreen, { toggleFullscreen }] = useFullScreen(document.body, {
    onEnter: () => {
      onScreenChange(true);
    },
    onExit: () => {
      onScreenChange(false);
    },
  });

  const showLength = verticalRange[1] - verticalRange[0];
  let showRange = verticalRange;
  if (showLength > barTasks.length) {
    // show all tasks
    showRange = [0, barTasks.length];
  } else if (verticalRange[1] > barTasks.length) {
    // there is also space left, move forward
    let offset = verticalRange[1] - barTasks.length;
    // if last task show more than half height, move forward to show full
    if (ganttFullHeight - ganttHeight - scrollY < rowHeight / 2) {
      offset -= 1;
    }
    showRange = [Math.max(0, verticalRange[0] - offset), verticalRange[1] - offset];
  }
  const visibleTaskList = barTasks.slice(...showRange);
  const newTasks = convertToBarTasks(
    visibleTaskList,
    dateSetup.dates,
    columnWidth,
    rowHeight,
    taskHeight,
    barCornerRadius,
    handleWidth,
    rtl,
    barProgressColor,
    barProgressSelectedColor,
    barBackgroundColor,
    barBackgroundSelectedColor,
    projectProgressColor,
    projectProgressSelectedColor,
    projectBackgroundColor,
    projectBackgroundSelectedColor,
    milestoneBackgroundColor,
    milestoneBackgroundSelectedColor,
    -h_start,
  );
  // position data will update after convert
  const newSelectedTask = newTasks.find((item) => item.id === selectedTask?.id);

  const reGanttEvent = {
    ...ganttEvent,
    changedTask: ganttEvent?.changedTask ? newTasks.find((item) => item.id === ganttEvent.changedTask.id) : undefined,
  };
  const gridProps: GridProps = {
    columnWidth,
    svgWidth,
    tasks,
    barTasks: newTasks,
    rowHeight,
    dates: dateSetup.dates,
    todayColor,
    ganttHeight,
    rtl,
    selectedTask: newSelectedTask,
    setRangeAddTime,
    setSelectedTask: handleSelectedTask,
    onDateChange,
    ganttEvent: reGanttEvent,
    verticalRange,
    horizontalRange,
  };

  const calendarProps: CalendarProps = {
    dateSetup,
    locale,
    viewMode,
    width: svgWidth,
    svgWidth,
    height: headerHeight,
    columnWidth,
    fontFamily,
    fontSize,
    rtl,
    horizontalRange,
    highlightRange: rangeAddTime || reGanttEvent.changedTask || newSelectedTask,
    scrollX,
    setScrollX,
  };
  const barProps: TaskGanttContentProps = {
    tasks: newTasks,
    dates: dateSetup.dates,
    ganttEvent: reGanttEvent,
    selectedTask: newSelectedTask,
    rowHeight,
    taskHeight,
    columnWidth,
    arrowColor,
    timeStep,
    fontFamily,
    fontSize,
    arrowIndent,
    svgWidth,
    horizontalRange,
    rtl,
    setGanttEvent,
    setFailedTask,
    setSelectedTask: handleSelectedTask,
    onDateChange,
    onProgressChange,
    onDoubleClick,
    onDelete,
  };

  const tableProps: TaskListProps = {
    rowHeight,
    rowWidth: listCellWidth,
    fontFamily,
    fontSize,
    tasks: newTasks,
    locale,
    scrollX,
    headerHeight,
    ganttHeight,
    selectedTask: newSelectedTask,
    taskListRef,
    setSelectedTask: handleSelectedTask,
    onExpanderClick: handleExpanderClick,
    TaskListHeader,
    TaskListTable,
  };

  const ganttProps = {
    BarContentRender,
  };

  return (
    <>
      <div className={'erda-gantt-wrapper'} onKeyDown={handleKeyDown} tabIndex={0} ref={wrapperRef}>
        {listCellWidth && <TaskList {...tableProps} />}
        <TaskGantt
          {...ganttProps}
          gridProps={gridProps}
          calendarProps={calendarProps}
          barProps={barProps}
          ganttHeight={ganttHeight}
          rootWrapper={rootWrapper}
        />
        {/* <div className={'erda-gantt-vertical-container'} ref={verticalGanttContainerRef} dir="ltr">
        </div> */}
        {/* {ganttEvent.changedTask && (
          <Tooltip
            arrowIndent={arrowIndent}
            rowHeight={rowHeight}
            svgContainerHeight={svgContainerHeight}
            svgContainerWidth={svgContainerWidth}
            fontFamily={fontFamily}
            fontSize={fontSize}
            scrollX={scrollX}
            scrollY={scrollY}
            task={ganttEvent.changedTask}
            headerHeight={headerHeight}
            taskListWidth={taskListWidth}
            TooltipContent={TooltipContent}
            rtl={rtl}
            svgWidth={svgWidth}
          />
        )} */}
        <VerticalScroll
          scrollHeight={ganttFullHeight}
          height={ganttHeight}
          topOffset={headerHeight}
          scroll={scrollY}
          onScroll={handleScrollY}
          rtl={rtl}
        />
        <div className="absolute bg-white bottom-4 right-6 flex shadow-card-lg">
          <div
            onClick={(e) => {
              e.stopPropagation();
              scrollToToday();
            }}
            className="gantt-operate-btn text-sub hover:text-default cursor-pointer"
          >
            {i18n.t('Today')}
          </div>
          <div
            onClick={(e) => {
              e.stopPropagation();
              toggleFullscreen();
            }}
            className="gantt-operate-btn flex justify-center items-center text-sub hover:text-default cursor-pointer"
          >
            <ErdaIcon type={isFullScreen ? 'collapse-text-input' : 'expand-text-input'} size={16} />
          </div>
        </div>
      </div>
      <HorizontalScroll
        width={svgWidth}
        offset={taskListWidth}
        scroll={scrollX}
        rtl={rtl}
        ref={horizontalRef}
        onScroll={handleScrollX}
      />
    </>
  );
};
