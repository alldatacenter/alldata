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
import { groupHandler, slowHandler } from 'common/utils/chart-utils';
import { gatewayApiPrefix } from '../../config';

export interface IOriginData {
  time: number[];
  results: any[];
}

const commonQuery = {};

// 特殊处理：多组多维度数据结构
export const multiGroupsAndDimensionDataHandler =
  (dataKeys: string[], tagLength?: number) => (originData: IOriginData) => {
    if (isEmpty(originData)) return {};
    const { time = [], results = [] } = originData || {};
    const data = get(results, '[0].data') || [];
    const parsedData = reduce(
      data,
      (result: any, value) => {
        const reData: any[] = [];
        const ip = value[dataKeys[0]].tag.slice(0, tagLength || 8);
        forEach(dataKeys, (item) => {
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

    return { time, results: parsedData };
  };

export const ApiMap = {
  qps: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_qps/histogram`,
    query: { ...commonQuery, sumCps: 'cnt_count' },
    dataHandler: groupHandler(['sumCps.cnt_count']),
  },
  pv: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_pv`,
    query: { limit: 10, sum: 'cnt_count', sort: 'sum_cnt_count', group: 'mapi' },
    dataHandler: slowHandler(['pv:sum.cnt_count']),
  },
  connect: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_connection/histogram`,
    query: { ...commonQuery, group: 'myip', avg: ['crd_mean', 'cwr_mean', 'cac_mean', 'cwa_mean'] },
    dataHandler: multiGroupsAndDimensionDataHandler(
      ['avg.crd_mean', 'avg.cwr_mean', 'avg.cac_mean', 'avg.cwa_mean'],
      20,
    ),
  },
};
