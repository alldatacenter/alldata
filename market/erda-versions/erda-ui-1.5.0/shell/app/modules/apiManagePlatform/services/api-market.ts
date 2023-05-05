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
import { setApiWithOrg } from 'common/utils';

// API 资源列表
export const getApiAssetsList = (payload: API_MARKET.QueryAssets): IPagingResp<API_MARKET.AssetListItem> => {
  return agent
    .get('/api/api-assets')
    .query(payload)
    .then((response: any) => response.body);
};

// 创建 API 资源
export const createAsset = (payload: API_MARKET.CreateAsset) => {
  return agent
    .post('/api/api-assets')
    .send(payload)
    .then((response: any) => response.body);
};

export const updateAsset = (payload: API_MARKET.UpdateAsset) => {
  return agent
    .put(`/api/api-assets/${payload.assetID}`)
    .send(payload)
    .then((response: any) => response.body);
};

export const getListOfVersions = ({
  assetID,
  ...rest
}: API_MARKET.QueryVersionsList): { list: API_MARKET.VersionItem[] } => {
  return agent
    .get(`/api/api-assets/${assetID}/versions`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getAssetDetail = ({ assetID }: API_MARKET.QAsset): API_MARKET.AssetDetail => {
  return agent.get(`/api/api-assets/${assetID}`).then((response: any) => response.body);
};

export const deleteAsset = ({ assetID }: API_MARKET.QAsset) => {
  return agent.delete(`/api/api-assets/${assetID}`).then((response: any) => response.body);
};

export const updateAssetVersion = ({ assetID, versionID, ...rest }: API_MARKET.UpdateAssetVersion) => {
  return agent
    .put(`/api/api-assets/${assetID}/versions/${versionID}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteAssetVersion = ({ assetID, versionID }: API_MARKET.QVersion) => {
  return agent.delete(`/api/api-assets/${assetID}/versions/${versionID}`).then((response: any) => response.body);
};

export const addNewVersion = (payload: API_MARKET.CreateVersion): API_MARKET.AssetVersion => {
  return agent
    .post(`/api/api-assets/${payload.assetID}/versions`)
    .send(payload)
    .then((response: any) => response.body);
};

// 获取资源版本详情
export const getAssetVersionDetail = ({
  assetID,
  versionID,
  ...rest
}: API_MARKET.QueryVersionDetail): API_MARKET.AssetVersionDetail => {
  return agent
    .get(`/api/api-assets/${assetID}/versions/${versionID}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const exportSwagger = ({ assetID, versionID, specProtocol }: API_MARKET.exportSwagger) => {
  return setApiWithOrg(`/api/api-assets/${assetID}/versions/${versionID}/export?specProtocol=${specProtocol}`);
};

export const getVersionTree = <T = API_MARKET.VersionTree>({ assetID, ...rest }: API_MARKET.QueryVersionTree): T => {
  return agent
    .get(`/api/api-assets/${assetID}/swagger-versions`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getMyProject = <T>(payload: { pageSize: number; pageNo: number; q?: string }): T => {
  return agent
    .get('/api/projects/actions/list-my-projects')
    .query(payload)
    .then((response: any) => response.body);
};

export const getApps = <T>(payload: { pageSize: number; pageNo: number; projectId: number; q?: string }): T => {
  return agent
    .get('/api/applications/actions/list-my-applications')
    .query(payload)
    .then((response: any) => response.body);
};

export const getAppDetail = (applicationId: string | number): Promise<API_MARKET.Response<IApplication>> => {
  return agent.get(`/api/applications/${applicationId}`).then((response: any) => response.body);
};

export const getInstances = ({
  assetID,
  minor,
  swaggerVersion,
}: API_MARKET.QueryInstance): API_MARKET.IInstantiation => {
  return agent
    .get(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/minors/${minor}/instantiations`)
    .then((response: any) => response.body);
};

export const relationInstance = ({
  assetID,
  minor,
  swaggerVersion,
  ...rest
}: API_MARKET.RelationInstance): API_MARKET.IInstantiation => {
  return agent
    .post(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/minors/${minor}/instantiations`)
    .send({ ...rest })
    .then((response: any) => response.body);
};

export const updateInstance = ({
  assetID,
  minor,
  swaggerVersion,
  instantiationID,
  ...rest
}: API_MARKET.UpdateInstance): API_MARKET.IInstantiation => {
  return agent
    .put(
      `/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/minors/${minor}/instantiations/${instantiationID}`,
    )
    .send({ ...rest })
    .then((response: any) => response.body);
};

export const runAttemptTest = ({
  assetID,
  swaggerVersion,
  ...rest
}: API_MARKET.RunAttemptTest): API_MARKET.RunAttemptTestResponse => {
  return agent
    .post(`/api/api-assets/${assetID}/swagger-versions/${swaggerVersion}/attempt-test`)
    .send({ ...rest })
    .then((response: any) => response.body);
};

export const getAppInstance = <T = API_MARKET.AppInstanceItem[]>({ appID }: { appID: number }): T => {
  return agent.get(`/api/api-app-services/${appID}`).then((response: any) => response.body);
};
