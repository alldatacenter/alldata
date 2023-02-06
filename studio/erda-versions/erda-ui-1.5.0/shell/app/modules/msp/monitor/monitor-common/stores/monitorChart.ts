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
import { get } from 'lodash';
import { loadChart } from '../services/monitorChart';

const initState = {} as any;
const defaultDataHandler = (dt: any) => dt;
interface IChartQuery {
  chartName: string;
  moduleName: string;
  query: {
    [pro: string]: any;
    extendHandler: object;
    fetchApi: string;
  };
  dataHandler: Function | undefined;
}

interface ILoadSucProps {
  origData: IChartResult;
  moduleName: string;
  chartName: string;
  dataHandler: Function | undefined;
  query: object;
  extendHandler: any;
}

const monitorChart = createStore({
  name: 'monitor-chart',
  state: initState,
  effects: {
    async loadChart({ call }, payload: IChartQuery) {
      const { moduleName, chartName, query, dataHandler } = payload;
      monitorChart.reducers.startLoadingChart({ moduleName, chartName });
      const { extendHandler, ...restQuery } = query;
      const origData = await call(loadChart, { ...restQuery });
      monitorChart.reducers.loadChartSuccess({
        origData,
        moduleName,
        chartName,
        dataHandler,
        query: restQuery,
        extendHandler,
      });
    },
  },
  reducers: {
    startLoadingChart(state, { moduleName, chartName }: { moduleName: string; chartName: string }) {
      const curModule = state[moduleName] || {};
      if (curModule[chartName]) {
        // 使用内部loading而不是isFetching.effects下的，可以减少外部props从而减少一次render
        curModule[chartName] = { ...(curModule[chartName] || {}), loading: true };
      } else {
        // 首次加载时loading设为null, 避免闪过 ’没有数据‘
        curModule[chartName] = { results: [] };
      }
      state[moduleName] = curModule;
    },
    loadChartSuccess(
      state,
      { origData, moduleName, chartName, dataHandler = defaultDataHandler, ...rest }: ILoadSucProps,
    ) {
      let data = {};
      try {
        data = dataHandler(origData, {
          moduleName,
          chartName,
          prevData: get(state, `${moduleName}.${chartName}`),
          ...rest,
        });
      } catch (e) {
        // eslint-disable-next-line no-console
        console.error(`data handler error on ${moduleName}.${chartName}`, e);
        data = {};
      }
      const curModule = {
        ...state[moduleName],
        [chartName]: {
          loading: false,
          ...data,
        },
      };
      state[moduleName] = curModule;
    },
    clearChartData(state, { moduleName, chartName }) {
      if (moduleName) {
        if (chartName) {
          const curModule = state[moduleName] || {};
          state[moduleName] = {
            ...curModule,
            [chartName]: {},
          };
        } else {
          state[moduleName] = {};
        }
      }
    },
  },
});
export default monitorChart;
