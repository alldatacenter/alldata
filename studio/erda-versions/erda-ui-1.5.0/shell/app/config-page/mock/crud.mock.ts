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

import { cloneDeep } from 'lodash';

export const enhanceMock = (data: any, payload: any) => {
  console.log('------', payload);
  if (payload?.event?.operation === 'update') {
    const _data = cloneDeep(data);
    _data.protocol.components.gantt.data = {
      updateList: [
        // {
        //   start: getDate(1),
        //   end: getDate(10),
        //   title: 'R1-测试数据测试数据测试数据测试数据测试数据测试数据测试数据',
        //   key: 'R1',
        //   isLeaf: false,
        //   extra: {
        //     type: 'requirement',
        //     user: '张三',
        //     status: { text: '进行中', status: 'processing' },
        //   },
        // },
        {
          key: payload.event.operationData.meta.nodes.key,
          title: `T${payload.event.operationData.meta.nodes.key}测试测试测试测试测试测试测试测试测试测试测试`,
          start: payload.event.operationData.meta.nodes.start,
          end: payload.event.operationData.meta.nodes.end,
          isLeaf: true,
          extra: {
            type: 'task',
            user: '张三',
            status: { text: '进行中', status: 'processing' },
          },
        },
      ],
      expandList: null,
    };
    return _data;
  }
  if (payload.event?.operation === 'expandNode') {
    const _data = cloneDeep(data);
    _data.protocol.components.gantt.data = {
      expandList: {
        R2: [
          {
            id: '2-1',
            name: 'T1-1测试测试测试测试测试测试测试测试测试测试测试',
            start: getDate(1),
            end: getDate(5),
            isLeaf: true,
            extra: {
              type: 'task',
              user: '张三',
              status: { text: '进行中', status: 'processing' },
            },
          },
        ],
      },
    };
    return _data;
  }
  return data;
};
const currentDate = new Date();
const getDate = (day: number) => new Date(currentDate.getFullYear(), currentDate.getMonth(), day).getTime();

const makeData = (num: number) => {
  return new Array(num).fill('').map((_, idx) => ({
    key: `1-${idx + 1}`,
    title: `T1-${idx + 1}测试测试测试测试测试测试测试测试测试测试测试`,
    start: getDate(1),
    end: getDate(5),
    isLeaf: true,
    extra: {
      type: 'task',
      user: '张三',
      status: { text: '进行中', status: 'processing' },
    },
  }));
};

export const mockData = {
  scenario: {
    scenarioType: 'issue-gantt',
    scenarioKey: 'issue-gantt',
  },
  protocol: {
    hierarchy: {
      root: 'page',
      structure: {
        page: ['topHead', 'filter', 'ganttContainer'],
        topHead: ['issueAddButton'],
        ganttContainer: ['gantt'],
      },
    },
    components: {
      ganttContainer: { type: 'Container' },
      page: { type: 'Container' },
      topHead: {
        data: {},
        name: 'topHead',
        operations: {},

        state: {},
        type: 'RowContainer',
      },
      issueAddButton: {
        data: {},
        name: 'issueAddButton',
        operations: {},
        state: {},
        type: 'Button',
      },
      gantt: {
        type: 'Gantt',
        data: {
          expandList: {
            0: [
              {
                start: null, //new Date('2019-1-1').getTime(),
                end: null,
                title: 'Rss1-测试数据测试数据测试数据测试数据测试数据测试数据测试数据',
                key: 'R1ss',
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                start: getDate(1), //new Date('2019-1-1').getTime(),
                end: getDate(15),
                title:
                  'R1-测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据',
                key: 'R1',
                isLeaf: false,
                extra: {
                  type: 'requirement',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                start: getDate(10),
                end: getDate(20),
                title: 'R2-测试数据测试数据测试数据测试数据测试数据测试数据测试数据',
                key: 'R2',
                isLeaf: false,
                extra: {
                  type: 'requirement',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                start: getDate(10),
                end: getDate(20),
                title: 'R3-测试数据测试数据测试数据测试数据测试数据测试数据测试数据',
                key: 'R3',
                isLeaf: false,
                extra: {
                  type: 'requirement',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
            ],
            // R1: makeData(1000),
            R11: [
              {
                key: '1-1',
                title: 'T1-1测试测试测试测试测试测试测试测试测试测试测试',
                // start: getDate(1),
                // end: getDate(5),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
            ],
            R1: [
              {
                id: '1-1',
                name: 'T1-1测试测试测试测试测试测试测试测试测试测试测试',
                // start: getDate(1),
                // end: getDate(5),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-2',
                name: 'T1-2测试测试测试测试测试测试测试测试测试测试测试',
                // start: getDate(2),
                // end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'error' },
                },
              },
              {
                id: '1-3',
                name: 'T1-3测试测试测试测试测试测试测试测试测试测试测试',
                // start: getDate(2),
                // end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-4',
                name: 'T1-4测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(28),
                end: getDate(29),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'error' },
                },
              },
              {
                id: '1-5',
                name: 'T1-5测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(1),
                end: getDate(30),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'success' },
                },
              },
              {
                id: '1-6',
                name: 'T1-6测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-7',
                name: 'T1-7测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-8',
                name: 'T1-8测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-9',
                name: 'T1-9测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-10',
                name: 'T1-10测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-11',
                name: 'T1-11测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
              {
                id: '1-12',
                name: 'T1-12测试测试测试测试测试测试测试测试测试测试测试',
                start: getDate(2),
                end: getDate(10),
                isLeaf: true,
                extra: {
                  type: 'task',
                  user: '张三',
                  status: { text: '进行中', status: 'processing' },
                },
              },
            ],
          },
        },
        operations: {
          update: {
            key: 'update',
            reload: true,
            fillMeta: 'nodes',
            async: true,
            meta: {
              // 前端修改的数据放在meta.nodes里，update后，后端data.updateList返回相关修改
              nodes: [{ key: 'R1-1', start: 100, end: 1000 }],
            },
          },
          expandNode: {
            key: 'expandNode',
            reload: true,
            fillMate: 'keys',
            meta: { keys: ['xxx'] },
          },
        },
      },
      filter: {
        type: 'ContractiveFilter',
        name: 'filter',
        state: {
          conditions: [
            {
              emptyText: '全部',
              fixed: true,
              key: 'iteration',
              label: '迭代',
              options: [
                { label: '1.1', value: '1.1' },
                { label: '1.2', value: '1.2' },
              ],
              type: 'select',
            },
            {
              emptyText: '全部',
              fixed: true,
              haveFilter: true,
              key: 'user',
              label: '成员',
              options: [
                { label: '张三', value: '1' },
                { label: '李四', value: '1' },
              ],
              type: 'select',
            },
            {
              fixed: true,
              key: 'q',
              placeholder: '根据名称过滤',
              type: 'input',
            },
          ],
          values: {
            spaceName: '4',
          },
        },
        operations: {
          filter: {
            key: 'filter',
            reload: true,
          },
        },
      },
    },
  },
};
