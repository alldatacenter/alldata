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

import agent from 'agent';
// 发布仓库
export const getPublisherList = (payload: PUBLISHER.PublisherListQuery): IPagingResp<PUBLISHER.IPublisher> => {
  return agent
    .get('/api/publishers')
    .query(payload)
    .then((response: any) => response.body);
};

export const getJoinedPublisherList = (payload: PUBLISHER.PublisherListQuery): IPagingResp<PUBLISHER.IPublisher> => {
  return agent
    .get('/api/publishers/actions/list-my-publishers')
    .query(payload)
    .then((response: any) => response.body);
};

export const addPublisher = (data: PUBLISHER.IPublisher) => {
  return agent
    .post('/api/publishers')
    .send(data)
    .then((response: any) => response.body);
};

export const updatePublisher = (data: PUBLISHER.IPublisher) => {
  return agent
    .put('/api/publishers')
    .send(data)
    .then((response: any) => response.body);
};

export const deletePublisher = ({ publisherId }: { publisherId: string }) => {
  return agent.delete(`/api/publishers/${publisherId}`).then((response: any) => response.body);
};

export const getPublisherDetail = ({ publisherId }: { publisherId: string }): PUBLISHER.IPublisher => {
  return agent.get(`/api/publishers/${publisherId}`).then((response: any) => response.body);
};

// 发布内容
export const getArtifactsList = (query: PUBLISHER.ArtifactsListQuery): IPagingResp<PUBLISHER.IArtifacts> => {
  return agent
    .get('/api/publish-items')
    .query(query)
    .then((response: any) => response.body);
};

export const getJoinedArtifactsList = (query: PUBLISHER.ArtifactsListQuery): IPagingResp<PUBLISHER.IArtifacts> => {
  return agent
    .get('/api/my-publish-items')
    .query(query)
    .then((response: any) => response.body);
};

export const addArtifacts = (data: PUBLISHER.IArtifacts) => {
  return agent
    .post('/api/publish-items')
    .send(data)
    .then((response: any) => response.body);
};

export const updateArtifacts = ({ artifactsId, ...data }: { artifactsId: string }) => {
  return agent
    .put(`/api/publish-items/${artifactsId}`)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteArtifacts = ({ artifactsId }: { artifactsId: string }) => {
  return agent.delete(`/api/publish-items/${artifactsId}`).then((response: any) => response.body);
};

// 发布内容版本
export const getVersionList = (query: PUBLISHER.VersionListQuery): IPagingResp<PUBLISHER.IVersion> => {
  const { artifactsId, ...rest } = query;
  return agent
    .get(`/api/publish-items/${artifactsId}/versions`)
    .query(rest)
    .then((response: any) => response.body);
};

export const setGrayAndPublish = (query: PUBLISHER.IUpdateGrayQuery) => {
  const { action, ...rest } = query;
  return agent
    .post(`/api/publish-items/versions/actions/${action}`)
    .send(rest)
    .then((response: any) => response.body);
};

// 获取线上版本信息
export const getOnlineVersionData = (query: PUBLISHER.IOnlineVersionQuery): IPagingResp<PUBLISHER.IOnlineVersion> => {
  const { publishItemId, mobileType, packageName } = query;
  return agent
    .get(`/api/publish-items/${publishItemId}/versions/actions/public-version`)
    .query({ mobileType, packageName })
    .then((response: any) => response.body);
};

export const addVersion = (data: PUBLISHER.IVersion) => {
  const { artifactsId, ...rest } = data;
  return agent
    .post(`/api/publish-items/${artifactsId}/versions`)
    .send(rest)
    .then((response: any) => response.body);
};

export const publicVersion = ({ artifactsId, version }: { artifactsId: string; version: string }) => {
  return agent
    .post(`/api/publish-items/${artifactsId}/versions/${version}/actions/public`)
    .then((response: any) => response.body);
};

export const unpublicVersion = ({ artifactsId, version }: { artifactsId: string; version: string }) => {
  return agent
    .post(`/api/publish-items/${artifactsId}/versions/${version}/actions/unpublic`)
    .then((response: any) => response.body);
};

export const setDefaultVersion = ({ artifactsId, version }: { artifactsId: string; version: string }) => {
  return agent
    .post(`/api/publish-items/${artifactsId}/versions/${version}/actions/default`)
    .then((response: any) => response.body);
};

export const getArtifactDetail = (id: number | string): PUBLISHER.IArtifacts => {
  return agent.get(`/api/publish-items/${id}`).then((response: any) => response.body);
};

export const getLibVersion = (palyload: PUBLISHER.QueryLibVersion): PUBLISHER.LibVersion[] => {
  return agent
    .get('/api/lib-references/actions/fetch-versions')
    .query(palyload)
    .then((response: any) => response.body);
};

export const getH5PackageNames = ({ publishItemId }: { publishItemId: string }): string[] => {
  return agent
    .get(`/api/publish-items/${publishItemId}/versions/actions/get-h5-packagename`)
    .then((response: any) => response.body);
};

export const getAKAIByPublishItemId = ({
  publishItemId,
}: {
  publishItemId: string;
}): Record<string, PUBLISHER.MonitorItem> => {
  return agent.get(`/api/publish-items/${publishItemId}/list-monitor-keys`).then((response: any) => response.body);
};

export const uploadOfflinePackage = ({
  publishItemId,
  data,
}: PUBLISHER.OfflinePackage): PUBLISHER.OfflinePackageType => {
  return agent
    .post(`/api/publish-items/${publishItemId}/versions/create-offline-version`)
    .send(data)
    .then((response: any) => response.body);
};
