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

import { get, map, isEmpty, forEach, reduce } from 'lodash';

/** *
 *  monitor重构后，返回数据格式为：
 * {
        "title":"访问量(7天内)",
        "time":[1545494400000,...],
        "results":[
            {
              "name":"ta_timing",
              "data":[
                {"count.plt":{"name":"请求数","data":[...],...}
                ]
            }
        ]
    }

    chart中需要的返回数据为：
    {
      time:[...]
      results:[
        {name,data,....}
      ]
    }
 *
 *  */

// groupHandler: 获取返回数据中data下的单个key的data
export const groupHandler = (dataItemKey) => (originData) => {
  if (isEmpty(originData)) return {};
  const { results, time } = originData;
  const res = get(results, '[0].data') || [];
  return {
    time,
    results: map(res, (item) => {
      if (!dataItemKey) {
        const { tag, name, data, ...rest } = Object.values(item)[0];
        return { ...rest, name: tag || name, data };
      } else {
        const { tag, name, data, ...rest } = item[dataItemKey] || {};
        return { ...rest, name: tag || name, data };
      }
    }),
  };
};

// sort返回数据为：{list:[{name,value},...]}
export const sortHandler =
  (dataItemKey) =>
  (originData = [], { extendHandler }) => {
    const list = get(originData, 'results[0].data') || [];
    const dataKey = dataItemKey || (extendHandler && extendHandler.dataKey);
    return {
      list: map(list, (item) => {
        const { tag, name, data, unit = '' } = item[dataKey] || {};
        return { name: tag || name, value: data, unit };
      }),
    };
  };

// 慢加载：返回值为：{list:[{....}]}
export const slowHandler =
  (dataKeys) =>
  (originData = []) => {
    if (isEmpty(originData)) return {};
    const list = get(originData, 'results[0].data') || [];
    return {
      list: map(list, (item) => {
        const reData = {};
        forEach(dataKeys, (keyMap) => {
          const [key, dataKey] = keyMap.split(':');
          const { tag, name, data } = item[dataKey] || {};
          reData[key] = data;
          reData.name = tag || name;
        });
        return reData;
      }),
    };
  };

// 错误事务：返回值为：{list:[{....}]}
export const errorHttpHandler =
  () =>
  (originData = []) => {
    if (isEmpty(originData)) return {};
    const list = get(originData, 'results[0].data') || [];
    return {
      list: reduce(
        list,
        (result, item) => {
          const { data, tag: name } = item;
          forEach(data, (subItem) => {
            const subData = get(subItem, 'sum.elapsed_count');
            const timeData = get(subItem, 'maxFieldTimestamp.elapsed_max');
            const appData = get(subItem, 'last.tags.source_application_id');
            const { data: count, tag: httpCode } = subData;
            const { data: time } = timeData;
            result.push({ name, count, httpCode, time, applicationId: appData.data });
          });
          return result;
        },
        [],
      ),
    };
  };

// 错误SQL：返回值为：{list:[{....}]}
export const errorDbHandler =
  () =>
  (originData = []) => {
    if (isEmpty(originData)) return {};
    const list = get(originData, 'results[0].data') || [];
    return {
      list: reduce(
        list,
        (result, item) => {
          const { data: count, tag: name } = get(item, 'sum.elapsed_count');
          const { data: time } = get(item, 'maxFieldTimestamp.elapsed_max');
          result.push({ name, count, time });
          return result;
        },
        [],
      ),
    };
  };

// 获取多组数据: 返回值中取data[0]下的多个不同维度的key数据
export const multipleDataHandler = (dataKeys) => (originData) => {
  if (isEmpty(originData)) return {};
  const { time, results } = originData || {};
  const data = get(results, '[0].data[0]') || {};
  const reData = [];
  forEach(dataKeys, (item) => {
    data[item] && reData.push(data[item]);
  });
  return { time, results: reData };
};

export const multipleGroupDataHandler = (dataKeys) => (originData) => {
  if (isEmpty(originData)) return {};
  const { time, results } = originData || {};
  const data = get(results, '[0].data') || [];
  const reData = [];
  forEach(data, (d) => {
    forEach(dataKeys, (key) => {
      const item = d[key];
      if (item) {
        const { tag, name } = item;
        reData.push({ ...d[key], tag: name, group: tag || name });
      }
    });
  });
  return { time, results: reData };
};
