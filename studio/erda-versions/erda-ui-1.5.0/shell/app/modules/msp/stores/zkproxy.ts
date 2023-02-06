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
import i18n from 'i18n';
import { isEmpty, map, filter } from 'lodash';
import {
  getNodeList,
  updateNodeRule,
  getAppDetail,
  getRunTimes,
  getBranchesRule,
  updateBranchesRule,
  clearBranchesRule,
  getZkInterfaceList,
  getServiceByIp,
} from '../services/zkproxy';

interface IState {
  nodeData: MS_ZK.INodeData;
  appDetail: any;
  branches: string[];
  branchesRule: MS_ZK.IBranchesRule[];
  zkInterfaceList: MS_ZK.IZkproxy[];
  zkInterfaceConfig: any;
}

const initNodeData = {
  appid: '',
  node: [],
};

const initState: IState = {
  branches: [],
  branchesRule: [],
  appDetail: {},
  nodeData: initNodeData,
  zkInterfaceList: [],
  zkInterfaceConfig: {},
};

const zkproxy = createStore({
  name: 'zkproxy',
  state: initState,
  effects: {
    async getNodeList({ call, update, getParams, getQuery }) {
      const { env, projectId } = getParams();
      const { az, appId } = getQuery();
      if (!az || !appId) return;
      const nodeData = await call(getNodeList, { env, projectId, az, appid: appId } as any);
      update({ nodeData: nodeData || initNodeData });
    },
    async updateNodeRule(
      { call, getParams, getQuery },
      payload: { host: string; tenantId?: string; ruleData: MS_ZK.INodeListItem; tenantid?: string },
    ) {
      const { env, projectId } = getParams();
      const { az, appId } = getQuery();
      await call(updateNodeRule, { env, projectId, az, appid: appId, ...payload } as any, {
        successMsg: i18n.t('operated successfully'),
      });
      await zkproxy.effects.getNodeList();
    },
    async getAppDetail({ call, update, select, getQuery }) {
      const { appId } = getQuery();
      if (!appId) {
        update({ appDetail: {} });
        return;
      }
      // 如果没有id或id与url中的不一致则重新获取
      const newAppId = Number(appId);
      let { appDetail } = await select((s) => s);
      if (isEmpty(appDetail) || newAppId !== appDetail.id) {
        appDetail = await call(getAppDetail, newAppId);
      }
      update({ appDetail });
    },
    async getBranches({ call, update, getParams, getQuery }) {
      const { appId } = await getQuery();
      const runtimes = await call(getRunTimes, appId);
      const { env } = getParams();
      update({
        branches: map(
          filter(runtimes, (item) => item.extra.workspace === env),
          ({ name }) => name,
        ),
      });
    },
    async getBranchesRule({ call, update, getParams, getQuery }) {
      const { projectId, env } = getParams();
      const { appId, az } = await getQuery();
      if (!appId || !az) return;
      const data: { rule: MS_ZK.IBranchesRule[] } | null = await call(getBranchesRule, {
        projectId,
        env,
        appId,
        az,
      });
      update({ branchesRule: data && data.rule ? data.rule : [] });
    },
    async updateBranchesRule({ call, getParams, getQuery }, payload: { body: { rule: MS_ZK.IBranchesRule[] } }) {
      const { projectId, env } = getParams();
      const { appId, az } = await getQuery();
      await call(
        updateBranchesRule,
        { projectId, env, appId, az, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      await zkproxy.effects.getBranchesRule();
    },
    async clearBranchesRule({ call, getParams, getQuery }) {
      const { projectId, env } = getParams();
      const { appId, az } = await getQuery();
      await call(clearBranchesRule, { projectId, env, appId, az }, { successMsg: i18n.t('updated successfully') });
      await zkproxy.effects.getBranchesRule();
    },
    async getZkInterfaceList({ call, update, getParams }, payload: { az: string; runtimeId: number }) {
      const { projectId, env, tenantGroup } = getParams();
      const zkInterfaceList = await call(getZkInterfaceList, {
        projectId,
        env,
        tenantGroup,
        ...payload,
      });

      update({ zkInterfaceList });
    },
    async getServiceByIp({ call }, payload: MS_ZK.IServiceQuery) {
      return call(getServiceByIp, payload);
    },
  },
  reducers: {
    clearZkproxyInterfaceList(state) {
      state.zkInterfaceList = [];
    },
    setZkInterfaceConfig(state, zkInterfaceConfig) {
      state.zkInterfaceConfig = zkInterfaceConfig;
    },
  },
});

export default zkproxy;
