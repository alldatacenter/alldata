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

import { get } from 'lodash';
import { createStore } from 'core/cube';
import {
  getAiCapacityData,
  getBiCapacityAjaxErr,
  getBiCapacityAjaxInfo,
  getBiCapacityApdex,
} from '../services/monitor-overview';

interface IState {
  aiCapacityData: MONITOR_OVERVIEW.IAIData;
  biCapacityData: MONITOR_OVERVIEW.IBIData;
}

const initState: IState = {
  aiCapacityData: {},
  biCapacityData: {},
} as IState;

const monitorOverview = createStore({
  name: 'monitorOverview',
  state: initState,
  effects: {
    async getAiCapacityData({ call, update, getParams }, payload: MONITOR_OVERVIEW.IChartQuery) {
      const { terminusKey } = getParams();
      const data = await call(getAiCapacityData, { ...payload, filter_target_terminus_key: terminusKey });
      const curData = get(data, 'results[0].data[0]') || {};
      const aiCapacityData = {
        webAvg: get(curData, '["avg.elapsed_mean"].data') || 0,
        webCpm: get(curData, '["sumCpm.elapsed_count"].data') || 0,
      };
      update({ aiCapacityData });
    },
    async getBiCapacityApdex({ call, getParams }, payload: MONITOR_OVERVIEW.IChartQuery) {
      const { terminusKey } = getParams();
      const res = await call(getBiCapacityApdex, { ...payload, filter_tk: terminusKey });
      const apdex = get(res, 'results[0].data[0]["apdex.plt"].data') || 0;
      monitorOverview.reducers.getBiCapacityDataSuccess({ apdex });
    },
    async getBiCapacityAjaxErr({ call, getParams }, payload: MONITOR_OVERVIEW.IChartQuery) {
      const { terminusKey } = getParams();
      const res = await call(getBiCapacityAjaxErr, { ...payload, filter_tk: terminusKey });
      const ajaxErr = get(res, 'results[0].data[0]["range.status"].data[1].percent') || 0;
      monitorOverview.reducers.getBiCapacityDataSuccess({ ajaxErr });
    },
    async getBiCapacityAjaxInfo({ call, getParams }, payload: MONITOR_OVERVIEW.IChartQuery) {
      const { terminusKey } = getParams();
      const res = await call(getBiCapacityAjaxInfo, { ...payload, filter_tk: terminusKey });
      const data = get(res, 'results[0].data[0]') || {};
      const ajaxInfo = {
        ajaxResp: get(data, '["avg.tt"].data') || 0,
        ajaxCpm: get(data, '["cpm.tt"].data') || 0,
      };
      monitorOverview.reducers.getBiCapacityDataSuccess({ ...ajaxInfo });
    },
  },
  reducers: {
    getBiCapacityDataSuccess(state, payload) {
      state.biCapacityData = { ...state.biCapacityData, ...payload };
    },
  },
});

export default monitorOverview;
