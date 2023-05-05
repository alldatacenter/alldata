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

const mockData: CP_TREE_SELECT.Spec = {
  type: 'TreeSelect',
  data: {
    treeData: [
      { key: 'g0', id: 'g0', pId: '0', title: 'g0', value: 'v-g0', isLeaf: false, selectable: false, disabled: true },
      { key: 'g1', id: 'g1', pId: '0', title: 'g1', value: 'v-g1', isLeaf: false, selectable: false, disabled: true },
      { key: 'g2', id: 'g2', pId: '0', title: 'g2', value: 'v-g2', isLeaf: false, selectable: false, disabled: true },
    ],
  },
  state: { value: { value: '测试', label: '测试2' } },
  props: {
    placeholder: '请选择',
    title: '请选择配置单',
  },
  operations: {
    onSearch: {
      key: 'search',
      reload: true,
      fillMeta: 'searchKey',
      meta: { searchKey: '' },
    },
    onChange: {
      key: 'onChange',
      reload: true,
      fillMeta: 'value',
      meta: { value: '' },
    },
    onLoadData: {
      key: 'onLoadData',
      reload: true,
      fillMeta: 'pId',
      meta: { pId: '' },
    },
  },
};

export default mockData;
