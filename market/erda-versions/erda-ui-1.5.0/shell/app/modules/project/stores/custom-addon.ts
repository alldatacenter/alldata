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
import i18n from 'i18n';
import {
  getCloudInstances,
  addCustomAddonIns,
  updateCustomAddonConfig,
  getAddonsByCategory,
  getCloudGateway,
  addDiceAddonIns,
  addTenantAddonIns,
} from '../services/addon';
import userStore from 'user/stores';
import orgStore from 'app/org-home/stores/org';
import projectStore from 'project/stores/project';

const inCmpPage = () => window.location.pathname.includes('cmp/');

interface IState {
  addonList: CUSTOM_ADDON.Item[];
}

const initState: IState = {
  addonList: [],
};

const customAddon = createFlatStore({
  name: 'customAddon',
  state: initState,
  effects: {
    async getCloudInstances({ call, getParams }, query: Omit<CUSTOM_ADDON.InsQuery, 'projectID'>) {
      const { projectId } = getParams();
      return call(getCloudInstances, { ...query, projectID: projectId });
    },
    async addCustomAddonIns({ call, getParams }, query: Omit<CUSTOM_ADDON.AddBody, 'projectId'>) {
      const { projectId } = getParams();
      return call(
        addCustomAddonIns,
        { ...query, projectId: +projectId },
        { successMsg: i18n.t('dop:The creation of the service takes a while, please wait a moment') },
      );
    },

    async addDiceAddonIns({ call, getParams }, payload: Omit<CUSTOM_ADDON.AddDiceAddOns, 'projectId' | 'clusterName'>) {
      const { projectId } = getParams();
      const clusterConfig = projectStore.getState((s) => s.info.clusterConfig);
      return call(
        addDiceAddonIns,
        { ...payload, projectId: +projectId, clusterName: clusterConfig[payload.workspace] },
        { successMsg: i18n.t('dop:The creation of the service takes a while, please wait a moment') },
      );
    },
    async addTenantAddonIns({ call }, payload: CUSTOM_ADDON.AddTenantAddon) {
      return call(addTenantAddonIns, payload, {
        successMsg: i18n.t('dop:The creation of the service takes a while, please wait a moment'),
      });
    },
    async updateCustomAddonConfig({ call }, query: Omit<CUSTOM_ADDON.UpdateBody, 'operatorId'>) {
      const { id: operatorId } = userStore.getState((s) => s.loginUser);
      return call(
        updateCustomAddonConfig,
        { ...query, operatorId: +operatorId },
        { successMsg: i18n.t('updated successfully') },
      );
    },
    async getAddonsList({ call, update, getParams }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId } = getParams();
      const _params: CUSTOM_ADDON.QueryCustoms = { org_id: orgId };
      if (!inCmpPage()) {
        _params.project_id = projectId;
      }
      const addonList = await call(getAddonsByCategory, _params);
      if (addonList !== null) {
        update({ addonList });
      }
    },
    async getCloudGateway({ call, getParams }, payload: Omit<CUSTOM_ADDON.QueryCloudGateway, 'projectID'>) {
      const { projectId } = getParams();
      return call(getCloudGateway, { ...payload, projectID: projectId });
    },
  },
  reducers: {},
});

export default customAddon;
