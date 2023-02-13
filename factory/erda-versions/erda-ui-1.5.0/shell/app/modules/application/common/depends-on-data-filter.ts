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

import { forEach, cloneDeep, set } from 'lodash';
import { message } from 'antd';
import i18n from 'i18n';

let matchedService: string[] = [];
const checkCircularDep = (data: object) => {
  const circularDep = {};
  let circularDepPair: string[] = [];
  forEach(data, (value: any, key: string) => {
    // 依赖自身
    if (value.depends_on && value.depends_on.includes(key)) {
      circularDepPair = [key, key];
      return;
    }
    if (circularDep[key] && value.depends_on && value.depends_on.includes(circularDep[key])) {
      circularDepPair = [key, circularDep[key]];
      return;
    }
    forEach(value.depends_on, (dep: string) => {
      circularDep[dep] = key;
    });
  });
  return circularDepPair;
};

const getParentItem = (name: string, object: any) => {
  let parents: any[] = [];
  const matchResult: any[] = [];
  const group: any[] = [];

  forEach(object, (value: any, key: string) => {
    if (value.depends_on && value.depends_on.includes(name)) {
      matchResult.push({
        ...value,
        name: key,
      });
    }
  });

  if (matchResult.length) {
    matchResult.forEach((item: any) => {
      const itemParents = getParentItem(item.name, object);
      set(item, 'parents', itemParents);
    });

    if (group.length) {
      group.push(matchResult);
      parents = group;
    } else {
      parents = matchResult;
    }
  }

  return parents;
};

const setParents = (item: any, target: any[]) => {
  if (item.parents.length) {
    target.unshift(item.parents);
    item.parents.forEach((i: any) => {
      setParents(i, target);
    });
  }
};

const mergeTree = (index: number, tree: any[], target: any[]) => {
  if (!target[index]) {
    // eslint-disable-next-line no-param-reassign
    target[index] = [];
  }

  let match = false;
  tree.forEach((group: any[]) => {
    group[index] &&
      group[index].forEach((i: any) => {
        match = true;
        const result = target[index].find((item: any) => item.name === i.name);
        const added = matchedService.includes(i.name);
        if (!result && !added) {
          matchedService.push(i.name);
          target[index].push(i);
        }
      });
  });

  if (match) {
    mergeTree(index + 1, tree, target);
  }
};

export default (currentItem: any) => {
  matchedService = [];
  const originData = cloneDeep(currentItem);

  const circularDep = checkCircularDep(originData);
  if (circularDep.length) {
    message.warning(
      `${i18n.t('dop:detected circular dependencies')}：${circularDep.join(' 和 ')}， ${i18n.t(
        'dop:the loop node does not render',
      )}`,
    );
    return [];
  }

  const items: any[] = [];
  const noDependsOn: any[] = [];
  forEach(originData, (value: any, key: string) => {
    if (!value.depends_on || (value.depends_on && !value.depends_on.length)) {
      noDependsOn.push({
        value,
        key,
      });
      delete originData[key];
    }
  });

  const tree: any[] = [];
  noDependsOn.forEach(({ key, value }: any, index: number) => {
    tree[index] = [];
    const result = getParentItem(key, currentItem);
    if (!result.length) {
      if (!items[0]) {
        tree[index][0] = [
          {
            ...value,
            name: key,
          },
        ];
      } else {
        tree[index][0].push({
          ...value,
          name: key,
        });
      }
    } else {
      result.forEach((item: any) => {
        const newItem = {
          ...item,
        };
        delete newItem.parents;
        tree[index].unshift([newItem]);
        setParents(item, tree[index]);
      });

      tree[index].push([
        {
          ...value,
          name: key,
        },
      ]);
    }
  });

  mergeTree(0, tree, items);

  return items.filter((i: any[]) => i.length);
};
