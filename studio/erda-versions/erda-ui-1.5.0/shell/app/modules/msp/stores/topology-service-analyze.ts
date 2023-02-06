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

import { createFlatStore } from 'core/cube';
import {
  getProcessDashboardId,
  getTraceSlowTranslation,
  getExceptionTypes,
  getInstanceIds,
} from '../services/topology-service-analyze';

interface IState {
  activedNode?: Partial<TOPOLOGY.INode>;
}

const initState: IState = {
  activedNode: undefined,
};
const topologyServiceStore = createFlatStore({
  name: 'topologyService',
  state: initState,
  effects: {
    async getProcessDashboardId({ call }, payload: TOPOLOGY_SERVICE_ANALYZE.CommonQuery) {
      const { dashboardId } = await call(getProcessDashboardId, payload);
      return dashboardId;
    },
    async getTraceSlowTranslation(
      { call },
      payload: Merge<
        TOPOLOGY_SERVICE_ANALYZE.CommonQuery,
        { operation: string; start: number; end: number; sort: TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE; limit: number }
      >,
    ) {
      const traceSlowTranslation = await call(getTraceSlowTranslation, payload);
      return traceSlowTranslation;
    },
    async getExceptionTypes(
      { call },
      payload: Merge<TOPOLOGY_SERVICE_ANALYZE.CommonQuery, TOPOLOGY_SERVICE_ANALYZE.TimestampQuery>,
    ) {
      const exceptionTypes = await call(getExceptionTypes, payload);
      return exceptionTypes;
    },
    async getInstanceIds(
      { call },
      payload: Merge<TOPOLOGY_SERVICE_ANALYZE.CommonQuery, TOPOLOGY_SERVICE_ANALYZE.TimestampQuery>,
    ) {
      const exceptionTypes = await call(getInstanceIds, payload);
      return exceptionTypes;
    },
  },
  reducers: {
    setActivedNode(state, activedNode?: Partial<TOPOLOGY.INode>) {
      state.activedNode = state.activedNode?.id === activedNode?.id ? undefined : activedNode;
    },
  },
});
export default topologyServiceStore;
