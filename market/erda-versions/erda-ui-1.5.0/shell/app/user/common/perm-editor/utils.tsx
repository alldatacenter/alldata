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

import { map, maxBy, sortBy, filter, some } from 'lodash';

export const getTableList = (data: IPerm, scope: string, filterKey: string) => {
  let list = [] as any[];
  const countData = (curData: any, key = scope, depth = 0, prevData = {}) => {
    if (!curData) return;
    if (curData.role) {
      list.push({ ...prevData, action: { ...curData, key } });
    } else {
      const { name, ...rest } = curData;
      map(rest, (item, cKey) => {
        const curPrevData = { ...prevData, [`depth${depth}`]: { key, name } };
        countData(item, cKey, depth + 1, curPrevData);
      });
    }
  };
  countData(data);

  if (filterKey) {
    list = filter(list, (l) => some(l, (item) => item.key.includes(filterKey) || item.name.includes(filterKey)));
  }

  const maxDeepthObj = maxBy(list, (item) => Object.keys(item).length);
  const tableList = [] as any[];
  map(list, (item) => {
    const itemData = { ...item };
    map(maxDeepthObj, (val, key) => {
      if (!itemData[key]) {
        itemData[key] = {};
      }
    });
    const sortKeys = sortBy(Object.keys(itemData), (k) => (k.startsWith('depth') ? Number(k.slice(5)) : 1000));
    itemData.actionKey = map(sortKeys, (k) => itemData[k].key || '__').join('');
    tableList.push(itemData);
  });
  return map(
    sortBy(tableList, (item) => item.actionKey),
    ({ actionKey, ...rest }) => rest,
  );
};
