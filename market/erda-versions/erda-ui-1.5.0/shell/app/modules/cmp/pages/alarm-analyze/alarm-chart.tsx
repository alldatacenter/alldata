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
import i18n from 'i18n';
import moment from 'moment';
import { get, map, isEmpty, isArray } from 'lodash';
import { ChartBaseFactory } from 'monitor-common';

const chartRender = (obj: any) => {
  if (!obj) return null;
  const reQuery = { ...(obj.query || {}), dependentKey: { start: 'startTimeMs', end: 'endTimeMs' } };
  const { fetchApi: api, ...rest } = obj;
  const Chart = ChartBaseFactory.create({ ...rest });
  return (props: any) => {
    const { query, ...ownProps } = props;
    const fetchApi = api && `/api/alert/metrics/${api}`;
    return <Chart {...rest} {...ownProps} fetchApi={fetchApi} query={{ ...query, ...reQuery }} />;
  };
};

const commonChartAttr = {
  moduleName: 'alarmChart',
};

const CHART_MAP = {
  alarmTrend: {
    ...commonChartAttr,
    titleText: i18n.t('alarm volume trend (within 7 days)'),
    chartName: 'alarmTrend',
    fetchApi: 'alert_trend/histogram',
    query: {
      points: 7,
      filter_alert_scope: 'org',
      count: 'tags.alert_id',
      group: 'cluster_name',
    },
    dataHandler: (originData: any) => {
      if (isEmpty(originData)) return {};
      const { results, time } = originData;
      const res = get(results, '[0].data') || [];
      return {
        results: map(res, (item) => {
          const { tag, name, data, ...rest }: any = item['count.tags.alert_id'] || {};
          return { ...rest, name: tag || name, data };
        }),
        xAxis: isArray(time) && map(time, (_time) => moment(Number(_time)).format('MM/DD')),
      };
    },
  },
  alarmProportion: {
    ...commonChartAttr,
    titleText: i18n.t('alarm volume statistics (within 7 days)'),
    viewType: 'pie',
    chartName: 'alarmProportion',
    fetchApi: 'alert_statistics',
    query: {
      filter_alert_scope: 'org',
      count: 'tags.alert_id',
      group: 'cluster_name',
      extendHandler: { seriesName: i18n.t('number of alarms') },
    },
    dataHandler: (originData: object, { extendHandler }: any) => {
      const { seriesName } = extendHandler;
      const pieArr: any[] = [];
      const data = get(originData, 'results[0].data');
      if (data) {
        map(data, (item) => {
          const { tag = '', name = '', ...rest } = item['count.tags.alert_id'] || {};
          pieArr.push({ name: tag || name, value: rest.data });
        });
      }
      let results: any[] = [];
      if (pieArr.length) {
        results = [{ name: seriesName, data: pieArr }];
      }
      return { results };
    },
  },
  alarmTypeProportion: {
    ...commonChartAttr,
    titleText: i18n.t('alarm type statistics (within 7 days)'),
    viewType: 'pie',
    chartName: 'alarmTypeProportion',
    fetchApi: 'alert_type_percent',
    query: {
      filter_alert_scope: 'org',
      count: 'tags.alert_id',
      group: 'alert_type',
      extendHandler: { seriesName: i18n.t('alarm type') },
    },
    dataHandler: (originData: object, { extendHandler }: any) => {
      const { seriesName } = extendHandler;
      const pieArr: any[] = [];
      const data = get(originData, 'results[0].data');
      if (data) {
        map(data, (item) => {
          const { tag = '', name = '', ...rest } = item['count.tags.alert_id'] || {};
          pieArr.push({ name: tag || name, value: rest.data });
        });
      }
      let results: any[] = [];
      if (pieArr.length) {
        results = [{ name: seriesName, data: pieArr }];
      }
      return { results };
    },
  },
};

export default {
  AlarmTrendChart: chartRender(CHART_MAP.alarmTrend) as any,
  AlarmProportionChart: chartRender(CHART_MAP.alarmProportion) as any,
  AlarmTypeProportionChart: chartRender(CHART_MAP.alarmTypeProportion) as any,
};
