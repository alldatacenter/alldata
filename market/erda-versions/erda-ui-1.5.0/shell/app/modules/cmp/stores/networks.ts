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
import { getVpcList, addVpc, getVswList, addVsw, getCloudZone } from '../services/networks';
import i18n from 'i18n';

interface IState {
  vpcList: NETWORKS.ICloudVpc[];
  vswList: NETWORKS.ICloudVsw[];
}

const initState: IState = {
  vpcList: [],
  vswList: [],
};

const networks = createStore({
  name: 'cloudNetworks',
  state: initState,
  effects: {
    async getVpcList({ call, update }, payload?: Merge<IPagingReq, NETWORKS.ICloudVpcQuery>) {
      const { list: vpcList } = await call(getVpcList, payload);
      update({ vpcList: vpcList || [] });
    },
    async addVpc({ call }, payload: NETWORKS.IVpcCreateBody) {
      const res = await call(addVpc, payload);
      return res;
    },
    async getVswList({ call, update }, payload: Merge<IPagingReq, NETWORKS.ICloudVswQuery>) {
      const { list: vswList } = await call(getVswList, payload);
      update({ vswList: vswList || [] });
    },
    async addVsw({ call }, payload: NETWORKS.IVswCreateBody) {
      const res = await call(addVsw, payload, { successMsg: i18n.t('added successfully'), fullResult: true });
      return res;
    },
    async getCloudZone({ call }, payload: NETWORKS.ICloudZoneQuery) {
      const res = await call(getCloudZone, payload);
      return res;
    },
  },
  reducers: {
    resetState() {
      return initState;
    },
    clearVpcList(state) {
      state.vpcList = [];
    },
  },
});

export default networks;
