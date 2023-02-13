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
import {
  addAPI,
  addPolicy,
  createApiLimit,
  createApiPackage,
  createConsumer,
  createOpenApiConsumer,
  createPackageApi,
  deleteAPI,
  deleteConsumer,
  deleteLimit,
  deleteOpenApiConsumer,
  deletePackage,
  deletePackageApi,
  deletePolicy,
  getApiConsumers,
  getApiDomain,
  getApiLimits,
  getAPIList,
  getApiPackageDetail,
  getApiPackageList,
  getApiPackages,
  getAPISummary,
  getAuthInfo,
  getBusinessCors,
  getBusinessCustom,
  getBusinessProxy,
  getConsumer,
  getConsumerAuthorizes,
  getConsumerAuthPackages,
  getConsumerCredentials,
  getConsumerDetail,
  getConsumerList,
  getDeployedBranches,
  getErrorSummary,
  getImportableApiList,
  getOpenApiConsumerList,
  getPackageDetailApiList,
  getPolicyList,
  getRegisterApps,
  getSafetyCsrf,
  getSafetyIP,
  getSafetyServerGuard,
  getSafetyWaf,
  getServiceApiPrefix,
  getServiceRuntime,
  getStatusCode,
  getStatusCodeChart,
  importApis,
  saveApiDomain,
  saveBusinessCors,
  saveBusinessCustom,
  saveBusinessProxy,
  saveConsumerApi,
  saveConsumerApiPolicy,
  saveSafetyCsrf,
  saveSafetyIP,
  saveSafetyServerGuard,
  saveSafetyWaf,
  updateAPI,
  updateApiLimit,
  updateApiPackage,
  updateAuthInfo,
  updateConsumerAuthorizes,
  updateConsumerAuthPackages,
  updateConsumerCredentials,
  updateConsumerDetail,
  updateOpenApiConsumer,
  updatePackageApi,
  updatePolicy,
} from 'msp/services/gateway';
import orgStore from 'app/org-home/stores/org';
import { getDefaultPaging } from 'common/utils';
import i18n from 'i18n';
import mspStore from 'msp/stores/micro-service';
import { getAppDetail } from 'application/services/application';
import { getRuntimeDetail } from 'runtime/services/runtime';
import { getProjectInfo } from 'project/services/project';
import apiRequestStore from 'api-insight/stores/request';
import { PAGINATION } from 'app/constants';
import { eventHub } from 'common/utils/event-hub';

interface PolicyFilter {
  diceApp: string;
  diceService: string;
}

interface ImportableApis {
  apis: GATEWAY.ImportApiItem[];
  routePrefix: string;
}

interface IState {
  authInfoList: GATEWAY.AuthInfoItem[];
  authInfoSelected: string[];
  apiPackageList: GATEWAY.ApiPackageItem[];
  apiPackageListPaging: IPaging;
  apiPackageDetail: GATEWAY.ApiPackageItem;
  registerApps: GATEWAY.RegisterApp[];
  consumerAuthorizes: GATEWAY.AuthInfoItem[];
  packageDetailApiList: GATEWAY.PackageDetailApiListItem[];
  packageDetailApiListPaging: IPaging;
  importableApis: ImportableApis;
  runtimeEntryData: GATEWAY.RuntimeEntryData;
  consumer: GATEWAY.Consumer;
  apiFilterCondition: GATEWAY.ApiFilterCondition;
  projectInfo: PROJECT.Detail;
  openApiConsumerList: GATEWAY.ConsumersName[];
  openApiConsumerListPaging: IPaging;
  consumerAuthPackages: GATEWAY.ConsumerAuthPackages[];
  authData: {
    keyAuth: {
      authTips: string;
      authData: GATEWAY.IAuthData_data[];
    };
    oAuth: {
      authData: GATEWAY.IAuthData_data[];
    };
  };
  authConfig: GATEWAY.IAuthConfig[];
  trafficControlPolicy: {
    policyList: GATEWAY.PolicyListItem[];
  };
  authPolicy: {
    policyList: GATEWAY.PolicyListItem[];
  };
  filters: GATEWAY.ApiFilter;
  apiList: GATEWAY.ApiResponse;
  apiDomain: GATEWAY.ApiDomain;
  apiLimits: GATEWAY.ApiLimitsItem[];
  apiLimitsPaging: IPaging;
  safetyWaf: GATEWAY.SafetyWaf;
  safetyIP: GATEWAY.SafetyIp;
  safetyServerGuard: GATEWAY.SafetyServerGuard;
  safetyCsrf: GATEWAY.SafetyCsrf;
  businessProxy: GATEWAY.BusinessProxy;
  businessCors: GATEWAY.BusinessCors;
  businessCustom: GATEWAY.BusinessCustom;
  policyFilter: PolicyFilter;
  needAuthApiList: GATEWAY.ApiListItem[];
  needAuthApiListPaging: IPaging;
  authConsumer: Record<string, any>;
  consumerList: GATEWAY.IConsumer[];
  statusCodeChart: Record<string, any>;
  errorSummary: any[];
  statusCode: any[];
  countSummary: any[];
  rtSummary: any[];
}

const initState: IState = {
  authInfoList: [],
  authInfoSelected: [],
  apiPackageList: [],
  apiPackageListPaging: getDefaultPaging(),
  apiPackageDetail: {} as GATEWAY.ApiPackageItem,
  registerApps: [],
  consumerAuthorizes: [],
  packageDetailApiList: [],
  packageDetailApiListPaging: getDefaultPaging(),
  importableApis: {} as ImportableApis,
  runtimeEntryData: {} as GATEWAY.RuntimeEntryData,
  consumer: {
    endpoint: {
      outerAddr: '',
      innerAddr: '',
      innerTips: '',
    },
  } as GATEWAY.Consumer,
  apiFilterCondition: {} as GATEWAY.ApiFilterCondition,
  projectInfo: {} as PROJECT.Detail,
  openApiConsumerList: [],
  openApiConsumerListPaging: getDefaultPaging(),
  consumerAuthPackages: [],
  authData: {
    keyAuth: { authTips: '', authData: [] },
    oAuth: { authData: [] },
  },
  authConfig: [],
  trafficControlPolicy: {
    policyList: [],
  },
  authPolicy: {
    policyList: [],
  },
  filters: {} as GATEWAY.ApiFilter,
  apiList: { result: [], page: {} as GATEWAY.ApiPage },
  apiDomain: {} as GATEWAY.ApiDomain,
  apiLimits: [],
  apiLimitsPaging: getDefaultPaging(),
  safetyWaf: {} as GATEWAY.SafetyWaf,
  safetyIP: {} as GATEWAY.SafetyIp,
  safetyServerGuard: {} as GATEWAY.SafetyServerGuard,
  safetyCsrf: {} as GATEWAY.SafetyCsrf,
  businessProxy: {} as GATEWAY.BusinessProxy,
  businessCors: {} as GATEWAY.BusinessCors,
  businessCustom: {} as GATEWAY.BusinessCustom,
  policyFilter: {} as PolicyFilter,
  needAuthApiList: [],
  needAuthApiListPaging: getDefaultPaging(),
  authConsumer: {},
  consumerList: [],
  statusCodeChart: {},
  errorSummary: [],
  statusCode: [],
  rtSummary: [],
  countSummary: [],
};

const queryRegisterApps = (hasMenu: boolean, showIntro: boolean) => {
  if (hasMenu) {
    if (!showIntro) {
      gatewayStore.effects.getRegisterApps();
    }
  } else {
    eventHub.once('gatewayStore/getRegisterApps', (flag) => {
      if (!flag) {
        gatewayStore.effects.getRegisterApps();
      }
    });
  }
};

const gatewayStore = createStore({
  name: 'gatewayStore',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isIn, isLeaving, isEntering, query }: IRouteInfo) => {
      const [showIntro, menu] = mspStore.getState((s) => [s.intro.APIGateway, s.mspMenu]);
      const hasMenu = !!menu.length;
      if (isIn('gateway-route')) {
        if (['api-policies', 'old-policies', 'api-monitor'].some((path) => isIn(path))) {
          gatewayStore.effects.getConsumer();
        }

        const paramsGroup = ['appId', 'runtimeId'];
        if (paramsGroup.every((p) => p in query)) {
          // 从runtime进入
          const { runtimeId, appId } = query;
          gatewayStore.effects.getRuntimeEntryData({ appId, runtimeId });
        } else {
          // 从微服务进入
          queryRegisterApps(hasMenu, showIntro);
        }
      }
      if (isEntering('api-platform')) {
        queryRegisterApps(hasMenu, showIntro);
      }
      if (isEntering('consumer-audit')) {
        gatewayStore.effects.getApiFilterCondition();
        gatewayStore.effects.getProjectInfo();
      }
      if (isLeaving('gateway-route')) {
        gatewayStore.reducers.clearGatewayInfo();
        apiRequestStore.reducers.resetSearchFields();
      }
    });
  },
  effects: {
    async getRuntimeDetail({ call }, payload: { runtimeId: number | string }) {
      const res = await call(getRuntimeDetail, payload);
      return res;
    },
    async getRuntimeEntryData({ call, update }, payload: { runtimeId: string; appId: string }) {
      const { runtimeId, appId } = payload;
      const { name: appName } = await call(getAppDetail, appId);
      const { services } = await call(getRuntimeDetail, { runtimeId });
      update({ runtimeEntryData: { diceApp: appName, services } });
    },
    async updateFilters({ update, select }, payload: GATEWAY.ApiFilter) {
      const filters = select((s: IState) => s.filters);
      update({ filters: { ...filters, ...payload } });
    },
    async createConsumer({ call, getParams }, payload) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const authConsumer = await call(createConsumer, { ...payload, orgId, env, projectId });
      if (authConsumer) {
        gatewayStore.effects.getConsumerList();
      }
    },
    async deleteConsumer({ call }, payload) {
      const delConsumer = await call(deleteConsumer, { ...payload });
      if (delConsumer) {
        gatewayStore.effects.getConsumerList();
      }
    },
    async getConsumerList({ call, update, getParams }) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const consumerList = await call(getConsumerList, { orgId, projectId, env });
      if (consumerList) {
        update({ consumerList: consumerList.consumers });
      }
    },
    async getRegisterApps({ call, getParams, update }) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { apps: registerApps } = await call(getRegisterApps, { orgId, projectId, env });
      update({ registerApps });
    },
    async getServiceRuntime({ call, getParams }, payload: Omit<GATEWAY.GetRuntimeDetail, keyof GATEWAY.Base>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(getServiceRuntime, { orgId, projectId, env, ...payload });
      return res;
    },
    async getApiDomain({ call, update, getParams }, payload: Omit<GATEWAY.GetDomain, keyof GATEWAY.Base>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const apiDomain = await call(getApiDomain, { orgId, projectId, env, ...payload });
      update({ apiDomain });
    },
    async saveApiDomain({ call, update, getParams }, payload: Omit<GATEWAY.SaveDomain, keyof GATEWAY.Base>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const apiDomain = await call(
        saveApiDomain,
        { orgId, projectId, env, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      update({ apiDomain });
    },
    async getSafetyWaf({ call, update, getParams }, payload: Omit<GATEWAY.GetSafety, 'packageId'> = {}) {
      const { packageId } = getParams();
      const safetyWaf = await call(getSafetyWaf, { packageId, ...payload });
      if (payload.apiId) {
        update({ safetyWaf });
      }
      return safetyWaf;
    },
    async getSafetyIP({ call, update, getParams }, payload: Omit<GATEWAY.GetSafety, 'packageId'> = {}) {
      const { packageId } = getParams();
      const safetyIP = await call(getSafetyIP, { packageId, ...payload });
      if (payload.apiId) {
        update({ safetyIP });
      }
      return safetyIP;
    },
    async getSafetyServerGuard({ call, update, getParams }, payload: Omit<GATEWAY.GetSafety, 'packageId'> = {}) {
      const { packageId } = getParams();
      const safetyServerGuard = await call(getSafetyServerGuard, { packageId, ...payload });
      if (payload.apiId) {
        update({ safetyServerGuard });
      }
      return safetyServerGuard;
    },
    async getSafetyCsrf({ call, update, getParams }, payload: Omit<GATEWAY.GetSafety, 'packageId'> = {}) {
      const { packageId } = getParams();
      const safetyCsrf = await call(getSafetyCsrf, { packageId, ...payload });
      if (payload.apiId) {
        update({ safetyCsrf });
      }
      return safetyCsrf;
    },
    async getBusinessProxy({ call, update, getParams }, payload: Omit<GATEWAY.GetBusiness, 'packageId'> = {}) {
      const { packageId } = getParams();
      const businessProxy = await call(getBusinessProxy, { packageId, ...payload });
      if (payload.apiId) {
        update({ businessProxy });
      }
      return businessProxy;
    },
    async getBusinessCors({ call, update, getParams }, payload: Omit<GATEWAY.GetBusiness, 'packageId'> = {}) {
      const { packageId } = getParams();
      const businessCors = await call(getBusinessCors, { packageId, ...payload });
      if (payload.apiId) {
        update({ businessCors });
      }
      return businessCors;
    },
    async getBusinessCustom({ call, update, getParams }, payload: Omit<GATEWAY.GetBusiness, 'packageId'> = {}) {
      const { packageId } = getParams();
      const businessCustom = await call(getBusinessCustom, { packageId, ...payload });
      if (payload.apiId) {
        update({ businessCustom });
      }
      return businessCustom;
    },
    async saveSafetyWaf({ call, update, getParams }, payload: Omit<GATEWAY.SaveWrf, 'packageId'>) {
      const { packageId } = getParams();
      const safetyWaf = await call(
        saveSafetyWaf,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ safetyWaf });
      }
    },
    async saveSafetyIP({ call, update, getParams }, payload: Omit<GATEWAY.SaveIp, 'packageId'>) {
      const { packageId } = getParams();
      const safetyIP = await call(
        saveSafetyIP,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ safetyIP });
      }
    },
    async saveSafetyServerGuard({ call, update, getParams }, payload: Omit<GATEWAY.SaveServerGuard, 'packageId'>) {
      const { packageId } = getParams();
      const safetyServerGuard = await call(
        saveSafetyServerGuard,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ safetyServerGuard });
      }
    },
    async saveSafetyCsrf({ call, update, getParams }, payload: Omit<GATEWAY.SaveCsrf, 'packageId'>) {
      const { packageId } = getParams();
      const safetyCsrf = await call(
        saveSafetyCsrf,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ safetyCsrf });
      }
    },
    async saveBusinessProxy({ call, update, getParams }, payload: Omit<GATEWAY.SaveProxy, 'packageId'>) {
      const { packageId } = getParams();
      const businessProxy = await call(
        saveBusinessProxy,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ businessProxy });
      }
    },
    async saveBusinessCors({ call, update, getParams }, payload: Omit<GATEWAY.SaveCors, 'packageId'>) {
      const { packageId } = getParams();
      const businessCors = await call(
        saveBusinessCors,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ businessCors });
      }
    },
    async saveBusinessCustom({ call, update, getParams }, payload: Omit<GATEWAY.SaveCustom, 'packageId'>) {
      const { packageId } = getParams();
      const businessCustom = await call(
        saveBusinessCustom,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      if (payload.apiId) {
        update({ businessCustom });
      }
    },
    async getGatewayAddonInfo({ call, getParams }, { appId }: { appId: string }) {
      const { projectId } = getParams();
      const { name: projectName } = await call(getProjectInfo, projectId);
      if (appId) {
        const { name: appName } = await call(getAppDetail, appId);
        return {
          projectName: projectName.toLowerCase(),
          appName: appName.toLowerCase(),
        };
      }
      return { projectName: projectName.toLowerCase() };
    },
    async getConsumerDetail({ call, update }, payload) {
      const { authConfig } = await call(getConsumerDetail, payload);
      if (authConfig && authConfig.auths) {
        const keyAuth = authConfig.auths.find(
          (item: GATEWAY.IAuthConfig) => item.authType === 'key-auth',
        ) as GATEWAY.IAuthConfig;
        const oAuth = authConfig.auths.find(
          (item: GATEWAY.IAuthConfig) => item.authType === 'oauth2',
        ) as GATEWAY.IAuthConfig;

        update({
          authData: {
            keyAuth: {
              authTips: keyAuth.authTips || '',
              authData: keyAuth.authData.data || [],
            },
            oAuth: {
              authData: oAuth.authData.data || [],
            },
          },
        });
      }
    },
    async updateConsumerDetail({ call }, payload: GATEWAY.updateConsumer) {
      await call(updateConsumerDetail, payload, { successMsg: i18n.t('updated successfully') });
      await gatewayStore.effects.getConsumerDetail(payload.consumerId);
    },
    async saveConsumerApi({ call }, payload) {
      const result = await call(saveConsumerApi, payload, {
        successMsg: i18n.t('operated successfully'),
      });
      if (result) {
        gatewayStore.effects.getConsumerList();
      }
    },
    async saveConsumerApiPolicy({ call }, payload: GATEWAY.SavePoliciesApi) {
      const result = await call(saveConsumerApiPolicy, payload, {
        successMsg: i18n.t('set successfully'),
      });
      if (result) {
        gatewayStore.effects.getConsumerList();
      }
    },
    async getAPIList(
      { call, update, getParams },
      payload: Merge<
        Merge<Omit<GATEWAY.GetApiList, keyof GATEWAY.Base>, GATEWAY.Query>,
        { filters: GATEWAY.ApiFilter }
      >,
    ) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId, env } = getParams() as { projectId: string; env: GATEWAY.EnvType };
      const apiList = await call(getAPIList, {
        page: payload.pageNo,
        size: payload.pageSize,
        orgId,
        projectId,
        env,
        diceService: payload.diceService || undefined,
        diceApp: payload.diceApp || undefined,
        runtimeId: payload.runtimeId || undefined,
        ...payload.filters,
        from: payload.from,
      });
      if (apiList) {
        update({ apiList });
      }
    },
    async getNeedAuthAPIList({ call, update, getParams }, payload) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId, env } = getParams();

      if (payload.isResetNull) {
        update({ needAuthApiList: [], needAuthApiListPaging: getDefaultPaging() });
        return {
          list: [],
          total: 0,
        };
      }

      const { result, page } = await call(getAPIList, {
        page: payload.pageNo,
        size: payload.pageSize || PAGINATION.pageSize,
        orgId,
        projectId,
        env,
        needAuth: 1,
        ...payload.filters,
      });
      const { totalNum, curPage, pageSize } = page;
      const needAuthApiListPaging: IPaging = {
        total: totalNum,
        hasMore: Math.ceil(totalNum / pageSize) > curPage,
        pageNo: curPage,
        pageSize,
      };
      update({ needAuthApiList: result, needAuthApiListPaging });
      return {
        list: result,
        total: page.totalNum,
      };
    },
    async addAPI({ select, call, getParams }, payload: GATEWAY.AddAPI) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const consumer = select((s: IState) => s.consumer);
      const { projectId, env } = getParams();
      const { consumerId } = consumer;

      const res = await call(
        addAPI,
        {
          ...payload,
          orgId,
          env,
          projectId,
          consumerId,
        },
        { successMsg: i18n.t('added successfully') },
      );
      return res;
    },
    async updateAPI({ select, call, getParams }, payload: Merge<GATEWAY.AddAPI, { apiId: string }>) {
      const { projectId, env } = getParams();
      const { consumerId } = select((s: IState) => s.consumer);
      const newAPI = await call(
        updateAPI,
        { ...payload, env, projectId, consumerId },
        { successMsg: i18n.t('updated successfully') },
      );
      gatewayStore.reducers.updateAPISuccess(newAPI);
    },
    async deleteAPI({ select, call }, { apiId }: GATEWAY.ApiListItem) {
      await call(deleteAPI, apiId, { successMsg: i18n.t('deleted successfully') });
      return select((s: IState) => s);
    },
    async getConsumer({ call, update, getParams }) {
      const clusterName = mspStore.getState((s) => s.clusterName);
      const { env, projectId } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const consumer = await call(getConsumer, {
        projectId,
        orgId,
        env,
        az: encodeURIComponent(clusterName),
      });

      update({ consumer });
    },
    async getAPISummary({ call, update, getParams }, payload: GATEWAY.Common) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId } = getParams();
      const result = await call(getAPISummary, {
        ...payload,
        key: `${orgId}_${projectId}`,
      });
      update({ [`${payload.type}Summary`]: result });
    },
    async getStatusCode({ call, update, getParams }, payload: GATEWAY.Common) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId } = getParams();
      const statusCode = await call(getStatusCode, {
        ...payload,
        key: `${orgId}_${projectId}`,
      });
      update({ statusCode });
    },
    async getStatusCodeChart({ call, update, getParams }, payload) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId } = getParams();
      const statusCodeChart = await call(getStatusCodeChart, {
        ...payload,
        key: `${orgId}_${projectId}`,
      });
      update({ statusCodeChart });
    },
    async getErrorSummary({ call, update, getParams }, payload: GATEWAY.Common) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { projectId } = getParams();
      const errorSummary = await call(getErrorSummary, {
        ...payload,
        key: `${orgId}_${projectId}`,
      });
      update({ errorSummary });
    },
    async getPolicyList({ call, update, getParams }, payload: { category: string }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { env, projectId } = getParams();
      const result = await call(getPolicyList, {
        ...payload,
        projectId,
        orgId,
        env,
      });
      if (result) {
        let obj = {};
        switch (payload.category) {
          case 'trafficControl':
            obj = { trafficControlPolicy: result };
            break;
          case 'auth':
            obj = { authPolicy: result };
            break;
          default:
            break;
        }
        update(obj);
      }
    },
    async addPolicy({ call, getParams }, payload: { data: GATEWAY.UpdatePolicy; category: string }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { env, projectId } = getParams();
      // eslint-disable-next-line no-param-reassign
      payload.data = {
        ...payload.data,
        projectId,
        orgId,
        env,
      };
      await call(addPolicy, payload, { successMsg: i18n.t('added successfully') });
      gatewayStore.effects.getPolicyList({ category: 'trafficControl' });
    },
    async updatePolicy({ call, getParams }, payload: { data: GATEWAY.UpdatePolicy; category: string }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { env } = getParams();
      // eslint-disable-next-line no-param-reassign
      payload.data = {
        ...payload.data,
        orgId,
        env,
      };
      const newData = await call(updatePolicy, payload, { successMsg: i18n.t('updated successfully') });
      gatewayStore.reducers.updateTrafficControlPolicySuccess(newData);
    },
    async deletePolicy({ call }, payload: { data: { policyId: string }; category: string }) {
      await call(deletePolicy, payload, { successMsg: i18n.t('deleted successfully') });
      gatewayStore.effects.getPolicyList({ category: 'trafficControl' });
    },
    async getApiPackageList({ call, update, getParams }, payload: Partial<GATEWAY.Query> = { pageNo: 1 }) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { total, list: apiPackageList } = await call(
        getApiPackageList,
        { orgId, projectId, env, ...payload },
        { paging: { key: 'apiPackageListPaging' } },
      );
      update({ apiPackageList });
      return { list: apiPackageList, total };
    },
    async createApiPackage({ call, getParams }, payload: Omit<GATEWAY.CreatApiPackege, keyof GATEWAY.Base>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(createApiPackage, { orgId, projectId, env, ...payload });
      return res;
    },
    async getApiPackageDetail({ update, call, getParams }) {
      const { packageId } = getParams();
      const apiPackageDetail = await call(getApiPackageDetail, { id: packageId });
      update({ apiPackageDetail });
    },
    async updateApiPackage({ call, update }, payload: GATEWAY.UpdataApiPackage) {
      const apiPackageDetail = await call(updateApiPackage, payload);
      update({ apiPackageDetail });
      return apiPackageDetail;
    },
    async deletePackage({ call }, payload: { packageId: number }) {
      await call(deletePackage, { ...payload }, { successMsg: i18n.t('deleted successfully') });
      gatewayStore.effects.getApiPackageList();
    },
    async getPackageDetailApiList(
      { call, update, getParams },
      payload: Omit<GATEWAY.GetPackageDetailApiList, 'packageId'>,
    ) {
      const { packageId } = getParams();
      const { list: packageDetailApiList, total } = await call(
        getPackageDetailApiList,
        { packageId, ...payload },
        { paging: { key: 'packageDetailApiListPaging' } },
      );
      update({ packageDetailApiList });
      return { list: packageDetailApiList, total };
    },
    async createPackageApi({ call, getParams }, payload: GATEWAY.CreatePackageApi) {
      const { packageId } = getParams();
      const res = await call(createPackageApi, { packageId, ...payload }, { successMsg: i18n.t('added successfully') });
      return res;
    },
    async updatePackageApi({ call, getParams }, payload) {
      const { packageId } = getParams();
      const res = await call(
        updatePackageApi,
        { packageId, ...payload },
        { successMsg: i18n.t('updated successfully') },
      );
      return res;
    },
    async getImportableApiList({ call, update, getParams }, payload) {
      const { packageId } = getParams();
      const importableApis = await call(getImportableApiList, { packageId, ...payload });
      update({ importableApis });
    },
    async importApis({ call, getParams }, payload: GATEWAY.Common) {
      const { packageId } = getParams();
      const res = await call(importApis, { packageId, ...payload }, { successMsg: i18n.t('imported successfully') });
      return res;
    },
    async deletePackageApi({ call, getParams }, payload: { apiId: string }) {
      const { packageId } = getParams();
      const res = await call(
        deletePackageApi,
        { packageId, ...payload },
        { successMsg: i18n.t('deleted successfully') },
      );
      return res;
    },
    async getConsumerAuthorizes({ call, update }, payload: GATEWAY.Package) {
      const consumerAuthorizes = await call(getConsumerAuthorizes, payload);
      update({ consumerAuthorizes });
    },
    async updateConsumerAuthorizes(
      { call, update },
      payload: Merge<GATEWAY.UpdateConsumerAuthByConsumers, { newList: GATEWAY.AuthInfoItem[] }>,
    ) {
      await call(updateConsumerAuthorizes, payload);
      update({ consumerAuthorizes: payload.newList });
    },
    async getOpenApiConsumerList({ call, update, getParams }, payload: Partial<GATEWAY.Query>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { total, list: openApiConsumerList } = await call(
        getOpenApiConsumerList,
        { orgId, projectId, env, ...payload },
        { paging: { key: 'openApiConsumerListPaging' } },
      );
      update({ openApiConsumerList });
      return { list: openApiConsumerList, total };
    },
    async createOpenApiConsumer(
      { call, getParams },
      payload: Pick<GATEWAY.CreateOpenApiConsumer, 'name' | 'description'>,
    ) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = call(
        createOpenApiConsumer,
        { orgId, projectId, env, ...payload },
        { successMsg: i18n.t('added successfully') },
      );
      return res;
    },
    async updateOpenApiConsumer({ call }, payload: { id: string; description: string }) {
      const res = call(updateOpenApiConsumer, { ...payload }, { successMsg: i18n.t('updated successfully') });
      return res;
    },
    async deleteOpenApiConsumer({ call }, payload: { consumerId: string }) {
      const res = await call(deleteOpenApiConsumer, { ...payload }, { successMsg: i18n.t('deleted successfully') });
      return res;
    },
    async getConsumerCredentials({ call, update }, payload: { consumerId: string }) {
      const {
        authConfig: { auths },
      } = await call(getConsumerCredentials, { ...payload });
      update({ authConfig: auths });
    },
    async updateConsumerCredentials({ call, update }, payload: GATEWAY.UpdateCredentials) {
      const {
        authConfig: { auths },
      } = await call(updateConsumerCredentials, { ...payload }, { successMsg: i18n.t('updated successfully') });
      update({ authConfig: auths });
    },
    async getConsumerAuthPackages({ call, update }, payload: { consumerId: string }) {
      const consumerAuthPackages = await call(getConsumerAuthPackages, { ...payload });
      update({ consumerAuthPackages });
    },
    async updateConsumerAuthPackages({ call }, payload: GATEWAY.UpdateConsumerAuthByPackages) {
      await call(updateConsumerAuthPackages, { ...payload }, { successMsg: i18n.t('updated successfully') });
    },
    async getApiFilterCondition({ call, getParams, update }) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const [apiPackages, apiConsumers] = (await Promise.all([
        call(getApiPackages, { orgId, projectId, env }),
        call(getApiConsumers, { orgId, projectId, env }),
      ])) as any as [GATEWAY.ApiPackageItem[], GATEWAY.ConsumersName[]];
      const apiFilterCondition = { apiPackages, apiConsumers };
      update({ apiFilterCondition });
    },
    async getApiLimits({ call, update, getParams }, payload: GATEWAY.GetApiLimit) {
      const { projectId, env, packageId } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { total, list: apiLimits } = await call(
        getApiLimits,
        { orgId, projectId, env, packageId, ...payload },
        { paging: { key: 'apiLimitsPaging' } },
      );
      update({ apiLimits });
      return { list: apiLimits, total };
    },
    async createApiLimit({ call, getParams }, payload: GATEWAY.updateLimit) {
      const { projectId, env, packageId } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(
        createApiLimit,
        { ...payload, orgId, projectId, env, packageId },
        { successMsg: i18n.t('created successfully') },
      );
      return res;
    },
    async updateApiLimit({ call }, payload: Merge<GATEWAY.updateLimit, { ruleId: string }>) {
      const res = await call(updateApiLimit, payload, { successMsg: i18n.t('updated successfully') });
      return res;
    },
    async deleteLimit({ call }, payload: { ruleId: string }) {
      const res = call(deleteLimit, payload, { successMsg: i18n.t('deleted successfully') });
      return res;
    },
    async getProjectInfo({ call, getParams, update }) {
      const { projectId } = getParams();
      const projectInfo = await call(getProjectInfo, projectId);
      update({ projectInfo });
    },
    async getDeployedBranches(
      { call, getParams, update },
      { diceService, diceApp }: { diceService: string; diceApp: string },
    ) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const projectInfo = await call(getDeployedBranches, {
        projectId,
        env,
        orgId,
        diceService,
        diceApp,
      });
      update({ projectInfo });
    },
    async getAuthinfo({ call, update }, payload: GATEWAY.GetAuthInfo) {
      const authInfoList = await call(getAuthInfo, payload);
      const authInfoSelected: string[] = [];
      authInfoList.forEach((item) => {
        if (item.selected) {
          authInfoSelected.push(item.id);
        }
      });
      update({ authInfoSelected, authInfoList });
    },
    async updateAuthinfo({ call }, payload: GATEWAY.UpdateAuthInfo) {
      const res = await call(updateAuthInfo, payload);
      return res;
    },
    async getServiceApiPrefix({ call, getParams }, payload: Omit<GATEWAY.QueryApiPrefix, keyof GATEWAY.Base>) {
      const { projectId, env } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(getServiceApiPrefix, { orgId, projectId, env, ...payload });
      return res;
    },
  },
  reducers: {
    updateAuthInfoList(state, data) {
      state.authInfoList = data;
    },
    updateAuthInfoSelected(state, selectedRowKeys) {
      state.authInfoSelected = selectedRowKeys;
    },
    clearApiPackageList(state) {
      state.apiPackageList = [];
    },
    clearApiPackageDetail(state) {
      state.apiPackageDetail = {} as GATEWAY.ApiPackageItem;
    },
    clearGatewayInfo(state) {
      state.filters = {} as GATEWAY.ApiFilter;
      state.policyFilter = {} as PolicyFilter;
      state.registerApps = [];
      state.apiDomain = {} as GATEWAY.ApiDomain;
      state.runtimeEntryData = {} as GATEWAY.RuntimeEntryData;
      state.apiFilterCondition = {} as GATEWAY.ApiFilterCondition;
      state.projectInfo = {} as PROJECT.Detail;
    },
    updateAPISuccess(state, payload) {
      const apiList = { ...state.apiList };
      apiList.result = apiList.result.map((item) => (item.apiId === payload.apiId ? payload : item));
      state.apiList = apiList;
    },
    cleanAPIList(state) {
      state.apiList = {
        result: [],
        page: {} as GATEWAY.ApiPage,
      };
    },
    clearApiFilter(state) {
      state.filters = {} as GATEWAY.ApiFilter;
    },
    clearImportableApis(state) {
      state.importableApis = {} as ImportableApis;
    },
    updatePolicyFilter(state, payload) {
      state.policyFilter = {
        ...state.policyFilter,
        ...payload,
      };
    },
    updateTrafficControlPolicySuccess(state, payload) {
      const trafficControlPolicy = { ...state.trafficControlPolicy };
      trafficControlPolicy.policyList = trafficControlPolicy.policyList.map((item) =>
        item.policyId === payload.policyId ? payload : item,
      );
      state.trafficControlPolicy = trafficControlPolicy;
    },
  },
});

export default gatewayStore;
