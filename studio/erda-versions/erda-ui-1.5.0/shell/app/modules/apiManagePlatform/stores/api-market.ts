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
  updateAsset,
  createAsset,
  getApiAssetsList,
  getAssetVersionDetail,
  getListOfVersions,
  getAssetDetail,
  updateAssetVersion,
  deleteAssetVersion,
  deleteAsset,
  relationInstance,
  updateInstance,
  getVersionTree,
  addNewVersion,
  runAttemptTest,
  getInstances,
} from '../services/api-market';
import { getDefaultPaging, goTo } from 'common/utils';
import breadcrumbStore from 'layout/stores/breadcrumb';
import permStore from 'user/stores/permission';
import i18n from 'i18n';
import { isCreator } from 'apiManagePlatform/components/auth-wrap';
import orgStore from 'app/org-home/stores/org';

interface IState {
  assetList: API_MARKET.AssetListItem[];
  assetListPaging: IPaging;
  assetDetail: API_MARKET.AssetDetail;
  assetVersionDetail: API_MARKET.AssetVersionDetail;
  assetVersionList: API_MARKET.VersionItem[];
  versionTree: API_MARKET.VersionTreeItem[];
  instance: API_MARKET.Instantiation;
  instancePermission: API_MARKET.InstantiationPermission;
}

const initState: IState = {
  assetList: [],
  assetListPaging: getDefaultPaging(),
  assetDetail: {
    asset: {},
    permission: {},
  } as API_MARKET.AssetDetail,
  assetVersionDetail: {
    access: {},
    spec: {},
    version: {},
    asset: {},
  } as API_MARKET.AssetVersionDetail,
  assetVersionList: [],
  versionTree: [],
  instance: {} as API_MARKET.Instantiation,
  instancePermission: {} as API_MARKET.InstantiationPermission,
};

const checkAssetReadAuthInApp = (appID: number, showNoAuth: boolean) => {
  permStore.effects.checkRouteAuth({
    id: `${appID}`,
    type: 'app',
    routeMark: 'apiManage',
    cb: () => {
      const hasAuth = permStore.getState((s) => s.app.apiManage.apiMarket.read.pass);
      if (!hasAuth && showNoAuth) {
        goTo(goTo.pages.noAuth, { replace: true });
      }
    },
  });
};

const apiMarketStore = createStore({
  name: 'apiMarket',
  state: initState,
  effects: {
    async getAssetList({ call, update, select }, payload: API_MARKET.QueryAssets) {
      const { list } = await call(getApiAssetsList, payload, { paging: { key: 'assetListPaging' } });
      let assetList = list || [];
      if (payload.pageNo && payload.pageNo > 1) {
        const prevList = select((s) => s.assetList);
        assetList = [...prevList, ...list];
      }
      update({ assetList });
    },
    async createAsset({ call }, payload: Omit<API_MARKET.CreateAsset, 'orgID'>) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(
        createAsset,
        { ...payload, orgID: orgId },
        { successMsg: i18n.t('default:created successfully') },
      );
      return res;
    },
    async editAsset({ call }, payload: API_MARKET.UpdateAsset) {
      const res = await call(updateAsset, payload, { successMsg: i18n.t('default:updated successfully') });
      return res;
    },
    async getAssetDetail({ call, update }, payload: API_MARKET.QAsset, refreshAuth = false) {
      const assetDetail = await call(getAssetDetail, payload);
      const { assetName, projectID, creatorID, appID } = assetDetail.asset;
      breadcrumbStore.reducers.setInfo('assetName', assetName);
      const isOrgManage = permStore.getState((s) => s.org.apiAssetEdit.pass);
      if (!isOrgManage && !isCreator(creatorID) && refreshAuth) {
        // 更新关联项目信息后，刷新权限
        if (projectID) {
          permStore.effects.checkRouteAuth({
            id: `${projectID}`,
            type: 'project',
            routeMark: 'apiManage',
            cb: () => {
              const [hasReadAuth, hasEditAuth] = permStore.getState((s) => [
                s.project.apiManage.apiMarket.read.pass,
                s.project.apiManage.apiMarket.edit.pass,
              ]);
              if (!hasReadAuth) {
                if (appID) {
                  checkAssetReadAuthInApp(appID, true);
                } else {
                  goTo(goTo.pages.noAuth, { replace: true });
                }
              } else if (!hasEditAuth) {
                if (appID) {
                  checkAssetReadAuthInApp(appID, false);
                }
              }
            },
          });
        } else {
          goTo(goTo.pages.noAuth, { replace: true });
        }
      }
      update({ assetDetail });
      return assetDetail;
    },
    async getListOfVersions({ call, update }, payload: API_MARKET.QueryVersionsList) {
      const res = await call(getListOfVersions, payload);
      update({ assetVersionList: res.list || [] });
      return res;
    },
    async getAssetVersionDetail({ call, update }, payload: API_MARKET.QueryVersionDetail) {
      const assetVersionDetail = await call(getAssetVersionDetail, payload);
      const { assetName, major, minor, patch } = assetVersionDetail.version;
      const { projectID, creatorID, public: isPublic } = assetVersionDetail.asset;
      const name = `${assetName}（V ${major}.${minor}.${patch}）`;
      breadcrumbStore.reducers.setInfo('assetName', name);
      // 企业管理员
      const isOrgManage = permStore.getState((s) => s.org.apiAssetEdit.pass);
      if (!isOrgManage) {
        // 如果是public，则所有人可以看，否则进行权限校验
        if (!isPublic) {
          // 项目成员包含应用成员，读权限只需判断是否项目成员即可
          if (projectID) {
            permStore.effects.checkRouteAuth({
              id: `${projectID}`,
              type: 'project',
              routeMark: 'apiManage',
              cb: () => {
                const hasAuth = permStore.getState((s) => s.project.apiManage.apiMarket.read.pass);
                if (!(hasAuth || isCreator(creatorID))) {
                  goTo(goTo.pages.noAuth, { replace: true });
                }
              },
            });
          } else if (!isCreator(creatorID)) {
            goTo(goTo.pages.noAuth, { replace: true });
          }
        }
      }
      update({
        assetVersionDetail: {
          access: {},
          ...assetVersionDetail,
        },
      });
      return assetVersionDetail;
    },
    async deleteAsset({ call }, payload: API_MARKET.QAsset) {
      const res = await call(deleteAsset, payload, { successMsg: i18n.t('default:deleted successfully') });
      return res;
    },
    async deleteAssetVersion({ call }, payload: API_MARKET.QVersion) {
      const res = await call(deleteAssetVersion, payload, {
        successMsg: i18n.t('default:deleted successfully'),
      });
      return res;
    },

    async updateAssetVersion({ call }, payload: API_MARKET.UpdateAssetVersion) {
      const res = call(updateAssetVersion, payload);
      return res;
    },

    async addNewVersion({ call }, payload: API_MARKET.CreateVersion) {
      const res = await call(addNewVersion, payload, { successMsg: i18n.t('default:added successfully') });
      return res;
    },
    async getVersionTree({ call, update }, payload: API_MARKET.QueryVersionTree) {
      const res = await call(getVersionTree, payload);
      update({ versionTree: res.list });
      return res;
    },
    async editInstance({ call, update }, payload: API_MARKET.UpdateInstance | API_MARKET.RelationInstance) {
      let res = {} as API_MARKET.IInstantiation;
      if ('instantiationID' in payload) {
        res = await call(updateInstance, payload);
      } else {
        res = await call(relationInstance, payload);
        if (payload.type === 'dice') {
          // 初次关联内部实例，项目/应用信息同步至资源关联关系，重新拉取详情
          apiMarketStore.effects.getAssetDetail({ assetID: payload.assetID }, true);
        }
      }
      update({ instance: res.instantiation || {}, instancePermission: res.permission || {} });
    },
    async getInstance({ call, update }, payload: API_MARKET.QueryInstance) {
      update({ instance: {} as API_MARKET.Instantiation });
      const res = await call(getInstances, payload);
      update({ instance: res.instantiation || {}, instancePermission: res.permission || {} });
      return res;
    },
    async runAttemptTest({ call }, payload: API_MARKET.RunAttemptTest) {
      const res = await call(runAttemptTest, payload);
      return res;
    },
  },
  reducers: {
    clearVersionTree(state) {
      state.versionTree = [];
    },
    resetAssetList(state) {
      state.assetList = [];
      state.assetListPaging = getDefaultPaging();
    },
    resetVersionList(state) {
      state.assetVersionList = [];
    },
    clearAssetDetail(state) {
      state.assetDetail = {
        asset: {},
        permission: {},
      } as API_MARKET.AssetDetail;
    },
    clearState(state, { key, value }) {
      state[key] = value;
    },
  },
});

export default apiMarketStore;
