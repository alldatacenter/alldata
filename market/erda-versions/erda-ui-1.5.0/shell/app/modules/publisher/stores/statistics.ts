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
import {
  getStatisticsTrend,
  getStatisticsChart,
  getStatisticsPieChart,
  getAllGroup,
  getTopLineChart,
  getVersionStatistics,
} from '../services/statistics';

interface IState {
  versionStatisticList: PUBLISHER.VersionStatistic[];
}

const initState: IState = {
  versionStatisticList: [],
};

const statistics = createStore({
  name: 'publisher-statistics',
  state: initState,
  effects: {
    async getStatisticsTrend({ call }, payload: { publisherItemId: string }) {
      const totalTrend = await call(getStatisticsTrend, payload);
      return totalTrend;
    },
    async getStatisticsChart({ call }, payload: PUBLISHER.IChartQuery) {
      const chart = await call(getStatisticsChart, payload);
      return chart;
    },
    async getStatisticsPieChart({ call }, payload: PUBLISHER.IChartQuery) {
      const chart = await call(getStatisticsPieChart, payload);
      return chart;
    },
    async getVersionStatistics({ call, update }, payload: PUBLISHER.VersionStatisticQuery) {
      const versionStatisticList = await call(getVersionStatistics, payload);
      update({ versionStatisticList });
    },
    async getAllGroup({ call }, payload: PUBLISHER.IChartQuery) {
      const data = await call(getAllGroup, payload);
      return data;
    },
    async getTopLineChart({ call }, payload: PUBLISHER.IChartQuery) {
      const data = await call(getTopLineChart, payload);
      return data;
    },
  },
  reducers: {
    clearVersionStatistic(state) {
      state.versionStatisticList = [];
    },
  },
});

export default statistics;
