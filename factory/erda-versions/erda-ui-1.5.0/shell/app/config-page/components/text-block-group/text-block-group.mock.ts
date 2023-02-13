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

const mockData: CP_TEXT_BLOCK_GROUP.Spec = {
  type: 'TextBlockGroup',
  prop: {},
  data: {
    data: [
      [
        {
          main: '233',
          sub: '未完成',
        },
        {
          main: '233',
          sub: '已到期',
        },
        {
          main: '233',
          sub: '本日截止',
        },
        {
          main: '233',
          sub: '本周截止',
        },
      ],
      [
        {
          main: '23',
          sub: '本月截止',
        },
        {
          main: '5',
          sub: '未指定截止日期',
        },
      ],
    ],
  },
};

export default mockData;
