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
import { getErrorTrend, getErrorChart, getErrorList, getErrorDetail, getAllVersion } from '../services/error-report';
import { get } from 'lodash';

interface IState {
  errorList: PUBLISHER.ErrorItem[];
  errorDetail: PUBLISHER.ErrorDetail | null;
}
const initState: IState = {
  errorList: [],
  errorDetail: null,
};

const errorReport = createStore({
  name: 'publisher-error-report',
  state: initState,
  effects: {
    async getErrorTrend({ call }, payload: Merge<{ publisherItemId: string }, PUBLISHER.MonitorKey>) {
      const errorTrend = await call(getErrorTrend, payload);
      return errorTrend;
    },
    async getErrorChart({ call }, payload: PUBLISHER.IChartQuery) {
      const chart = await call(getErrorChart, payload);
      return chart;
    },
    async getErrorList({ call, update }, payload: PUBLISHER.ErrorListQuery) {
      const errorList = await call(getErrorList, payload);
      update({ errorList });
    },
    async getErrorDetail({ call, update }, payload: PUBLISHER.ErrorDetailQuery) {
      const res = await call(getErrorDetail, payload);
      const errorDetail = get(res, 'results.0.data.0');
      update({ errorDetail: errorDetail || null });
    },
    async getAllVersion({ call }, payload: PUBLISHER.AllVersionQuery) {
      const data = await call(getAllVersion, payload);
      return data;
    },
  },
  reducers: {
    clearErrorList(state) {
      state.errorList = [];
    },
    clearErrorDetail(state) {
      state.errorDetail = null;
    },
  },
});

export default errorReport;
