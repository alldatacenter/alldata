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

import { get, isEmpty, forEach, reduce } from 'lodash';
import { multiGroupsAndDimensionDataHandler } from 'app/modules/msp/monitor/api-insight/pages/request/config/apiConfig';
import { gatewayApiPrefix } from '../../config';

const commonQuery = {};

export const ApiMap = {
  memory: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/container_mem/histogram`,
    query: {
      ...commonQuery,
      group: 'container_id',
      avg: ['limit', 'usage', 'usage_percent'],
    },
    dataHandler: (originData = {}) => {
      if (isEmpty(originData)) return {};
      const { time = [], results = [] }: any = originData || {};
      const data = get(results, '[0].data') || [];
      const parsedData = reduce(
        data,
        (result: any, value) => {
          const reData: any[] = [];
          const usage_percent = value['avg.usage_percent'];

          const ip = usage_percent.tag.slice(0, 8);
          forEach(['avg.usage', 'avg.usage_percent'], (item) => {
            const dataItem: any = value[item];
            if (dataItem) {
              dataItem.tag = dataItem.name;
              reData.push(dataItem);
            }
          });

          return { ...result, [ip]: reData };
        },
        {},
      );

      // const parsedLines = reduce(data, (result: any, value) => {
      //   const reData: any[] = [];
      //   const limit = value['avg.limit'];

      //   const ip = limit.tag;
      //   if (limit) {
      //     limit.tag = limit.name;
      //     reData.push(limit);
      //   }

      //   return { ...result, [ip]: reData };
      // }, {});

      return { time, results: parsedData };

      // const { time, results } = originData as any;
      // const data = get(results, '[0].data[0]');
      // const limit = data['avg.limit'];
      // const usage = data['avg.usage'];
      // const usage_percent = data['avg.usage_percent'];
      // const reData = [{ ...usage }, { ...usage_percent }];
      // const finalData: any = {
      //   time, results: reData,
      // };
      // if (limit) {
      //   finalData.lines = [{
      //     name: '内存使用限制',
      //     value: find(limit.data, num => num !== 0) || 0,
      //   }];
      // }
      // return finalData;
    },
  },
  cpu: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/container_cpu/histogram`,
    query: {
      ...commonQuery,
      group: 'container_id',
      avg: 'usage_percent',
    },
    dataHandler: multiGroupsAndDimensionDataHandler(['avg.usage_percent']),
  },
  disk: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/container_disk_io/histogram`,
    query: {
      ...commonQuery,
      group: 'container_id',
      diffps: ['io_service_bytes_recursive_read', 'io_service_bytes_recursive_write'],
    },
    dataHandler: multiGroupsAndDimensionDataHandler([
      'diffps.io_service_bytes_recursive_read',
      'diffps.io_service_bytes_recursive_write',
    ]),
  },
  network: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/container_net_io/histogram`,
    query: {
      ...commonQuery,
      group: 'container_id',
      diffps: ['tx_bytes', 'rx_bytes'],
    },
    dataHandler: multiGroupsAndDimensionDataHandler(['diffps.rx_bytes', 'diffps.tx_bytes']),
  },
};
