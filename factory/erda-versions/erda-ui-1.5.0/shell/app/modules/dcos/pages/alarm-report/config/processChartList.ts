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

import { map } from 'lodash';
import { chartRender } from 'api-insight/common/components/apiRenderFactory';
import { groupHandler, multipleDataHandler } from 'common/utils/chart-utils';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'alarmReport',
  isCustomApi: true,
  fetchApi: '/api/orgCenter/metrics/procstat/histogram',
};

const commonQuery = {
  start: 'before_20m',
  end: 'after_10m',
};

const CHART_CONFIG_MAP = {
  processCPU: {
    ...commonAttr,
    titleText: `${i18n.t('cmp:process')} CPU ${i18n.t('cmp:usage rate')}`,
    chartName: 'processCPU',
    query: {
      ...commonQuery,
      max: 'cpu_usage',
    },
    viewProps: { unitType: 'PERCENT' },
    dataHandler: groupHandler('max.cpu_usage'),
  },
  processSpeed: {
    ...commonAttr,
    titleText: i18n.t('cmp:process read and write speed'),
    chartName: 'processSpeed',
    query: {
      ...commonQuery,
      max: ['read_rate', 'write_rate'],
    },
    viewProps: { unitType: 'TRAFFIC', unit: 'B/S' },
    dataHandler: multipleDataHandler(['max.read_rate', 'max.write_rate']),
  },
  processTime: {
    ...commonAttr,
    titleText: i18n.t('cmp:process read and write times'),
    chartName: 'processTime',
    query: {
      ...commonQuery,
      max: ['read_count', 'write_count'],
    },
    viewProps: { unitType: 'count' },
    dataHandler: multipleDataHandler(['max.read_count', 'max.write_count']),
  },
  processMem: {
    ...commonAttr,
    titleText: i18n.t('cmp:process memory'),
    chartName: 'processMem',
    query: {
      ...commonQuery,
      max: ['memory_data', 'memory_vms', 'memory_swap', 'memory_stack', 'memory_rss', 'memory_locked'],
    },
    viewProps: { unitType: 'CAPACITY', unit: 'B' },
    dataHandler: multipleDataHandler([
      'max.memory_data',
      'max.memory_vms',
      'max.memory_swap',
      'max.memory_stack',
      'max.memory_rss',
      'max.memory_locked',
    ]),
  },
  processDuration: {
    ...commonAttr,
    titleText: i18n.t('cmp:process duration'),
    chartName: 'processDuration',
    query: {
      ...commonQuery,
      max: 'up_time',
    },
    viewProps: { unit: 's' },
    dataHandler: groupHandler('max.up_time'),
  },
};

export default map(CHART_CONFIG_MAP, (config) => chartRender(config));
