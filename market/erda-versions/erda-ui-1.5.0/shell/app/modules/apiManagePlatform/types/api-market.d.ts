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

declare namespace API_MARKET {
  type SpecProtocol = 'oas2-json' | 'oas2-yaml' | 'oas3-json' | 'oas3-yaml';
  type AssetScope = 'all' | 'mine';
  type InstanceType = 'dice' | 'external';
  type PreviewTabs = 'Request' | 'Response';
  type ResponseTabs = 'header' | 'body';
  type RequestTabs = 'params' | 'header' | 'body';
  type Schema = 'https' | 'http';

  interface QueryAssets {
    pageNo?: number;
    pageSize?: number;
    keyword?: string;
    scope: AssetScope;
    hasProject: boolean;
    paging: boolean;
    latestVersion: boolean;
    latestSpec: boolean;
  }

  interface CommonData<T> {
    list: T;
    total: number;
  }

  interface CommonResList<T> {
    success: boolean;
    data: CommonData<T>;
  }

  type CommonQueryAssets = Omit<QueryAssets, 'pageNo' | 'pageSize' | 'keyword' | 'scope'>;

  interface AssetPermission {
    manage: boolean;
    addVersion: string;
    hasAccess: boolean;
  }

  interface Asset {
    id: number;
    assetID: string;
    assetName: string;
    desc: string;
    logo: string;
    versions: null;
    orgID: number;
    projectID: number;
    appID: number;
    creatorID: string;
    updaterID: string;
    createdAt: string;
    updatedAt: string;
    public: boolean;
    curMajor: number;
    curMinor: number;
    curPatch: number;
    projectName: string;
    appName: string;
  }

  interface AssetPermission {
    delete: boolean;
    edit: boolean;
    public: boolean;
    request: boolean;
  }

  interface AssetDetail {
    asset: Asset;
    permission: AssetPermission;
  }

  interface AssetVersion {
    id: number;
    orgID: number;
    assetID: string;
    assetName: string;
    major: number;
    minor: number;
    patch: number;
    desc: string;
    specProtocol: SpecProtocol;
    spec: string;
    creatorID: string;
    updaterID: string;
    createdAt: string;
    updatedAt: string;
    swaggerVersion: string;
    deprecated: boolean;
  }

  interface VersionItem {
    version: AssetVersion;
    spec: string;
    permission: {
      edit: boolean;
      delete: boolean;
    };
  }

  interface AssetSpec {
    id: number;
    orgID: number;
    assetID: string;
    versionID: number;
    specProtocol: SpecProtocol;
    spec: string;
    creatorID: string;
    updaterID: string;
    createdAt: string;
  }

  interface AssetListItem {
    asset: Asset;
    latestVersion: AssetVersion;
    latestSpec: AssetSpec;
    permission: AssetPermission;
  }

  interface CreateAsset {
    orgID: number;
    logo: string;
    assetID?: string;
    assetName: string;
    desc: string;
    versions: Array<{
      majorVersion?: number;
      minorVersion?: number;
      patchVersion?: number;
      specProtocol: SpecProtocol;
      specDiceFileUUID: string;
    }>;
  }

  interface UpdateAsset {
    assetID: string;
    assetName?: string;
    desc?: string;
    logo?: string;
    public?: boolean;
    projectID?: number;
    projectName?: string;
    appID?: number;
    appName?: string;
  }

  interface AssetVersionItem<T> {
    spec: T;
  }

  interface AssetSpec {
    assetID: string;
    assetName: string;
    createdAt: string;
    creatorID: string;
    desc: string;
    id: number;
    major: number;
    minor: number;
    orgID: number;
    patch: number;
    specProtocol: SpecProtocol;
    swaggerVersion: string;
    updatedAt: string;
    updaterID: string;
  }

  interface AssetVersionDetail {
    access: API_ACCESS.Access;
    asset: Asset;
    version: AssetVersion;
    spec: AssetSpec;
    hasInstantiation: boolean;
    hasAccess: boolean;
  }

  interface QueryVersionDetail {
    assetID: string;
    versionID: number;
    asset: boolean;
    spec: boolean;
  }

  interface DelVersion {
    assetID: string;
    version: string;
  }

  interface CreateAssetFormPipeline {
    spec: string;
    version: string;
    userID: number;
    logoUrl: string;
    assetID: string;
    runtimeID: number;
    specProtocol: string;
    description: string;
    serviceName: string;
    assetName: string;
  }

  interface QAsset {
    assetID: string;
  }

  interface QVersion {
    assetID: string;
    versionID: number;
  }

  interface UpdateAssetVersion extends QVersion {
    deprecated: boolean;
  }

  interface QueryVersionTree {
    assetID: string;
    patch: boolean;
    instantiation: boolean;
    access: boolean;
  }

  interface QueryVersionsList {
    assetID: string;
    major: number;
    minor: number;
    spec: boolean;
  }

  interface Response<T> {
    [k: string]: any;

    success: boolean;
    data: T;
    err: {
      code: string;
      msg: string;
    };
  }

  interface CreateVersion {
    assetID: string;
    major?: number;
    minor?: number;
    patch?: number;
    specProtocol: SpecProtocol;
    specDiceFileUUID: string;
  }

  interface VersionListItem {
    swaggerVersion: string;
    major: number;
    minor: number;
    patch: number;
  }

  interface AllVersion {
    assetID: string;
    orgID: number;
    total: number;
    list: VersionListItem[];
  }

  interface VersionTreeChild {
    id: number;
    major: number;
    minor: number;
    patch: number;
    deprecated: boolean;
  }

  interface VersionTreeItem {
    swaggerVersion: string;
    versions: VersionTreeChild[];
  }

  interface VersionTree {
    total: number;
    list: VersionTreeItem[];
  }

  interface exportSwagger {
    assetID: string;
    versionID: number;
    specProtocol: SpecProtocol;
  }

  interface RelationInstance {
    swaggerVersion: string;
    assetID: string;
    minor: number;
    major: number;
    type: InstanceType;
    url: string;
    projectID: number;
    appID: number;
    runtimeID: number;
    serviceName: string;
    workspace: API_ACCESS.Workspace;
  }

  interface UpdateInstance extends RelationInstance {
    instantiationID: number;
  }

  interface Instantiation {
    id: number;
    orgID: string;
    name: string;
    assetID: string;
    swaggerVersion: string;
    major: number;
    minor: number;
    type: 'dice' | 'external';
    url: string;
    creatorID: string;
    updaterID: string;
    createdAt: string;
    updatedAt: string;
    projectID: number;
    appID: number;
    serviceName: string;
    runtimeID: number;
    workspace: API_ACCESS.Workspace;
    projectName: string;
  }

  interface InstantiationPermission {
    edit: boolean;
  }

  interface IInstantiation {
    instantiation: Instantiation;
    permission: InstantiationPermission;
  }

  interface QueryInstance {
    assetID: string;
    swaggerVersion: string;
    major: number;
    minor: number;
  }

  interface BaseParams {
    [k: string]: any;

    key: string;
    value: any;
  }

  interface RunAttemptTest {
    assetID: string;
    clientID: string;
    clientSecret: string;
    swaggerVersion: string;
    apis: Array<{
      schema: Schema;
      method: string;
      url: string;
      params: BaseParams[];
      header: BaseParams[];
      body: {
        type: string;
        content: any;
      };
    }>;
  }

  interface RunAttemptTestResponse {
    request: {
      url: string;
      method: string;
    };
    response: {
      status: number;
      headers: Array<{
        [k: string]: any;
      }>;
      body: string;
    };
  }

  interface AppInstanceItem {
    runtimeID: number;
    runtimeName: string;
    workspace: API_ACCESS.Workspace;
    projectID: number;
    appID: number;
    serviceName: string;
    serviceAddr: string[];
    serviceExpose: string[];
  }
}
