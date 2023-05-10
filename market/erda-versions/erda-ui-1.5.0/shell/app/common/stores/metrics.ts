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
import { loadGatewayMetricItem, loadMetricItem, listMetricByResourceType } from 'common/services/metrics';
import { isEmpty, get, forEach, isArray } from 'lodash';
import { groupHandler, multipleDataHandler, multipleGroupDataHandler } from 'common/utils/chart-utils';

const initState: METRICS.IState = {
  listMetric: {},
  metricItem: {},
};

const metricsMonitorStore = createStore({
  name: 'metricsMonitor',
  state: initState,
  effects: {
    async listMetricByResourceType({ call }, payload: METRICS.ChartMetaQuerys) {
      const result = await call(listMetricByResourceType, payload);
      if (!isEmpty(result.metrics.data)) {
        metricsMonitorStore.reducers.listMetricByResourceTypeSuccess(result);
      }
    },
    async loadMetricItem({ call }, payload: METRICS.LoadMetricItemQuerys) {
      const metricItem = await call(loadMetricItem, payload);
      if (!isEmpty(metricItem)) {
        const { resourceType, chartQuery } = payload;
        metricsMonitorStore.reducers.loadMetricItemSuccess({ data: metricItem, query: chartQuery, resourceType });
      }
    },
    async loadGatewayMetricItem({ call }, payload: METRICS.GetGateway) {
      const metricItem = await call(loadGatewayMetricItem, payload);
      if (!isEmpty(metricItem)) {
        const { resourceType, chartQuery } = payload;
        metricsMonitorStore.reducers.loadMetricItemSuccess({ data: metricItem, query: chartQuery, resourceType });
      }
    },
  },
  reducers: {
    listMetricByResourceTypeSuccess(state, payload: METRICS.ChartMeta) {
      const { resourceType, metrics } = payload;
      const metricMap = {};
      metrics.data.forEach((metric: METRICS.Metric) => {
        metricMap[metric.name] = metric;
      });
      const listMetric = Object.assign({}, { ...state.listMetric }, { [resourceType]: metricMap });
      return { ...state, listMetric };
    },
    loadMetricItemSuccess(state, payload: METRICS.SetMetricItem) {
      const metricDataPair = {};
      const { data, query, resourceType } = payload;
      const keyEnum = ['avg', 'diff', 'diffps', 'max', 'min', 'sum', 'sumCps'];
      const dataKeys: string[] = [];
      forEach(keyEnum, (key) => {
        if (query[key]) {
          isArray(query[key])
            ? forEach(query[key], (subKey) => {
                dataKeys.push(`${key}.${subKey}`);
              })
            : dataKeys.push(`${key}.${query[key]}`);
        }
      });
      let getDataFun = groupHandler(dataKeys[0]); // 按照group取单维度数据
      if (resourceType === 'machine' && !query.group) {
        // 如果是机器监控，若无group查询则取多维度数据，
        // 后续addon监控是否需要遵循这一规则（无group查询取多维度）还不确定
        getDataFun = multipleDataHandler(dataKeys);
      }
      if (resourceType === 'multipleGroup') {
        // 网关需求多维分组
        getDataFun = multipleGroupDataHandler(dataKeys);
      }
      metricDataPair[data.id] = {
        loading: false,
        ...getDataFun(get(data, 'data.data') || {}),
      };

      const metricItem = Object.assign({}, { ...state.metricItem }, metricDataPair);
      state.metricItem = metricItem;
    },
    clearListMetrics(state) {
      state.listMetric = {};
      state.metricItem = {};
    },
  },
});

export default metricsMonitorStore;
