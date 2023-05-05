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

const mockData: CP_SORT_GROUP.Spec = {
  type: 'SortGroup',
  state: {
    dragParams: {
      dragKey: 1, // 拖拽的接点 id
      dragGroupId: 1,
      dropKey: 2, // 放置到节点 id
      dropGroupId: 2,
      position: 1, // 放置节点的上: -1, 下: 1
    },
  },
  props: {
    // draggable: false,
    // groupDraggable: false,
  },
  operations: {
    moveItem: {
      key: 'moveItem',
      reload: true,
    },
    moveGroup: {
      key: 'moveGroup',
      reload: true,
    },
    clickItem: {
      key: 'clickItem',
      reload: true,
      fillMeta: 'data',
      meta: { data: '' },
    },
  },
  data: {
    type: 'sort-item',
    value: [
      {
        id: 1,
        groupId: 11, // 即stageId
        title: '1',
        operations: {
          add: {
            icon: 'add',
            key: 'addParallelAPI',
            hoverTip: '添加并行接口',
            disabled: true,
            disabledTip: '',
            reload: true,
            hoverShow: true,
            meta: { id: 1 },

            // command: [
            //   { key: 'set', state: { visible: true }, target: '滑窗1' },
            //   { key: 'set', state: { formData: { clickId: 1, position: 1 } }, target: '表单1' },
            // ],
          },
          split: {
            icon: 'split',
            key: 'standalone',
            hoverTip: '改为串行',
            disabled: false,
            reload: true,
            disabledTip: '',
            meta: { id: 1 },
          },
          delete: {
            icon: 'shanchu',
            key: 'deleteAPI',
            disabled: false,
            reload: true,
            disabledTip: '',
            meta: { id: 1 },
          },
        },
      },
      {
        id: 2,
        groupId: 11,
        title: '2',
        operations: {
          add: {
            icon: 'add',
            key: 'addParallelAPI',
            hoverTip: '添加并行接口',
            disabled: false,
            disabledTip: '',
            reload: true,
            hoverShow: true,
            meta: { id: 1 },
          },
          delete: {
            icon: 'shanchu',
            key: 'deleteAPI',
            disabled: false,
            reload: true,
            disabledTip: '',
            meta: { id: 2 },
          },
        },
      },
      {
        id: 3,
        groupId: 2,
        title: '3',
        operations: {},
      },
      {
        id: 4,
        groupId: 2,
        title: '4',
        operations: {},
      },
      {
        id: 5,
        groupId: 3,
        title: '5',
        operations: {},
      },
    ],
  },
};

export default mockData;
