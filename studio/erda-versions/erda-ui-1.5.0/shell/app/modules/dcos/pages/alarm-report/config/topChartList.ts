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

import { get, isEmpty, map } from 'lodash';
import { chartRender } from 'api-insight/common/components/apiRenderFactory';
import { topTable } from '../topTable';
import i18n from 'i18n';

const dataHandler = (dataKey: string) => (originData: any) => {
  if (isEmpty(originData)) return {};
  const list = get(originData, 'results[0].data') || [];

  return {
    list: map(list, (item: any) => {
      const { tag: name, data: value } = item.data[0][dataKey];
      return {
        id: item.tag,
        name,
        value,
      };
    }),
  };
};

export const commonAttr = {
  moduleName: 'alarmReport',
  isCustomApi: true,
  fetchApi: '/api/orgCenter/metrics/procstat',
};

const commonQuery = {
  start: 'before_1m',
  end: 'after_1m',
  group: ['pid', 'process_name'],
  limit: 10,
};

const CHART_CONFIG_MAP = {
  processTopCPU: {
    ...commonAttr,
    titleText: `CPU ${i18n.t('cmp:usage rate')} TOP10`,
    chartName: 'processTopCPU',
    viewRender: topTable,
    viewProps: { valueTitle: i18n.t('cmp:usage rate'), unitType: 'PERCENT' },
    query: {
      ...commonQuery,
      sort: 'max_cpu_usage',
      max: 'cpu_usage',
    },
    dataHandler: dataHandler('max.cpu_usage'),
  },
  processTopRead: {
    ...commonAttr,
    titleText: `${i18n.t('cmp:read speed')} TOP10`,
    chartName: 'processTopRead',
    viewRender: topTable,
    viewProps: { valueTitle: i18n.t('cmp:speed'), unitType: 'TRAFFIC', unit: 'B/S' },
    query: {
      ...commonQuery,
      sort: 'max_read_rate',
      max: 'read_rate',
    },
    dataHandler: dataHandler('max.read_rate'),
  },
  processTopWrite: {
    ...commonAttr,
    titleText: `${i18n.t('cmp:write speed')} TOP10`,
    chartName: 'processTopWrite',
    viewRender: topTable,
    viewProps: { valueTitle: i18n.t('cmp:speed'), unitType: 'TRAFFIC', unit: 'B/S' },
    query: {
      ...commonQuery,
      sort: 'max_write_rate',
      max: 'write_rate',
    },
    dataHandler: dataHandler('max.write_rate'),
  },
};

export default map(CHART_CONFIG_MAP, (config) => chartRender(config));
