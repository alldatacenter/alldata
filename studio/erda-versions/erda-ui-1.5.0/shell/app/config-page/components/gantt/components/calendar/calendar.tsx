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
import { ViewMode } from '../../types/public-types';
import { TopPartOfCalendar } from './top-part-of-calendar';
import {
  addToDate,
  getCachedDateTimeFormat,
  getDaysInMonth,
  getLocaleMonth,
  getWeekNumberISO8601,
} from '../../helpers/date-helper';
import { ErdaIcon } from 'common';
import moment from 'moment';
import { min, max, flatten } from 'lodash';
import { DateSetup } from '../../types/date-setup';
import i18n from 'i18n';
import './calendar.scss';

export interface CalendarProps {
  dateSetup: DateSetup;
  locale: string;
  viewMode: ViewMode;
  rtl: boolean;
  width: number;
  height: number;
  columnWidth: number;
  fontFamily: string;
  fontSize: string;
  horizontalRange: number[];
  highlightRange?: {
    x1: number;
    x2: number;
    [x: string]: any;
  };
}

const Days = [i18n.t('Sun'), i18n.t('Mon'), i18n.t('Tue'), i18n.t('Wed'), i18n.t('Thu'), i18n.t('Fri'), i18n.t('Sat')];
const Months = [
  i18n.t('January'),
  i18n.t('February'),
  i18n.t('March'),
  i18n.t('April'),
  i18n.t('May'),
  i18n.t('June'),
  i18n.t('July'),
  i18n.t('August'),
  i18n.t('September'),
  i18n.t('October'),
  i18n.t('November'),
  i18n.t('December'),
];
export const Calendar: React.FC<CalendarProps> = React.memo(
  ({
    dateSetup,
    locale,
    viewMode,
    rtl,
    width,
    height,
    columnWidth,
    horizontalRange,
    fontFamily,
    fontSize,
    highlightRange,
    displayWidth,
    scrollX,
    setScrollX,
    svgWidth,
    mousePos,
  }) => {
    const today = new Date();
    const getCalendarValuesForMonth = () => {
      const topValues: ReactChild[] = [];
      const bottomValues: ReactChild[] = [];
      const topDefaultHeight = height * 0.5;
      for (let i = 0; i < dateSetup.dates.length; i++) {
        const date = dateSetup.dates[i];
        const bottomValue = getLocaleMonth(date, locale);
        bottomValues.push(
          <text
            key={bottomValue + date.getFullYear()}
            y={height * 0.8}
            x={columnWidth * i + columnWidth * 0.5}
            className={'erda-gantt-calendar-bottom-text'}
          >
            {bottomValue}
          </text>,
        );
        if (i === 0 || date.getFullYear() !== dateSetup.dates[i - 1].getFullYear()) {
          const topValue = date.getFullYear().toString();
          let xText: number;
          if (rtl) {
            xText = (6 + i + date.getMonth() + 1) * columnWidth;
          } else {
            xText = (6 + i - date.getMonth()) * columnWidth;
          }
          topValues.push(
            <TopPartOfCalendar
              key={topValue}
              value={topValue}
              x1Line={columnWidth * i}
              y1Line={0}
              y2Line={topDefaultHeight}
              xText={xText}
              yText={topDefaultHeight * 0.9}
            />,
          );
        }
      }
      return [topValues, bottomValues];
    };

    const getCalendarValuesForWeek = () => {
      const topValues: ReactChild[] = [];
      const bottomValues: ReactChild[] = [];
      let weeksCount = 1;
      const topDefaultHeight = height * 0.5;
      const { dates } = dateSetup;
      for (let i = dates.length - 1; i >= 0; i--) {
        const date = dates[i];
        let topValue = '';
        if (i === 0 || date.getMonth() !== dates[i - 1].getMonth()) {
          // top
          topValue = `${getLocaleMonth(date, locale)}, ${date.getFullYear()}`;
        }
        // bottom
        const bottomValue = `W${getWeekNumberISO8601(date)}`;

        bottomValues.push(
          <text
            key={date.getTime()}
            y={height * 0.8}
            x={columnWidth * (i + +rtl)}
            className={'erda-gantt-calendar-bottom-text'}
          >
            {bottomValue}
          </text>,
        );

        if (topValue) {
          // if last day is new month
          if (i !== dates.length - 1) {
            topValues.push(
              <TopPartOfCalendar
                key={topValue}
                value={topValue}
                x1Line={columnWidth * i + weeksCount * columnWidth}
                y1Line={0}
                y2Line={topDefaultHeight}
                xText={columnWidth * i + columnWidth * weeksCount * 0.5}
                yText={topDefaultHeight * 0.9}
              />,
            );
          }
          weeksCount = 0;
        }
        weeksCount++;
      }
      return [topValues, bottomValues];
    };
    const reHighlightRange = {
      ...highlightRange,
      ...(highlightRange?.id && !highlightRange.start && !highlightRange.end ? { x1: -1, x2: -1 } : {}),
    };
    const HoverBar = ({ style }: { style: Obj }) =>
      highlightRange ? (
        <div
          className="absolute rounded bg-hover-gray-bg"
          style={{
            width: Math.abs(reHighlightRange.x2 - reHighlightRange.x1),
            height: 40,
            left: min([reHighlightRange.x1, reHighlightRange.x2]),
            top: 26,
            ...style,
          }}
        />
      ) : null;

    const HoverTime = ({ style }: { style: Obj }) =>
      mousePos ? (
        <div
          className="absolute rounded bg-hover-gray-bg"
          style={{
            width: columnWidth,
            height: 40,
            left: mousePos[0] * columnWidth,
            top: 26,
            ...style,
          }}
        />
      ) : null;

    const onChangeScrollX = (direction: number) => {
      const moveLen = Math.floor(displayWidth / columnWidth) - 4; // less then display count;
      const moveX = moveLen > 0 ? moveLen * columnWidth : columnWidth;
      if (direction === -1) {
        setScrollX((prevX) => (moveX >= prevX ? -1 : prevX - moveX));
      } else if (direction === 1) {
        setScrollX((prevX) => (moveX + prevX + displayWidth >= svgWidth ? svgWidth - displayWidth : prevX + moveX));
      }
    };
    const getCalendarValuesForDay = () => {
      let bottomValues: React.ReactNode = null;
      const dates = dateSetup.dates.slice(...horizontalRange);
      const dateInWeeks = [];
      // append date when screen have more space
      if (!dates.length) return null;
      let appendDateLength = Math.max(0, horizontalRange[1] - horizontalRange[0] - dates.length);
      while (appendDateLength-- > 0) {
        const lastDayInLastWeek = dates[dates.length - 1];
        dates.push(addToDate(lastDayInLastWeek, 1, 'day'));
      }
      const firstDay = dates[0];

      const firstDayInWeek = firstDay.getDay();
      // use Monday as first day of week

      const firstWeek = dates.splice(0, firstDayInWeek === 0 ? 1 : 7 - firstDayInWeek + 1);
      while (firstWeek.length < 7) {
        const firstDayInFirstWeek = firstWeek[0];
        firstWeek.unshift(addToDate(firstDayInFirstWeek, -1, 'day'));
      }
      dateInWeeks.push(firstWeek);
      while (dates.length) {
        dateInWeeks.push(dates.splice(0, 7));
      }
      const lastWeek = dateInWeeks[dateInWeeks.length - 1];
      while (lastWeek.length < 7) {
        const lastDayInLastWeek = lastWeek[lastWeek.length - 1];
        lastWeek.push(addToDate(lastDayInLastWeek, 1, 'day'));
      }

      const offsetX = (firstDayInWeek ? firstDayInWeek - 1 : 6) * columnWidth;
      bottomValues = (
        <div
          className="flex h-full w-full erda-gantt-calendar-header-container"
          style={{ transform: `translateX(${-offsetX}px)` }}
        >
          {<HoverBar style={{ transform: `translateX(${offsetX}px)` }} />}
          {<HoverTime style={{ transform: `translateX(${offsetX}px)` }} />}
          {flatten(dateInWeeks).map((day, idx) => {
            const mark =
              reHighlightRange?.x1 === columnWidth * idx - offsetX ||
              reHighlightRange?.x2 === columnWidth * (idx + 1) - offsetX;
            const cls = `${
              mark
                ? 'calendar-highlight-text'
                : `${[0, 6].includes(day.getDay()) ? 'calendar-disabled-text' : 'calendar-normal-text'}`
            }`;
            const isStartPos = columnWidth * idx - offsetX === 0;
            const isToday = moment(day).isSame(today, 'day');
            return (
              <div
                key={day.getTime()}
                style={{
                  width: columnWidth,
                  height: 40,
                  top: 28,
                  left: columnWidth * idx,
                }}
                className={`absolute flex flex-col items-center text-xs justify-center ${cls} ${
                  isToday ? 'text-red' : ''
                }`}
              >
                <span>{Days[day.getDay()]}</span>
                <span>{day.getDate()}</span>
                {isStartPos || day.getDate() === 1 ? (
                  <div className="absolute text-default-8 font-medium " style={{ top: -16 }}>
                    {Months[day.getMonth()]}
                  </div>
                ) : null}
                {isToday ? (
                  <div
                    style={{ left: (columnWidth + 22) / 2, bottom: -14 }}
                    className="absolute erda-gantt-calendar-today flex justify-center"
                  >
                    <div>{i18n.t('dop:Today')}</div>
                  </div>
                ) : null}
              </div>
            );
          })}
          {scrollX > 0 ? (
            <div
              className="flex items-center erda-gantt-calendar-arrow-left"
              onClick={() => onChangeScrollX(-1)}
              style={{ left: offsetX }}
            >
              <ErdaIcon type="zuofan" className="ml-1" size={10} />
            </div>
          ) : null}
          {displayWidth + scrollX < svgWidth ? (
            <div
              className="flex items-center erda-gantt-calendar-arrow-right"
              onClick={() => onChangeScrollX(1)}
              style={{ left: offsetX + displayWidth - 16 }}
            >
              <ErdaIcon type="youfan" className="ml-1" size={10} />
            </div>
          ) : null}
        </div>
      );
      return bottomValues;
    };

    const getCalendarValuesForOther = () => {
      const topValues: ReactChild[] = [];
      const bottomValues: ReactChild[] = [];
      const ticks = viewMode === ViewMode.HalfDay ? 2 : 4;
      const topDefaultHeight = height * 0.5;
      const { dates } = dateSetup;
      for (let i = 0; i < dates.length; i++) {
        const date = dates[i];
        const bottomValue = getCachedDateTimeFormat(locale, {
          hour: 'numeric',
        }).format(date);

        bottomValues.push(
          <text
            key={date.getTime()}
            y={height * 0.8}
            x={columnWidth * (i + +rtl)}
            className={'erda-gantt-calendar-bottom-text'}
            fontFamily={fontFamily}
          >
            {bottomValue}
          </text>,
        );
        if (i === 0 || date.getDate() !== dates[i - 1].getDate()) {
          const topValue = `${date.getDate()} ${getLocaleMonth(date, locale)}`;
          topValues.push(
            <TopPartOfCalendar
              key={topValue + date.getFullYear()}
              value={topValue}
              x1Line={columnWidth * i + ticks * columnWidth}
              y1Line={0}
              y2Line={topDefaultHeight}
              xText={columnWidth * i + ticks * columnWidth * 0.5}
              yText={topDefaultHeight * 0.9}
            />,
          );
        }
      }

      return [topValues, bottomValues];
    };

    // let topValues: ReactChild[] = [];
    // let bottomValues: ReactChild[] = [];
    // switch (dateSetup.viewMode) {
    //   // case ViewMode.Month:
    //   //   [topValues, bottomValues] = getCalendarValuesForMonth();
    //   //   break;
    //   // case ViewMode.Week:
    //   //   [topValues, bottomValues] = getCalendarValuesForWeek();
    //   //   break;
    //   case ViewMode.Day:
    //     [topValues, bottomValues] = getCalendarValuesForDay();
    //     break;
    //   default:
    //     [topValues, bottomValues] = getCalendarValuesForOther();
    //     break;
    // }
    const finalWidth = max([columnWidth * dateSetup.dates.length, width]);
    return (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        className="overflow-visible"
        width={width}
        height={height}
        fontFamily={fontFamily}
      >
        <g className="erda-gantt-calendar" fontSize={fontSize}>
          <rect x={0} y={0} width={finalWidth} height={height} className={'erda-gantt-calendar-header'} />
          <foreignObject
            x={0}
            y={0}
            width={finalWidth}
            height={height}
            className={'erda-gantt-calendar-header overflow-visible'}
          >
            {getCalendarValuesForDay()}
          </foreignObject>
          {/* {topValues} */}
        </g>
      </svg>
    );
  },
);
