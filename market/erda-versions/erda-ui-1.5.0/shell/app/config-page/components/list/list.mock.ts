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

const mockData: CP_LIST.Spec = {
  type: 'List',
  props: {
    pageNo: 1,
    pageSize: 10,
    total: 100,
    pageSizeOptions: ['10', '20', '50', '100'],
  },
  operations: {
    changePageNo: {
      key: 'changePageNo',
      reload: true,
      fillMeta: 'pageNo',
    },
    changePageSize: {
      key: 'changePageSize',
      reload: true,
      fillMeta: 'pageSize',
    },
  },
  data: {
    list: [
      {
        id: '1',
        title: '测试1',
        description: '测试测试测试测试',
        prefixImg: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
        extraInfos: [
          { icon: 'help', text: '1234', tooltip: '提示信息' },

          { icon: 'help', text: '33333', tooltip: '提示信息' },
        ],
        operations: {
          click: {
            key: 'click',
            show: false,
            reload: false,
          },
          toManage: {
            key: 'toManage',
            text: '管理',
            reload: false,
            command: {
              key: 'goto',
              target: 'https://dice.terminus.io',
              jumpOut: false,
            },
          },
          exist: {
            key: 'exist',
            text: '退出',
            reload: true,
            confirm: '是否确定退出？',
            meta: { id: '1' },
          },
        },
      },
      {
        id: '2',
        title: '测试2',
        description: '测试测试',
        prefixImg: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
        extraInfos: [{ icon: 'help', text: '1234', tooltip: '提示信息' }],
      },
    ],
  },
};

export default mockData;
