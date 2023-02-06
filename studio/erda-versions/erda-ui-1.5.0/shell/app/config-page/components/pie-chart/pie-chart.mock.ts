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

const mockData: Array<MockSpec<CP_PIE_CHART.Spec>> = [
  {
    _meta: {
      title: '单个',
    },
    type: 'PieChart',
    name: 'cpuChart',
    props: {},
    state: {},
    data: {
      data: [
        {
          color: 'primary8',
          formatter: '92.7 Core',
          name: '已分配',
          value: 92.76,
        },
        {
          color: 'primary5',
          formatter: '142.4 Core',
          name: '剩余分配',
          value: 142.44,
        },
        {
          color: 'primary2',
          formatter: '4.8 Core',
          name: '不可分配',
          value: 4.8,
        },
      ],
      label: 'CPU',
    },
    operations: {},
  },
  // {
  //   _meta: {
  //     title: '多行',
  //     desc: '没有 data 时才会使用 group 数据',
  //   },
  //   type: 'PieChart',
  //   props: {
  //     option: {},
  //     // style: Obj;
  //     // chartStyle?: Obj;
  //     // direction?: 'col' | 'row';
  //     // size?: 'small' | 'normal' | 'big';
  //     // pureChart?: boolean;
  //     // visible?: boolean;
  //     title: 'ttt',
  //     // tip?: string | string[];
  //   },
  //   data: {
  //     label: 'label',
  //     group: [
  //       {
  //         "color": "primary8",
  //         "formatter": "92.7 Core",
  //         "name": "已分配",
  //         "value": 92.76
  //       },
  //       {
  //         "color": "primary5",
  //         "formatter": "142.4 Core",
  //         "name": "剩余分配",
  //         "value": 142.44
  //       },
  //       {
  //         "color": "primary2",
  //         "formatter": "4.8 Core",
  //         "name": "不可分配",
  //         "value": 4.8
  //       }
  //     ]
  //   },
  // },
];

export default mockData;
