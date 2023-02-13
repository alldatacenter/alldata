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

import routeInfoStore from 'core/stores/route';
import { createFlatStore } from 'core/cube';
import orgStore from 'app/org-home/stores/org';
import {
  getAddonList,
  getAddonDetail,
  getAddonReferences,
  deleteAddonIns,
  getExportAddonSpec,
  importCustomAddon,
} from '../services/addon';

interface IState {
  addonList: ADDON.Instance[];
  addonReferences: ADDON.Reference[];
  addonDetail: ADDON.Instance;
}

const initState: IState = {
  addonList: [],
  addonReferences: [],
  addonDetail: {
    config: {},
  } as ADDON.Instance,
};

const addon = createFlatStore({
  name: 'addon',
  state: initState,
  effects: {
    async getAddonList({ call, update }) {
      const routes = routeInfoStore.getState((s) => s.routes);
      const [rootRoute] = routes.slice(-2, routes.length - 1);
      const { path } = rootRoute;
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const addonList = await call(getAddonList, { path, orgId });
      update({ addonList });
    },
    async getAddonDetail({ call, update }, insId: string, temp?: boolean) {
      const addonDetail = await call(getAddonDetail, insId);
      !temp && update({ addonDetail }); // 临时获取，不存在store里
      return addonDetail;
    },
    async getAddonReferences({ call, update }, insId: string) {
      const addonReferences = await call(getAddonReferences, insId);
      update({ addonReferences });
    },
    async deleteAddonIns({ call }, insId: string) {
      return call(deleteAddonIns, insId);
    },
    async getExportAddonSpec({ call, getParams }) {
      const { projectId } = getParams();
      return call(getExportAddonSpec, projectId);
    },
    async importCustomAddon({ call, getParams }, body: Obj) {
      const { projectId } = getParams();
      return call(importCustomAddon, { projectId, body });
    },
  },
  reducers: {
    setAddonDetail(state, payload: ADDON.Instance) {
      state.addonDetail = payload;
    },
    clearAddonDetail(state) {
      state.addonDetail = {
        config: {},
      } as ADDON.Instance;
    },
    clearAddonReferences(state) {
      state.addonReferences = [];
    },
  },
});

export default addon;
