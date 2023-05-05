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

const mockData: CP_CONFIGURABLE_FILTER.Spec = {
  type: 'ConfigurableFilter',
  data: {
    conditions: [
      {
        key: 'iterationIDs',
        type: 'select',
        label: '迭代',
        options: [
          {
            label: '1.2',
            value: 708,
          },
          {
            label: '1.1',
            value: 687,
          },
        ],
        placeholder: '选择迭代',
      },
      {
        label: '状态',
        options: [
          {
            children: [
              {
                label: '待处理',
                value: 21005,
                status: 'warning',
              },
              {
                label: '进行中',
                value: 21006,
                status: 'processing',
              },
              {
                label: '已完成',
                value: 21007,
                status: 'success',
              },
            ],
            icon: 'ISSUE_ICON.issue.TASK',
            label: '任务',
            value: 'TASK',
          },
          {
            children: [
              {
                label: '待处理',
                value: 21011,
                status: 'warning',
              },
              {
                label: '进行中',
                value: 21012,
                status: 'processing',
              },
              {
                label: '无需修复',
                value: 21013,
                status: 'default',
              },
              {
                label: '重复提交',
                value: 21014,
                status: 'default',
              },
              {
                label: '已解决',
                value: 21015,
                status: 'success',
              },
              {
                label: '重新打开',
                value: 21016,
              },
              {
                label: '已关闭',
                value: 21017,
                status: 'success',
              },
            ],
            icon: 'ISSUE_ICON.issue.BUG',
            label: '缺陷',
            value: 'BUG',
          },
          {
            children: [
              {
                label: '待处理',
                value: 21001,
                status: 'warning',
              },
              {
                label: '进行中',
                value: 21002,
                status: 'processing',
              },
              {
                label: '测试中',
                value: 21003,
                status: 'processing',
              },
              {
                label: '已完成',
                value: 21004,
                status: 'success',
              },
            ],
            icon: 'ISSUE_ICON.issue.REQUIREMENT',
            label: '需求',
            value: 'REQUIREMENT',
          },
        ],
        type: 'select',
        key: 'states',
      },
      {
        key: 'labelIDs',
        label: '标签',
        options: [
          {
            label: 'cba',
            value: 50,
          },
          {
            label: 'abc',
            value: 49,
          },
        ],
        placeholder: '请选择标签',
        type: 'select',
      },
      {
        type: 'select',
        key: 'priorities',
        label: '优先级',
        options: [
          {
            label: '紧急',
            value: 'URGENT',
          },
          {
            value: 'HIGH',
            label: '高',
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
      },
      {
        label: '严重程度',
        options: [
          {
            label: '致命',
            value: 'FATAL',
          },
          {
            value: 'SERIOUS',
            label: '严重',
          },
          {
            label: '一般',
            value: 'NORMAL',
          },
          {
            label: '轻微',
            value: 'SLIGHT',
          },
          {
            label: '建议',
            value: 'SUGGEST',
          },
        ],
        key: 'severities',
        placeholder: '选择严重程度',
        type: 'select',
      },
      {
        key: 'creatorIDs',
        label: '创建人',
        options: [
          {
            label: 'test',
            value: '12022',
          },
          {
            label: 'dice',
            value: '2',
          },
        ],
        type: 'select',
      },
      {
        key: 'assigneeIDs',
        options: [
          {
            value: '12022',
            label: 'test',
          },
          {
            label: 'dice',
            value: '2',
          },
        ],
        type: 'select',
        label: '处理人',
      },
      {
        type: 'dateRange',
        emptyText: '全部',
        key: 'createdAtStartEnd',
        label: '创建日期',
      },
      {
        type: 'dateRange',
        key: 'finishedAtStartEnd',
        label: '截止日期',
      },
    ],
    filterSet: [
      { id: 1, data: {}, label: '全部打开', isPreset: true },
      { id: 2, data: { states: [1, 2] }, label: '自定义筛选器', isPreset: false },
    ],
  },
  state: {
    values: {
      priorities: ['HIGH'],
    },
    selectedFilterSet: 4,
  },
  operations: {
    deleteFilterSet: { fillMeta: 'id', key: 'deleteFilter', reload: true },
    filter: { key: 'filter', reload: true },
    saveFilterSet: { fillMeta: 'name', key: 'saveFilter', reload: true },
  },
};

export default mockData;
