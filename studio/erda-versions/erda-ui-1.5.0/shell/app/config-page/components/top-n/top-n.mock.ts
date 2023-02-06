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

import { functionalColor } from 'common/constants';

const mockData: Array<MockSpec<CP_DATA_RANK.Spec>> = [
  {
    _meta: {
      title: 'Data Rank',
      desc: '数据 Top N',
    },
    type: 'TopN',
    props: {
      theme: [
        {
          color: functionalColor.actions,
          titleIcon: 'mail',
          backgroundIcon: 'map-draw',
        },
        {
          color: functionalColor.success,
          titleIcon: 'mysql',
          backgroundIcon: 'shezhi',
        },
        {
          color: functionalColor.warning,
          titleIcon: 'RocketMQ',
          backgroundIcon: 'map-draw',
        },
        {
          color: functionalColor.error,
          titleIcon: 'morenzhongjianjian',
          backgroundIcon: 'data-server',
        },
      ],
    },
    data: {
      list: [
        {
          title: '吞吐量最大Top5',
          span: 6,
          items: [
            {
              id: '', // serviceId
              name: '服务名称A', // serviceName
              value: 300,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称b',
              value: 80,
              percent: 80,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称c',
              value: 77,
              percent: 70,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称d',
              value: 50,
              percent: 40,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
              value: 25,
              percent: 10,
              unit: 'reqs/s',
            },
          ],
        },
        {
          title: '吞吐量最小Top5',
          span: 6,
          items: [
            {
              id: '', // serviceId
              name: '服务名称A',
              value: 100,
              percent: 11,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称b',
              value: 80,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称c',
              value: 77,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称d',
              value: 50,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
              value: 25,
              percent: 100,
              unit: 'reqs/s',
            },
          ],
        },
        {
          title: '平均延迟Top5',
          span: 6,
          items: [
            {
              id: '', // serviceId
              name: '服务名称A',
              value: 100,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称b',
              value: 80,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称c',
              value: 77,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称d',
              value: 50,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
              value: 25,
              percent: 100,
              unit: 'reqs/s',
            },
          ],
        },
        {
          title: '错误率Top5',
          span: 6,
          items: [
            {
              id: '', // serviceId
              name: '服务名称A',
              value: 100,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称b',
              value: 80,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称c',
              value: 77,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称d',
              value: 50,
              percent: 100,
              unit: 'reqs/s',
            },
            {
              id: '', // serviceId
              name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
              value: 25,
              percent: 100,
              unit: 'reqs/s',
            },
          ],
        },
      ],
    },
  },
];

export default mockData;
