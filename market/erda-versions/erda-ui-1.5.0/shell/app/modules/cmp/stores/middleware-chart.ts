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

import { createStore } from 'core/cube';
import { getChartMeta, getContainerChart, IChartMeta, IChartDataQuery } from '../services/middleware-chart';

interface IState {
  containerChartMetas: IChartMeta[];
  middlewareChartMetas: IChartMeta[];
}

const initState: IState = {
  containerChartMetas: [],
  middlewareChartMetas: [],
};

const middlewareChart = createStore({
  name: 'middleware-chart',
  state: initState,
  effects: {
    async getChartMeta({ call, update }, payload: { type: string }) {
      const chartMetas = await call(getChartMeta, payload);
      if (payload.type === 'addon_container') {
        update({ containerChartMetas: chartMetas });
      } else {
        update({ middlewareChartMetas: chartMetas });
      }
    },
    async getContainerChart({ call }, payload: IChartDataQuery) {
      return call(getContainerChart, payload);
    },
  },
});

export default middlewareChart;
