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

export const enhanceMock = (mockData: any, payload: any) => {
  if (!payload.hierarchy) {
    return mockData;
  }
  return payload;
};

export const mockData = {
  scenario: {
    scenarioKey: 'project-list-my', // 后端定义
    scenarioType: 'project-list-my', // 后端定义
  },
  protocol: {
    hierarchy: {
      root: 'dataRank',
      structure: {
        myPage: ['dataRank'],
      },
    },
    components: {
      myPage: { type: 'Container' },
      dataRank: {
        type: 'TopN',
        data: {
          list: [
            {
              title: '吞吐量最大Top5',
              type: 'maximumThroughputTop5',
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
                  name: '服务名称b',
                  value: 80,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称c',
                  value: 77,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称d',
                  value: 50,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
                  value: 25,
                  percent: 100,
                  unit: 'reqs/s',
                },
              ],
            },
            {
              title: '吞吐量最小Top5',
              type: 'minimumThroughputTop5',
              span: 6,
              items: [
                {
                  name: '服务名称A',
                  value: 100,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称b',
                  value: 80,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称c',
                  value: 77,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称d',
                  value: 50,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
                  value: 25,
                  percent: 100,
                  unit: 'reqs/s',
                },
              ],
            },
            {
              title: '平均延迟Top5',
              type: 'averageDelayTop5',
              span: 6,
              items: [
                {
                  name: '服务名称A',
                  value: 100,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称b',
                  value: 80,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称c',
                  value: 77,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称d',
                  value: 50,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称很长的服务服务名称很长的服务服务名称很长的服务服务名称很长的服务',
                  value: 25,
                  percent: 100,
                  unit: 'reqs/s',
                },
              ],
            },
            {
              title: '错误率Top5',
              type: 'errorRateTop5',
              span: 6,
              items: [
                {
                  name: '服务名称A',
                  value: 100,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称b',
                  value: 80,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称c',
                  value: 77,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
                  name: '服务名称d',
                  value: 50,
                  percent: 100,
                  unit: 'reqs/s',
                },
                {
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
    },
  },
};
