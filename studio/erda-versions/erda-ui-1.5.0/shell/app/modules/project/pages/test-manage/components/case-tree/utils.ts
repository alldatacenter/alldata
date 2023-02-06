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

import { find, get, isArray, reduce } from 'lodash';
import i18n from 'i18n';

export const rootId = 0;
export const rootKey = '0';
export const recycledId = 'recycled' as any as number;
export const recycledKey = `${rootId}-${recycledId}`;
export const recycledRoot: TEST_SET.TestSetNode = {
  title: i18n.t('dop:recycle bin'),
  key: recycledKey,
  iconType: 'ljt',
  iconClass: 'text-red',
  id: recycledId,
  parentID: rootId,
  recycled: true,
  isLeaf: false,
};

interface IFunc {
  treeData: object[]; // treeData 树形结构
  eventKey: string; // 取值路径
  valueKey?: string; // 取值字段
}

// 依据eventKey获取当前node或者该node某个字段的值
export const getNodeByPath = ({ treeData, eventKey, valueKey }: IFunc): any => {
  if (!eventKey) {
    return '';
  }
  const list = eventKey.split('-').join(' children ').split(' ');
  if (valueKey) list.push(valueKey);
  return reduce(
    list,
    (result, singleKey) => {
      if (!result) {
        // 第一次
        const firstKey = get(treeData, [0, 'key']);
        if (firstKey === singleKey) {
          return get(treeData, [0]);
        }
      } else if (singleKey === 'children') {
        // 子节点则继续返回
        return get(result, ['children']);
      } else if (isArray(result)) {
        // 获取children中匹配的node
        return find(result, ({ id }: any) => `${id}` === singleKey);
      }
      // 最终单个节点的某个字段，比如key、title、id等
      return get(result, [singleKey]);
    },
    '',
  );
};

// 依据id获取当前node
interface IById {
  treeData: object[]; // treeData 树形结构
  id: number;
  recycled?: boolean;
}

export const getNodeById = ({ treeData, id, recycled = true }: IById): TEST_SET.TestSetNode | any =>
  reduce(
    treeData,
    (result, single: any) => {
      if (result) {
        // 已经查出来了
        return result;
      }
      if (id === single.id && recycled === single.recycled) {
        // 找到了
        return single;
      }
      // 继续在子级查找
      return getNodeById({ treeData: single.children, id, recycled });
    },
    null,
  );

interface ISelectParams {
  selectedKey: string;
  selectProjectId: number;
  projectId: number;
}
export const getSelectedKeys = ({ selectedKey }: ISelectParams) => {
  if (!selectedKey) {
    // 初始时没有节点
    return ['0'];
  }
  return [selectedKey];
};

export const getEventKeyId = (eventKey: string): number => {
  const list = eventKey.split('-');
  return parseInt(list[list.length - 1], 10);
};
