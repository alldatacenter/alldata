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

const mockData: Array<MockSpec<CP_BADGE.Spec>> = [
  {
    _meta: {
      title: '标准标签',
      desc: '',
    },
    type: 'Badge',
    props: {
      status: 'success' as CP_BADGE.Status,
      text: 'doing',
      color: 'green',
      tip: 'tip message',
      size: 'default',
      breathing: true,
    },
    data: {
      list: [],
    },
  },
  {
    _meta: {
      title: '小号标签',
      desc: '一般用在 table 标题列的第二行',
    },
    type: 'Badge',
    props: {
      status: 'warning' as CP_BADGE.Status,
      text: 'doing',
      color: 'yellow',
      tip: 'tip message',
      size: 'small',
      breathing: true,
    },
    data: {
      list: [],
    },
  },
];

export default mockData;
