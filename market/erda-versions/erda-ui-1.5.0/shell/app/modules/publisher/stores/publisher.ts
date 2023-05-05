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

import breadcrumbStore from 'app/layout/stores/breadcrumb';
import { getDefaultPaging } from 'common/utils';
import { createStore } from 'core/cube';
import orgStore from 'app/org-home/stores/org';
import i18n from 'i18n';
import {
  getPublisherList,
  addPublisher,
  updatePublisher,
  deletePublisher,
  getPublisherDetail,
  getJoinedPublisherList,
  getArtifactsList,
  getJoinedArtifactsList,
  addArtifacts,
  updateArtifacts,
  deleteArtifacts,
  getVersionList,
  addVersion,
  publicVersion,
  unpublicVersion,
  setDefaultVersion,
  getArtifactDetail,
  getLibVersion,
  setGrayAndPublish,
  getOnlineVersionData,
  getH5PackageNames,
  getAKAIByPublishItemId,
  uploadOfflinePackage,
} from '../services/publisher';

interface IState {
  publisherList: PUBLISHER.IPublisher[];
  publisherPaging: IPaging;
  joinedPublisherList: PUBLISHER.IPublisher[];
  joinedPublisherPaging: IPaging;
  publisherDetail: PUBLISHER.IPublisher | undefined;
  artifactsList: PUBLISHER.IArtifacts[];
  artifactsPaging: IPaging;
  joinedArtifactsList: PUBLISHER.IArtifacts[];
  joinedArtifactsPaging: IPaging;
  versionList: PUBLISHER.IVersion[];
  versionPaging: IPaging;
  artifactsDetail: PUBLISHER.IArtifacts;
  onlineVersionData: PUBLISHER.IOnlineVersion[];
  publishItemMonitors: Record<string, PUBLISHER.MonitorItem>;
}

const initState: IState = {
  publisherList: [],
  publisherPaging: getDefaultPaging(),
  joinedPublisherList: [],
  joinedPublisherPaging: getDefaultPaging(),
  publisherDetail: undefined,
  artifactsList: [],
  artifactsPaging: getDefaultPaging(),
  joinedArtifactsList: [],
  joinedArtifactsPaging: getDefaultPaging(),
  versionList: [],
  versionPaging: getDefaultPaging(),
  artifactsDetail: {} as PUBLISHER.IArtifacts,
  onlineVersionData: [] as PUBLISHER.IOnlineVersion[],
  publishItemMonitors: {} as Record<string, PUBLISHER.MonitorItem>,
};

const publisher = createStore({
  name: 'publisher',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, isLeaving, params }: IRouteInfo) => {
      const { publisherItemId } = params;
      if (isEntering('publisher')) {
        publisher.effects.getArtifactDetail(publisherItemId);
        publisher.effects.getAKAIByPublishItemId({ publishItemId: publisherItemId });
      } else if (isLeaving('publisher')) {
        publisher.reducers.clearArtifactsDetail();
      }
    });
  },
  effects: {
    // 发布仓库
    async getPublisherList({ call, update, select }, payload: Parameters<typeof getPublisherList>[0]) {
      const { loadMore, ...rest } = payload;
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const result = await call(getPublisherList, { ...rest, orgId }, { paging: { key: 'publisherPaging' } });
      let publisherList = select((state) => state.publisherList);
      if (loadMore && rest.pageNo !== 1) {
        publisherList = publisherList.concat(result.list);
      } else {
        publisherList = result.list;
      }
      update({ publisherList });
      return { ...result };
    },
    async getJoinedPublisherList({ call, update, select }, payload: Parameters<typeof getJoinedPublisherList>[0]) {
      const { loadMore, ...rest } = payload;
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const result = await call(
        getJoinedPublisherList,
        { ...rest, orgId },
        { paging: { key: 'joinedPublisherPaging' } },
      );
      let joinedPublisherList = select((state) => state.joinedPublisherList);
      if (loadMore && rest.pageNo !== 1) {
        joinedPublisherList = joinedPublisherList.concat(result.list);
      } else {
        joinedPublisherList = result.list;
      }
      update({ joinedPublisherList });
      return { ...result };
    },
    async addPublisherList({ call }, payload: Parameters<typeof addPublisher>[0]) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(addPublisher, { ...payload, orgId }, { successMsg: i18n.t('added successfully') });
      return res;
    },
    async updatePublisher({ call }, payload: Parameters<typeof updatePublisher>[0]) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const res = await call(
        updatePublisher,
        { ...payload, orgId },
        { successMsg: i18n.t('dop:modified successfully') },
      );
      return res;
    },
    async deletePublisher({ call }, payload: Parameters<typeof deletePublisher>[0]) {
      const res = await call(deletePublisher, payload, { successMsg: i18n.t('deleted successfully') });
      return res;
    },
    async getPublisherDetail({ call, update }, payload: Parameters<typeof getPublisherDetail>[0]) {
      const res = await call(getPublisherDetail, payload);
      update({ publisherDetail: res });
      breadcrumbStore.reducers.setInfo('publisherName', res.name);
      return res;
    },
    // 发布内容
    async getArtifactsList(
      { call, update, select },
      payload: Merge<Parameters<typeof getArtifactsList>[0], 'loadMore'>,
    ) {
      const { loadMore, ...rest } = payload;
      const result = await call(getArtifactsList, { ...rest }, { paging: { key: 'artifactsPaging' } });
      let artifactsList = select((state) => state.artifactsList);
      if (loadMore && rest.pageNo !== 1) {
        artifactsList = artifactsList.concat(result.list);
      } else {
        artifactsList = result.list;
      }
      update({ artifactsList });
      return { ...result };
    },
    // 发布内容
    async getJoinedArtifactsList(
      { call, update, select },
      payload: Merge<Parameters<typeof getJoinedArtifactsList>[0], 'loadMore'>,
    ) {
      const { loadMore, ...rest } = payload;
      const result = await call(getJoinedArtifactsList, { ...rest }, { paging: { key: 'joinedArtifactsPaging' } });
      let joinedArtifactsList = select((state) => state.joinedArtifactsList);
      if (loadMore && rest.pageNo !== 1) {
        joinedArtifactsList = joinedArtifactsList.concat(result.list);
      } else {
        joinedArtifactsList = result.list;
      }
      update({ joinedArtifactsList });
      return { ...result };
    },
    async addArtifacts({ call }, payload) {
      const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);
      const res = await call(
        addArtifacts,
        { ...payload, publisherId: Number(publisherId) },
        { successMsg: i18n.t('added successfully') },
      );
      return res;
    },
    async updateArtifacts({ call }, payload: Parameters<typeof updateArtifacts>[0]) {
      const res = await call(updateArtifacts, payload, { successMsg: i18n.t('dop:modified successfully') });
      return res;
    },
    async deleteArtifacts({ call }, payload: Parameters<typeof deleteArtifacts>[0]) {
      const res = await call(deleteArtifacts, payload, { successMsg: i18n.t('deleted successfully') });
      return res;
    },
    // 版本
    async getVersionList({ call, update, select }, payload: Parameters<typeof getVersionList>[0]) {
      const [prevList, versionPaging] = select((s) => [s.versionList, s.versionPaging]);
      try {
        const result = await call(getVersionList, payload, { paging: { key: 'versionPaging' } });
        const newList = payload.pageNo && payload.pageNo > 1 ? prevList.concat(result.list) : result.list;
        update({ versionList: newList });
        return { ...result };
      } catch (e) {
        update({ versionPaging: { ...versionPaging, hasMore: false } });
        return { list: [], total: 0 };
      }
    },
    async addVersion({ call }, payload: Parameters<typeof addVersion>[0]) {
      const res = await call(addVersion, payload, { successMsg: i18n.t('added successfully') });
      return res;
    },
    // 设置灰度值
    async setGrayAndPublish({ call }, payload: Parameters<typeof setGrayAndPublish>[0]) {
      const res = await call(setGrayAndPublish, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    // 获取线上版本信息
    async getOnlineVersionData({ call, update }, payload: Parameters<typeof getOnlineVersionData>[0]) {
      const { list } = await call(getOnlineVersionData, payload);
      update({ onlineVersionData: list || [] });
    },
    async publicVersion({ call }, payload: Parameters<typeof publicVersion>[0]) {
      const res = await call(publicVersion, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    async unpublicVersion({ call }, payload: Parameters<typeof unpublicVersion>[0]) {
      const res = await call(unpublicVersion, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    async setDefaultVersion({ call }, payload: Parameters<typeof setDefaultVersion>[0]) {
      const res = await call(setDefaultVersion, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    async getArtifactDetail({ call, update }, payload: number | string) {
      const artifactsDetail = await call(getArtifactDetail, payload);
      breadcrumbStore.reducers.setInfo('publishModuleName', artifactsDetail.name);
      update({ artifactsDetail });
    },
    async getLibVersion({ call }, payload: PUBLISHER.QueryLibVersion) {
      const res = await call(getLibVersion, payload);
      return res;
    },
    async getH5PackageNames({ call }, payload: { publishItemId: string }) {
      const res = await call(getH5PackageNames, payload);
      return res;
    },
    async getAKAIByPublishItemId({ call, update }, payload: { publishItemId: string }) {
      const publishItemMonitors = await call(getAKAIByPublishItemId, payload);
      update({ publishItemMonitors });
    },
    async uploadOfflinePackage({ call }, payload: PUBLISHER.OfflinePackage) {
      const res = await call(uploadOfflinePackage, payload);
      return res;
    },
  },
  reducers: {
    clearPublisherList(state) {
      state.publisherList = [];
      state.publisherPaging = getDefaultPaging();
    },
    clearJoinedPublisherList(state) {
      state.joinedPublisherList = [];
      state.joinedPublisherPaging = getDefaultPaging();
    },
    clearArtifactsList(state) {
      state.artifactsList = [];
      state.artifactsPaging = getDefaultPaging();
    },
    clearJoinedArtifactsList(state) {
      state.joinedArtifactsList = [];
      state.joinedArtifactsPaging = getDefaultPaging();
    },
    clearVersionList(state) {
      state.versionList = [];
      state.versionPaging = getDefaultPaging();
    },
    clearPublisherDetail(state) {
      state.publisherDetail = undefined;
    },
    clearArtifactsDetail(state) {
      state.artifactsDetail = {} as PUBLISHER.IArtifacts;
    },
  },
});

export default publisher;
