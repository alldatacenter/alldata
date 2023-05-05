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

/* eslint-disable no-param-reassign */
import { createFlatStore } from 'core/cube';
import i18n from 'i18n';
import {
  createTestEnv,
  updateTestEnv,
  deleteTestEnv,
  getTestEnvList,
  getAutoTestEnvList,
  createAutoTestEnv,
  updateAutoTestEnv,
  deleteAutoTestEnv,
} from '../services/test-env';

interface IState {
  active: TEST_ENV.Item | Obj;
  envList: TEST_ENV.Item[];
  autoEnvList: TEST_ENV.IAutoEnvItem[];
  envInfo: TEST_ENV.EnvListQuery;
  projectEnvList: TEST_ENV.Item[];
}

const initState: IState = {
  active: {},
  envList: [],
  autoEnvList: [],
  projectEnvList: [],
  envInfo: {
    envID: 0,
    envType: '' as TEST_ENV.EnvType,
  },
};

const testEnv = createFlatStore({
  name: 'testEnv',
  state: initState,
  effects: {
    async createTestEnv({ call }, payload: TEST_ENV.CreateBody, query: TEST_ENV.EnvListQuery) {
      const newData = await call(createTestEnv, payload, {
        successMsg: i18n.t('dop:environment configuration added successfully'),
      });
      testEnv.getTestEnvList(query);
      testEnv.setActiveEnv(newData);
    },
    async createAutoTestEnv({ call }, payload: TEST_ENV.ICreateAutoTestEnv, query: TEST_ENV.IAutoEnvQuery) {
      const newData = await call(createAutoTestEnv, payload, {
        successMsg: i18n.t('dop:environment configuration added successfully'),
      });
      testEnv.getAutoTestEnvList(query);
      testEnv.setActiveEnv(newData);
    },
    async updateTestEnv({ call }, payload: TEST_ENV.Item, query: TEST_ENV.EnvListQuery) {
      await call(updateTestEnv, payload, {
        successMsg: i18n.t('dop:environment configuration updated successfully'),
      });
      testEnv.getTestEnvList(query);
      testEnv.setActiveEnv(payload);
    },
    async updateAutoTestEnv({ call }, payload: TEST_ENV.ICreateAutoTestEnv) {
      await call(updateAutoTestEnv, payload, {
        successMsg: i18n.t('dop:environment configuration updated successfully'),
      });
      testEnv.getAutoTestEnvList({ scope: payload.scope, scopeID: Number(payload.scopeID) });
      testEnv.setActiveEnv(payload);
    },
    async deleteTestEnv({ call }, id: number, query: TEST_ENV.EnvListQuery) {
      await call(deleteTestEnv, id, { successMsg: i18n.t('dop:environment configuration deleted successfully') });
      testEnv.getTestEnvList(query);
      testEnv.setActiveEnv({});
    },
    async deleteAutoTestEnv({ call }, ns: string, query: TEST_ENV.IAutoEnvQuery) {
      await call(deleteAutoTestEnv, ns, {
        successMsg: i18n.t('dop:environment configuration deleted successfully'),
      });
      testEnv.getAutoTestEnvList(query);
      testEnv.setActiveEnv({});
    },
    async getTestEnvList({ call, update }, payload: TEST_ENV.EnvListQuery) {
      const envList = await call(getTestEnvList, payload);
      update({ envList });
    },
    async getProjectTestEnvList({ call, update }, payload: TEST_ENV.EnvListQuery) {
      const projectEnvList = await call(getTestEnvList, payload);
      update({ projectEnvList });
    },
    async getAutoTestEnvList({ call, update }, payload: TEST_ENV.IAutoEnvQuery) {
      const list = await call(getAutoTestEnvList, payload);
      const autoEnvList = (list || []).map(({ apiConfig, projectID, orgID, displayName, desc, ns }) => {
        return { ...apiConfig, projectID, orgID, displayName, desc, ns };
      });
      update({ autoEnvList });
    },
  },
  reducers: {
    setActiveEnv(state, payload: TEST_ENV.Item | {}) {
      state.active = payload;
    },
    clearEnvList(state) {
      state.envList = [];
    },
    clearProjectEnvList(state) {
      state.projectEnvList = [];
    },
    clearAutoTestEnvList(state) {
      state.autoEnvList = [];
    },
    openEnvVariable(state, payload: TEST_ENV.EnvListQuery) {
      state.envInfo = payload;
    },
    closeEnvVariable(state) {
      state.envInfo = {
        envID: 0,
        envType: '' as TEST_ENV.EnvType,
      };
    },
  },
});

export default testEnv;
