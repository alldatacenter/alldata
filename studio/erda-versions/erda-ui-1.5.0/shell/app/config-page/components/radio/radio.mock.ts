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

const mockData: CP_RADIO.Spec = {
  type: 'Radio',
  props: {
    options: [
      {
        key: 'my',
        text: '我的组织',
        operations: {
          click: {
            reload: false,
            key: 'myOrg',
            command: {
              key: 'goto',
              target: 'orgList',
            },
          },
        },
      },
      {
        key: 'all',
        text: '公开组织',
        operations: {
          click: {
            reload: false,
            key: 'allOrg',
            command: {
              key: 'goto',
              target: 'orgList',
            },
          },
        },
      },
    ],
    radioType: 'button',
    size: 'small',
  },
  state: {
    value: 'my',
  },
};

export default mockData;
