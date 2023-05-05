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
import { getMonitorTopology, getTagsOptions, getTopologyTags } from '../services/topology';
import { getCustomDashboardDetail } from 'cmp/services/custom-dashboard';

interface IState {
  topologyData: TOPOLOGY.ITopologyResp;
  isSimpleNode: boolean;
  scale: number;
  topologyTags: TOPOLOGY.ISingleTopologyTags[] | [];
  tagOptionsCollection: object;
  topologySize: {
    containerWidth: number;
    containerHeight: number;
  };
}

const initState = {
  topologyData: {},
  isSimpleNode: false,
  scale: 0.8,
  topologyTags: [],
  tagOptionsCollection: {},
  topologySize: {
    containerWidth: 0,
    containerHeight: 0,
  },
} as IState;

const topology = createStore({
  name: 'monitorTopology',
  state: initState,
  effects: {
    async getMonitorTopology({ call, update }, payload: TOPOLOGY.ITopologyQuery) {
      const topologyData = await call(getMonitorTopology, payload);
      update({ topologyData });
    },
    async getTopologyTags({ call, update }, payload: TOPOLOGY.ITopologyTagsQuery) {
      const topologyTags = await call(getTopologyTags, payload);
      update({ topologyTags });
      return topologyTags;
    },
    async getTagsOptions({ call, update, select }, payload: TOPOLOGY.ITopologyTagOptionQuery) {
      const tagOptions = await call(getTagsOptions, payload);
      const tagOptionsCollection = select((s) => s.tagOptionsCollection);
      update({ tagOptionsCollection: { ...tagOptionsCollection, [payload.tag]: tagOptions } });
      return tagOptions;
    },
    async getCustomDashboardDetail({ call }, payload: { id: string; scopeId?: string }) {
      const customDashboardDetail = await call(getCustomDashboardDetail, { ...payload, isSystem: true });
      return customDashboardDetail?.viewConfig;
    },
  },
  reducers: {
    clearMonitorTopology(state) {
      state.topologyData = {} as TOPOLOGY.ITopologyResp;
    },
    switchNodeMode(state) {
      state.isSimpleNode = !state.isSimpleNode;
    },
    setScale(state, scale) {
      state.scale = scale;
    },
    setTopologySize(state, containerWidth, containerHeight) {
      state.topologySize = {
        containerHeight,
        containerWidth,
      };
    },
  },
});

export default topology;
