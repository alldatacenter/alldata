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

import { forEach, isEmpty, get, mapKeys } from 'lodash';

/**
 * 转换results[0]data为标准格式
 * @param {*} responseData
 */
export const monitorDataConvertor = (responseData: any) => {
  if (isEmpty(responseData)) return {};
  const { time = [], results = [] } = responseData || {};
  const data = get(results, '[0].data') || [];
  const yAxis = [];
  const metricData = [] as object[];
  // data: [{ k: {...} }, { k: {...} }]
  forEach(data, (item) => {
    mapKeys(item, (v) => {
      const { chartType, ...rest } = v;
      yAxis[v.axisIndex] = 1;
      metricData.push({
        ...rest,
        name: v.tag || v.name,
        type: chartType,
      });
    });
  });
  const yAxisLength = yAxis.length;
  return { xData: time, metricData, yAxisLength, xAxisIsTime: true };
};
