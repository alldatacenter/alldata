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

/* eslint-disable no-unused-vars */
/* eslint-disable @typescript-eslint/no-unused-vars */
// 里面有注册图表库用的东西，所以放在这里引入注册
import './utils';

export { default as Echarts } from './components/echarts';
export { default as BarChart } from './components/bar-chart';
export { default as MapChart } from './components/map-chart';
export { default as PieChart } from './components/pie-chart';
export { default as HollowPieChart } from './components/hollow-pie-chart';
export { default as MonitorChartNew } from './components/monitor-chart-new';
// export { default as getFormatter } from './components/monitor-chart-formatter';
export { default as ChartRender } from './components/chart-render';
