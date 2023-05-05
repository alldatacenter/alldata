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
  getConfigs,
  addConfigs,
  removeConfigs,
  getConfigNameSpaces,
  importConfigs,
  exportConfigs,
} from '../services/pipeline-config';
import { reduce, filter } from 'lodash';
import i18n from 'app/i18n';

interface IState {
  fullConfigs: PIPELINE_CONFIG.ConfigItemMap;
  encryptConfigs: PIPELINE_CONFIG.ConfigItemMap;
  unEncryptConfigs: PIPELINE_CONFIG.ConfigItemMap;
  total: number;
}

const initState: IState = {
  fullConfigs: {},
  encryptConfigs: {},
  unEncryptConfigs: {},
  total: 0,
};

const pipelineConfig = createFlatStore({
  name: 'pipelineConfig',
  state: initState,
  effects: {
    async getConfigs({ getParams, select, call, update }, payload: PIPELINE_CONFIG.ConfigQuery[], apiPrefix?: string) {
      const { appId } = getParams();
      const newConfigs = await call(getConfigs, { appID: appId, payload, apiPrefix });
      const { fullConfigs, unEncryptConfigs, encryptConfigs } = select((s) => s);
      const newUnEncryptConfigs = reduce(
        newConfigs,
        (result, value, key) => {
          // eslint-disable-next-line no-param-reassign
          result[key] = filter(value, { encrypt: false });
          return result;
        },
        {},
      );
      const newEncryptConfigs = reduce(
        newConfigs,
        (result, value, key) => {
          // eslint-disable-next-line no-param-reassign
          result[key] = filter(value, { encrypt: true });
          return result;
        },
        {},
      );

      update({
        fullConfigs: {
          ...fullConfigs,
          ...newConfigs,
        },
        unEncryptConfigs: {
          ...unEncryptConfigs,
          ...newUnEncryptConfigs,
        },
        encryptConfigs: {
          ...encryptConfigs,
          ...newEncryptConfigs,
        },
      });
    },
    async addConfigs({ getParams, call }, payload: PIPELINE_CONFIG.AddConfigsBodyWithoutAppId, apiPrefix?: string) {
      const { appId: appID } = getParams();
      await call(
        addConfigs,
        { ...payload, query: { ...payload.query, appID }, apiPrefix },
        { successMsg: i18n.t('added successfully') },
      );
      pipelineConfig.getConfigs([{ namespace_name: payload.query.namespace_name, decrypt: false }], apiPrefix);
    },
    async updateConfigs({ getParams, call }, payload: PIPELINE_CONFIG.AddConfigsBodyWithoutAppId, apiPrefix?: string) {
      const { appId: appID } = getParams();
      await call(
        addConfigs, // 溪杨说创建和更新暂时使用同一个接口
        { ...payload, query: { ...payload.query, appID }, apiPrefix },
        { successMsg: i18n.t('dop:modified successfully') },
      );
      pipelineConfig.getConfigs([{ namespace_name: payload.query.namespace_name, decrypt: false }], apiPrefix);
    },
    async removeConfig(
      { getParams, call },
      { namespace_name, key }: Omit<PIPELINE_CONFIG.DeleteConfigQuery, 'appID'>,
      apiPrefix?: string,
    ) {
      const { appId: appID } = getParams();
      await call(
        removeConfigs,
        { namespace_name, key, appID, apiPrefix },
        { successMsg: i18n.t('deleted successfully') },
      );
      pipelineConfig.getConfigs([{ namespace_name, decrypt: false }], apiPrefix);
    },
    async removeConfigWithoutDeploy(
      { getParams, call },
      { namespace_name, key }: Omit<PIPELINE_CONFIG.DeleteConfigQuery, 'appID'>,
      apiPrefix?: string,
    ) {
      const { appId: appID } = getParams();
      await call(
        removeConfigs,
        { namespace_name, key, appID, apiPrefix },
        { successMsg: i18n.t('deleted successfully') },
      );
      pipelineConfig.getConfigs([{ namespace_name, decrypt: false }], apiPrefix);
    },
    async getConfigNameSpaces({ call, getParams }) {
      const { appId: appID } = getParams();
      const { namespaces } = await call(getConfigNameSpaces, appID);
      return namespaces;
    },
    async importConfigs({ call }, payload: any, apiPrefix?: string) {
      await call(
        importConfigs,
        { configs: JSON.parse(payload.configs), query: { ...payload.query } },
        { successMsg: i18n.t('imported successfully') },
      );
      pipelineConfig.getConfigs([{ namespace_name: payload.query.namespace_name, decrypt: false }], apiPrefix);
    },
    async exportConfigs({ call }, query) {
      const content = await call(exportConfigs, query);
      return content;
    },
  },
  reducers: {
    clearConfigs(state) {
      state.fullConfigs = {};
      state.unEncryptConfigs = {};
      state.encryptConfigs = {};
    },
  },
});

export default pipelineConfig;
