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
import { getErrorsList, getEventIds, getEventDetail } from '../services/errors';

interface IState {
  filters: Obj<string>;
  errors: MONITOR_ERROR.IError[];
  eventIds: string[];
  eventDetail: MONITOR_ERROR.IEventDetail;
}

const initState: IState = {
  filters: {},
  errors: [],
  eventIds: [],
  eventDetail: {} as MONITOR_ERROR.IEventDetail,
};

const error = createStore({
  name: 'monitorErrors',
  state: initState,
  effects: {
    async getErrorsList({ call, update }, payload: MONITOR_ERROR.IErrorQuery) {
      const errors = await call(getErrorsList, { ...payload });
      update({ errors });
    },
    async getEventIds({ call, update, getParams }) {
      const { errorType, errorId, terminusKey } = getParams();
      const eventIds = await call(getEventIds, { id: errorId, errorType, terminusKey });
      update({ eventIds });
    },
    async getEventDetail({ call, update, getParams }, payload: { id: string }) {
      const { terminusKey } = getParams();
      const eventDetail = await call(getEventDetail, { exceptionEventId: payload.id, scopeId: terminusKey });
      update({ eventDetail });
    },
  },
  reducers: {
    clearMonitorErrors(state) {
      return { ...state, errors: [] };
    },
    setFilters(state, payload) {
      return { ...state, filters: payload };
    },
    clearFilters(state) {
      return { ...state, filters: {} };
    },
    clearEventDetail(state) {
      return { ...state, eventIds: [], eventDetail: {} };
    },
  },
});
export default error;
