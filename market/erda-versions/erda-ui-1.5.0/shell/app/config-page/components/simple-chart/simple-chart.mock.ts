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

const mockData: CP_SIMPLE_CHART.Spec = {
  type: 'SimpleChart',
  data: {
    main: '2321',
    sub: '总数',
    compareText: '较昨日',
    compareValue: '+4',
    chart: {
      xAxis: ['2021-01-20', '2021-01-21', '2021-01-22', '2021-01-23', '2021-01-24', '2021-01-25', '2021-01-26'],
      series: [
        {
          name: '需求&任务总数',
          data: [820, 932, 901, 934, 10, 0, 0],
        },
      ],
    },
  },
};

export default mockData;
