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

import React, { useEffect, useMemo } from 'react';
import moment, { Moment } from 'moment';
import { isEmpty, map, get, set, values } from 'lodash';
import classnames from 'classnames';
import { useMount, useUnmount } from 'react-use';
import { Spin, Pagination, DatePicker } from 'antd';
import { Holder, Icon as CustomIcon, BoardGrid } from 'common';
import { useUpdate } from 'common/use-hooks';
import { getTimeRanges } from 'common/utils';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import alarmReportStore from '../../../stores/alarm-report';

import './index.scss';

const ReportRecords = () => {
  const [reportTaskRecords, reportTaskRecord, reportTaskRecordPaging] = alarmReportStore.useStore((s) => [
    s.reportTaskRecords,
    s.reportTaskRecord,
    s.reportTaskRecordPaging,
  ]);
  const { getReportTaskRecords, getReportTaskRecord, getReportTask } = alarmReportStore.effects;
  const { clearReportTaskRecords, clearReportTaskRecord } = alarmReportStore.reducers;
  const [getReportTaskRecordsLoading, getReportTaskRecordLoading] = useLoading(alarmReportStore, [
    'getReportTaskRecords',
    'getReportTaskRecord',
  ]);
  const { pageNo, pageSize, total } = reportTaskRecordPaging;
  const { query } = routeInfoStore.getState((s) => s);
  const [{ activedRecord }, updater] = useUpdate({
    activedRecord: query.recordId,
  });

  const layout = useMemo(
    () =>
      map(get(reportTaskRecord, 'dashboardBlock.viewConfig'), (item: any) => {
        const _item = { ...item };
        const chartType = get(_item, 'view.chartType');
        const staticData = get(_item, 'view.staticData');
        if (isEmpty(staticData)) {
          set(_item, 'view.staticData', {
            time: [],
            metricData: [],
          });
          return _item;
        }
        if (['chart:line', 'chart:bar', 'chart:area'].includes(chartType)) {
          const { time, results } = staticData;
          if (results[0].data.length > 1) {
            set(_item, 'view.staticData', {
              time,
              metricData: map(results[0].data, (itemData) => values(itemData)[0]),
            });
          } else {
            set(_item, 'view.staticData', {
              time,
              metricData: results[0].data[0],
            });
          }
        }
        if (chartType === 'chart:pie') {
          set(_item, 'view.staticData', {
            metricData: [
              {
                name: staticData.title || '',
                data: map(staticData.metricData, ({ title, value }) => ({ name: title, value })),
              },
            ],
          });
          set(_item, 'view.config.option.series', [
            {
              radius: ['30%', '50%'],
            },
          ]);
        }
        return _item;
      }),
    [reportTaskRecord],
  );

  useMount(() => {
    getReportTask();
    getReportTaskRecords({ pageNo, pageSize });
  });

  useUnmount(() => {
    clearReportTaskRecords();
    clearReportTaskRecord();
  });

  useEffect(() => {
    if (reportTaskRecords[0] && !activedRecord) {
      updater.activedRecord(reportTaskRecords[0].id);
    }
  }, [activedRecord, reportTaskRecords, updater]);

  useEffect(() => {
    if (activedRecord) {
      getReportTaskRecord(activedRecord);
    } else {
      clearReportTaskRecord();
    }
  }, [activedRecord, clearReportTaskRecord, getReportTaskRecord]);

  const handleChange = (no: number, dates?: Array<undefined | Moment>) => {
    let payload = { pageNo: no, pageSize } as any;
    if (dates) {
      payload = {
        ...payload,
        start: dates[0] && dates[0].valueOf(),
        end: dates[1] && dates[1].valueOf(),
      };
    }
    clearReportTaskRecords();
    updater.activedRecord(undefined);
    getReportTaskRecords(payload);
  };

  const handleClick = (id: number) => {
    updater.activedRecord(id);
  };

  return (
    <div className="task-report-records flex items-start justify-between">
      <div className="search-records pr-4 flex flex-col h-full">
        <div className="mb-2">
          <DatePicker.RangePicker
            borderTime
            className="w-full"
            onChange={(dates) => handleChange(pageNo, dates)}
            ranges={getTimeRanges()}
          />
        </div>
        <div className="flex-1 h-full overflow-auto">
          <Spin spinning={getReportTaskRecordsLoading}>
            <Holder when={isEmpty(reportTaskRecords)}>
              <ul>
                {map(reportTaskRecords, ({ id, start, end }) => (
                  <li
                    className={classnames({
                      'text-base': true,
                      'py-4': true,
                      'font-medium': true,
                      'text-center': true,
                      'hover-active-bg': true,
                      active: String(activedRecord) === String(id),
                    })}
                    key={id}
                    onClick={() => handleClick(id)}
                  >
                    <CustomIcon className="mr-2" type="rw" />
                    {end
                      ? `${moment(start).format('YYYY/MM/DD')}-${moment(end).format('YYYY/MM/DD')}`
                      : moment(start).format('YYYY-MM-DD')}
                  </li>
                ))}
              </ul>
              {total && (
                <Pagination
                  className="text-center mt-3"
                  simple
                  defaultCurrent={1}
                  total={total}
                  onChange={(no) => handleChange(no)}
                />
              )}
            </Holder>
          </Spin>
        </div>
      </div>
      <div className="flex-1 pl-4 overflow-auto h-full">
        <Spin spinning={getReportTaskRecordLoading}>
          <Holder when={isEmpty(layout)}>
            <BoardGrid.Pure layout={layout} />
          </Holder>
        </Spin>
      </div>
    </div>
  );
};

export default ReportRecords;
