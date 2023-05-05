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

import { get, find } from 'lodash';
import { commonChartRender } from 'monitor-common';
import { multipleDataHandler, groupHandler } from 'common/utils/chart-utils';
import i18n from 'i18n';

const commonQuery = {};
const apiPrefix = '/api/spot/metrics/charts';

const chartConfig = (api?: string) => {
  return {
    containerMem: {
      fetchApi: `${api || apiPrefix}/container_top/histogram`,
      query: { ...commonQuery, avg: ['mem_limit', 'mem_usage', 'mem_usage_percent'] },
      dataHandler: (originData = {}) => {
        const { time, results } = originData as any;
        const data = get(results, '[0].data[0]');
        const limit = data['avg.mem_limit'];
        const usage = data['avg.mem_usage'];
        const usage_percent = data['avg.mem_usage_percent'];
        const reData = [{ ...usage }, { ...usage_percent }];
        const finalData: any = {
          time,
          results: reData,
        };
        if (limit) {
          finalData.lines = [
            {
              name: i18n.t('msp:memory usage limit'),
              value: find(limit.data, (num) => num !== 0) || 0,
            },
          ];
        }
        return finalData;
      },
      moduleName: 'machineUnit',
      groupId: 'machineUnitChart',
      titleText: i18n.t('memory'),
      chartName: 'container_mem',
      viewProps: { unitTypes: ['CAPACITY', 'PERCENT'], rightUnitType: 'PERCENT', isTwoYAxis: true },
    },
    containerCpu: {
      fetchApi: `${api || apiPrefix}/container_top/histogram`,
      query: { ...commonQuery, avg: 'cpu_usage_percent' },
      dataHandler: groupHandler('avg.cpu_usage_percent'),
      moduleName: 'machineUnit',
      groupId: 'machineUnitChart',
      titleText: 'CPU',
      chartName: 'container_cpu',
      viewProps: { unitType: 'PERCENT' },
    },
    containerIo: {
      fetchApi: `${api || apiPrefix}/container_top/histogram`,
      query: { ...commonQuery, diffps: ['blk_read_bytes', 'blk_write_bytes'] },
      dataHandler: multipleDataHandler(['diffps.blk_read_bytes', 'diffps.blk_write_bytes']),
      moduleName: 'machineUnit',
      groupId: 'machineUnitChart',
      titleText: i18n.t('disk'),
      chartName: 'container_io',
      viewProps: { unitType: 'TRAFFIC', rightUnitType: 'TRAFFIC', isTwoYAxis: true },
    },
    containerNet: {
      fetchApi: `${api || apiPrefix}/container_top/histogram`,
      query: { ...commonQuery, diffps: ['rx_bytes', 'tx_bytes'] },
      dataHandler: multipleDataHandler(['diffps.rx_bytes', 'diffps.tx_bytes']),
      moduleName: 'machineUnit',
      groupId: 'machineUnitChart',
      titleText: i18n.t('network'),
      chartName: 'container_net',
      viewProps: { unitType: 'TRAFFIC', rightUnitType: 'TRAFFIC', isTwoYAxis: true },
    },
  };
};

export default (api?: string) => {
  const config = chartConfig(api);
  return {
    containerMem: commonChartRender(config.containerMem),
    containerCpu: commonChartRender(config.containerCpu),
    containerIo: commonChartRender(config.containerIo),
    containerNet: commonChartRender(config.containerNet),
  };
};
