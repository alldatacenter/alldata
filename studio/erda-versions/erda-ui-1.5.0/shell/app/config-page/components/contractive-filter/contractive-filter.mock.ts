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

const mockData: CP_FILTER.Spec = {
  type: 'ContractiveFilter',
  props: {
    delay: 300,
  },
  state: {
    conditions: [
      {
        emptyText: '全部',
        fixed: true,
        key: 'title',
        label: '标题',
        placeholder: '请输入标题',
        showIndex: 2,
        type: 'input',
      },
      {
        emptyText: '全部',
        key: 'priorities',
        label: '优先级',
        options: [
          {
            label: '紧急',
            value: 'URGENT',
          },
          {
            label: '高',
            value: 'HIGH',
          },
          {
            label: '中',
            value: 'NORMAL',
          },
          {
            label: '低',
            value: 'LOW',
          },
        ],
        placeholder: '选择优先级',
        type: 'select',
      },
      {
        emptyText: '全部',
        haveFilter: true,
        key: 'creatorIDs',
        label: '创建人',
        options: [
          {
            label: '陈泳伸',
            value: '1000014',
          },
          {
            label: '徐伟',
            value: '1000045',
          },
        ],
        quickSelect: {
          label: '选择自己',
          operationKey: 'ownerSelectMe',
        },
        type: 'select',
      },
      {
        emptyText: '全部',
        key: 'createdAtStartEnd',
        label: '创建日期',
        type: 'dateRange',
      },
    ],
    values: {
      priorities: ['HIGH'],
    },
  },
  operations: {
    filter: {
      key: 'filter',
      reload: true,
    },
    ownerSelectMe: {
      key: 'ownerSelectMe',
      reload: true,
    },
  },
};

export default mockData;
