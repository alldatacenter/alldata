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

const mockData: CP_TABLE.Spec = {
  type: 'Table',
  state: {
    total: 20,
    pageSize: 10,
    pageNo: 1, // 这里注意，如果filter组件里有数据变化，这里的pageNo要重置为1，就是用户改变查询参数后，要从第一页开始
  },
  operations: {
    // 当用户翻页的时候，我会先把上面state的pageNo改掉，然后再告诉你我执行了这个operation
    changePageNo: {
      key: 'changePageNo',
      reload: true,
    },
  },
  props: {
    rowKey: 'id',
    columns: [
      { title: '标题', dataIndex: 'title' },
      { title: '进度', dataIndex: 'progress', width: 100 },
      { title: '严重程度', dataIndex: 'severity', width: 100 },
      { title: '优先级', dataIndex: 'priority', width: 120 },
      { title: '状态', dataIndex: 'state', width: 120 },
      { title: '处理人', dataIndex: 'assignee', width: 120 },
      { title: '截止日期', dataIndex: 'deadline', width: 160 },
    ],
  },
  data: {
    list: [
      {
        id: '1111', // 唯一key
        type: 'REQUIREMENT',
        iterationID: 9,
        title: {
          renderType: 'textWithTags',
          prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
          value: '111111',
          tags: [
            { tag: 'tag1', color: 'red' },
            { tag: 'tag2', color: 'blue' },
            { tag: 'tag3', color: 'green' },
          ],
        },
        progress: { renderType: 'progress', value: '30' },
        severity: {
          renderType: 'operationsDropdownMenu',
          value: '严重',
          prefixIcon: 'ISSUE_ICON.severity.XX',
          operations: {
            changePriorityTo1: {
              // 这个key后端定，
              key: 'changePriorityTo1', // 这个key一定要有
              reload: true,
              disabled: true, // 根据实际情况
              text: '严重',
              prefixIcon: 'ISSUE_ICON.severity.HIGH',
              meta: { id: '事件id:1111', severity: '1' },
            },
          },
        },
        priority: {
          renderType: 'operationsDropdownMenu',
          value: '高',
          prefixIcon: 'ISSUE_ICON.priority.HIGH',
          operations: {
            changePriorityTo1: {
              // 这个key后端定，
              key: 'changePriorityTo1', // 这个key一定要有
              reload: true,
              disabled: true, // 根据实际情况
              text: '高',
              prefixIcon: 'ISSUE_ICON.priority.HIGH',
              meta: { id: '事件id:1111', priority: '1' },
            },
            changePriorityTo2: {
              key: 'changePriorityTo2', // 这个key一定要有
              reload: true,
              text: '中',
              prefixIcon: 'ISSUE_ICON.priority.NORMAL',
              meta: { id: '事件id:1111', priority: '' },
            },
          },
        },
        state: {
          renderType: 'operationsDropdownMenu',
          value: '进行中',
          prefixIcon: 'ISSUE_ICON.state.WORKING',
          operations: {
            changePriorityTo1: {
              // 这个key后端定，
              key: 'changePriorityTo1', // 这个key一定要有
              reload: true,
              disabled: true, // 根据实际情况
              text: '待处理',
              prefixIcon: 'ISSUE_ICON.state.OPEN',
              meta: { id: '事件id:1111', state: 'z' },
            },
            changePriorityTo2: {
              key: 'changePriorityTo2', // 这个key一定要有
              reload: true,
              text: '进行中',
              prefixIcon: 'ISSUE_ICON.state.WORKING',
              meta: { id: '事件id:1111', priority: '2' },
            },
            changePriorityTo3: {
              key: 'changePriorityTo3', // 这个key一定要有
              reload: true,
              text: '已完成',
              prefixIcon: 'ISSUE_ICON.state.DONE',
              meta: { id: '事件id:1111', priority: '3' },
            },
          },
        },
        assignee: {
          value: 'xx',
          renderType: 'userAvatar',
        },
      },
    ],
  },
};

export default mockData;
