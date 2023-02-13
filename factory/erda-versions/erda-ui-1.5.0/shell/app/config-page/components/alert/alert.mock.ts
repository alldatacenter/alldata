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

const mockData: Array<MockSpec<CP_ALERT.Spec>> = [
  {
    _meta: {
      title: '提示',
      desc: 'type 类型有 success | info | warning | error',
    },
    type: 'Alert',
    props: {
      message: 'There is success message without icon',
      type: 'info',
    },
  },
  {
    _meta: {
      title: '成功',
    },
    type: 'Alert',
    props: {
      message: 'There is success message',
      type: 'success',
    },
  },
  {
    _meta: {
      title: '警告',
    },
    type: 'Alert',
    props: {
      message: 'There is warning message',
      type: 'warning',
    },
  },
  {
    _meta: {
      title: '失败',
    },
    type: 'Alert',
    props: {
      visible: true,
      showIcon: true,
      message: 'There is alert message',
      type: 'error',
    },
  },
];

export default mockData;
