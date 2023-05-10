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

import { values, map, merge, filter, isEmpty } from 'lodash';
import { getChartData } from 'cmp/services/custom-dashboard';

export const createLoadDataFn = (api: any, chartType: string) => async (payload?: any) => {
  let extraQuery = payload;
  if (typeof payload === 'string') {
    try {
      extraQuery = JSON.parse(payload);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const { data } = await getChartData(merge({}, api, { query: extraQuery }));
  if (['chart:line', 'chart:area', 'chart:bar'].includes(chartType)) {
    const { results, ...rest } = data;
    if (results[0].data.length > 1) {
      return {
        ...rest,
        metricData: map(results[0].data, (item) => values(item)[0]),
      };
    } else {
      return {
        ...rest,
        metricData: results[0].data[0],
      };
    }
  }
  if (chartType === 'chart:pie') {
    return {
      metricData: !isEmpty(data.metricData)
        ? [
            {
              name: data.title || '',
              data: map(data.metricData, ({ title, value, name }) => ({ name: title || name, value })),
            },
          ]
        : [],
    };
  }
  // 新的统一返回结构
  if (chartType === 'chart:map') {
    const { cols, data: _data } = data;
    const aliases = filter(
      map(cols, (col) => col.key),
      (alias) => alias !== 'map_name',
    );
    const metricData = map(aliases, (alias) => ({
      name: alias,
      data: map(_data, (item) => ({
        name: item.map_name,
        value: item[alias],
      })),
    }));

    return { metricData };
  }
  return data;
};
