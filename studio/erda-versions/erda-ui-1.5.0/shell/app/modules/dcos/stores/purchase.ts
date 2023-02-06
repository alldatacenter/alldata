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
import * as PurchaseServices from 'dcos/services/purchase';
import i18n from 'i18n';

interface IState {
  purchaseList: PURCHASE.PurchaseItem[];
  availableRegions: PURCHASE.Region[];
  availableZones: PURCHASE.Zone[];
}

const initState: IState = {
  purchaseList: [],
  availableRegions: [],
  availableZones: [],
};

const purchase = createStore({
  name: 'pruchase',
  state: initState,
  effects: {
    async addResource({ call, getParams }, payload: PURCHASE.AddResource) {
      const params = getParams();
      const result = await call(
        PurchaseServices.addResource,
        { ...payload, clusterName: params.clusterName },
        { successMsg: i18n.t('cmp:source added successfully') },
      );
      return result;
    },
    async getPurchaseList({ call, update, getParams }) {
      const params = getParams();
      const purchaseList = await call(PurchaseServices.getPurchaseList, { name: params.clusterName });
      update({ purchaseList });
    },
    async getAvailableRegions({ call, update }, payload: Omit<PURCHASE.QueryZone, 'regionId'>) {
      purchase.reducers.clearAvailableZones();
      const availableRegions = await call(PurchaseServices.getAvailableRegions, payload);
      update({ availableRegions });
    },
    async getAvailableZones({ call, update }, payload: PURCHASE.QueryZone) {
      const availableZones = await call(PurchaseServices.getAvailableZones, payload);
      update({ availableZones });
    },
    async checkEcsAvailable({ call }, payload: PURCHASE.CheckEcs) {
      const res = await call(PurchaseServices.checkEcsAvailable, payload);
      return res;
    },
    async addPhysicalCluster({ call }, payload: PURCHASE.AddPhysicalCluster) {
      const res = await call(PurchaseServices.addPhysicalCluster, payload);
      return res;
    },
  },
  reducers: {
    clearPurchaseList(state) {
      state.purchaseList = [];
    },
    clearAvailableZones(state) {
      state.availableZones = [];
    },
  },
});

export default purchase;
