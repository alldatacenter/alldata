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

declare namespace GATEWAY {
  type EnvType = 'DEV' | 'TEST' | 'STAGING' | 'PROD';

  type LimitType = 'qps' | 'qpm' | 'qph' | 'qpd';

  type AuthType = 'key-auth' | 'oauth2' | 'sign-auth' | 'aliyun-app';

  interface GetAuthInfo {
    apiId: string;
    packageId: string;
  }

  interface Base {
    orgId: number;
    projectId: string;
    env: EnvType;
  }

  interface Query {
    pageSize: number;
    pageNo: number;
    domain?: string;
  }

  interface Package {
    packageId: string;
  }

  interface UpdateAuthInfo extends GetAuthInfo {
    consumers: string[];
  }

  interface RuntimeEntryData {
    diceApp: string;
    services: {
      [k: string]: RUNTIME_SERVICE.Detail;
    };
  }

  interface Endpoint {
    outerAddr: string;
    innerAddr: string;
    innerTips: string;
  }

  interface Consumer {
    endpoint: Endpoint;
    consumerId: string;
    consumerName: string;
    authConfig: any;
    gatewayInstance: string;
    clusterName: string;
  }

  interface AuthInfoItem {
    selected: boolean;
    id: string;
    name: string;
    description: string;
    createAt: string;
  }

  type GetRegisterApp = Base;

  interface RegisterApp {
    name: string;
    services: string[];
  }

  interface ApiPackageItem {
    id: string;
    createAt: string;
    name: string;
    bindDomain: string[] | null;
    authType: string;
    aclType: string;
    scene: string;
    description: string;
    needBindCloudapi: boolean;
  }

  interface CreateOpenApiConsumer extends Base {
    description: string;
    name: string;
  }

  interface UpdateConsumerAuthByConsumers {
    data: {
      consumers: string[];
    };
    packageId: string;
  }

  interface GetPackageDetailApiList {
    packageId: string;
    apiPath: string;
    method: string;
    sortField: string;
    sortType: string;
    diceApp: string;
    diceService: string;
    pageNo: number;
  }

  interface PackageDetailApiListItem {
    diceApp: string;
    diceService: string;
    apiPath: string;
    method: string;
    createAt: string;
  }

  interface ApiFilterCondition {
    apiPackages: ApiPackageItem[];
    apiConsumers: ConsumersName[];
  }

  interface ConsumersName {
    id: string;
    name: string;
    description: string;
    createAt: string;
  }

  type ConsumerAuthPackages = Merge<ApiPackageItem, { selected: boolean }>;

  interface UpdateConsumerAuthByPackages {
    consumerId: string;
    packages: string[];
  }

  interface IAuthData_data {
    consumer_id: string;
    created_at: number;
    id: string;
    key?: string;
    secret?: string;
    redirect_uri?: string[];
    name?: string;
    client_id?: string;
    client_secret?: string;
  }

  interface IAuthConfig {
    authType: string;
    authTips: string;
    authData: {
      total: number;
      data: IAuthData_data[];
    };
  }

  interface ConsumerDetail {
    consumerName: string;
    consumerId: string;
    authConfig: {
      auths: IAuthConfig[];
    };
    endpoint: Endpoint;
    gatewayInstance: string;
    clusterName: string;
  }

  interface PolicyListItem {
    category: string;
    policyId: string;
    policyName: string;
    displayName: string;
    config: {
      [k in LimitType]: number;
    };
    createAt: string;
  }

  interface ApiFilter {
    registerType?: string;
    netType?: string;
    method?: string;
    sortField?: string;
    sortType?: string;
    apiPath?: string;
  }

  interface GetApiList extends Base {
    page: number;
    size: string;
    diceService: string | undefined;
    diceApp: string | undefined;
    runtimeId: string | undefined;
    from?: string;
  }

  interface ApiListItem {
    apiId: string;
    path: string;
    displayPath: string;
    outerNetEnable: boolean;
    registerType: string;
    needAuth: boolean;
    method: string;
    description: string;
    redirectAddr: string;
    redirectPath: string;
    redirectType: string;
    monitorPath: string;
    group: {
      groupId: string;
      groupName: string;
      displayName: string;
    };
    createAt: string;
    policies: null | string[];
    swagger: {
      [key: string]: any;
    };
  }

  interface ApiPage {
    serialVersionUID: number;
    pageSize: number;
    curPage: number;
    totalNum: number;
    startIndex: number;
    endIndex: number;
  }

  interface ApiResponse {
    result: ApiListItem[];
    page: ApiPage;
  }

  interface AddAPI extends Base {
    consumerId: string;
    description: string;
    diceApp: string;
    diceService: string;
    method: string;
    path: string;
    policies: string[];
    redirectAddr?: string;
    redirectPath: string;
    redirectType: string;
    runtimeId: string;
  }

  interface ApiDomain {
    domainSuffix: string;
    domainPrefix: string;
  }

  interface GetDomain extends Base {
    diceService: string | undefined;
    diceApp: string | undefined;
  }

  type SaveDomain = Merge<GetDomain, ApiDomain>;

  interface GetRuntimeDetail extends Base {
    app: string | undefined;
    service: string | undefined;
  }

  interface RuntimeDetail {
    project_id: string;
    workspace: string;
    cluster_name: string;
    runtime_id: string;
    runtime_name: string;
    release_id: string;
    group_namespace: string;
    group_name: string;
    app_id: string;
    app_name: string;
    service_name: string;
    service_port: number;
    inner_address: string;
    use_apigw: number;
    is_endpoint: number;
    id: string;
    is_deleted: string;
    create_time: string;
    update_time: string;
  }

  interface GetImportApi {
    diceApp: string;
    diceService: string;
    method: string;
    apiPath: string;
  }

  interface ImportApiItem {
    apiPath: string;
    method: string;
    description: string;
    selected: boolean;
    apiId: string;
  }

  interface ApiLimitsItem {
    consumerId: string;
    packageId: string;
    method: string;
    apiPath: string;
    limit: {
      qps: number;
    };
    id: string;
    createAt: string;
    consumerName: string;
    packageName: string;
  }

  interface GetApiLimit extends Query {
    apiConsumer: string;
    apiPackage: string;
    orgId: number;
    packageId: string;
    env: EnvType;
  }

  interface updateLimit {
    consumerId: string;
    method?: string;
    apiPath: string;
    limit: {
      [k in LimitType]: number;
    };
    packageId: string;
  }

  interface GetSafety extends Package {
    apiId?: string;
  }

  interface GetBusiness extends Package {
    apiId?: string;
  }

  interface SafetyWaf {
    wafEnable: string;
    removeRules: string;
    switch: boolean;
    global: boolean;
  }

  interface SafetyIp {
    global: boolean;
    ipAclType: string;
    ipAclList: string[];
    ipSource: string;
    switch: boolean;
    ipMaxConnections?: number;
    ipRate?: {
      rate?: number;
      unit: string;
    };
  }

  interface SafetyServerGuard {
    global: boolean;
    maxTps: number;
    extraLatency: number;
    refuseCode: number;
    refuseResponse?: string;
    switch: boolean;
  }

  interface SafetyCsrf {
    global: boolean;
    switch: boolean;
    userCookie: string;
    excludedMethod: string[];
    tokenName: string;
    tokenDomain?: string;
    cookieSecure: boolean;
    validTTL: number;
    refreshTTL: number;
    errStatus: number;
    errMsg: string;
  }

  interface SaveIp extends GetSafety {
    global: boolean;
    ipEnable: boolean;
    ipSource: string;
    ipAclType: string;
    ipAclList: string[];
    ipMaxConnections: string;
    ipRate?: {
      rate: string;
      unit: string;
    };
  }

  interface SaveServerGuard extends GetSafety {
    global: boolean;
    serverGuardEnable: boolean;
    maxTps: number;
    extraLatency: number;
    refuseCode: number;
    refuseResponse: string;
  }

  interface SaveCsrf extends GetSafety {
    global: boolean;
    csrfEnable: boolean;
    userCookie: string;
    excludedMethod: string[];
    tokenName: string;
    tokenDomain: string;
    cookieSecure: string;
    validTTL: number;
    refreshTTL: number;
    errMsg: string;
    errStatus: number;
  }

  interface SaveWrf extends GetSafety {
    global: boolean;
    enable: boolean;
    removeRules: string;
    wafEnable: boolean;
  }

  interface BusinessProxy {
    switch: boolean;
    hostPassthrough: boolean;
    sslRedirect: boolean;
    reqBuffer: boolean;
    respBuffer: boolean;
    clientReqLimit: number;
    clientReqTimeout: number;
    clientRespTimeout: number;
    proxyConnTimeout: number;
    proxyReqTimeout: number;
    proxyRespTimeout: number;
    global: boolean;
  }

  interface BusinessCors {
    switch: boolean;
    methods: string;
    headers: string;
    origin: string;
    credentials: boolean;
    maxAge: number;
    global: boolean;
  }

  interface BusinessCustom {
    switch: boolean;
    config: string;
    global: boolean;
  }

  interface SaveProxy extends GetBusiness {
    switch: boolean;
    sslRedirect: boolean;
    hostPassthrough: boolean;
    reqBuffer: boolean;
    respBuffer: boolean;
    clientReqLimit: number;
    clientReqTimeout: number;
    clientRespTimeout: number;
    proxyReqTimeout: number;
    proxyRespTimeout: number;
    global: boolean;
  }

  interface SaveCors extends GetBusiness {
    switch: boolean;
    methods: string;
    headers: string;
    origin: string;
    credentials: boolean;
    maxAge: number;
    global: boolean;
  }

  interface SaveCustom extends GetBusiness {
    switch: boolean;
    global: boolean;
    config: string;
  }

  interface UpdatePolicy extends Base {
    policyId?: string;
    displayName: string;
    config: {
      [k in LimitType]: number;
    };
    policyName: string;
  }

  interface IConsumer {
    consumerId: string;
    consumerName: string;
    consumerApiInfos: any[];
  }

  interface CreatApiPackege extends Base {
    scene: string;
    bindDomain: string;
    name: string;
    authType: AuthType;
    aclType: string;
    description: string;
    needBindCloudapi: boolean;
  }

  interface UpdataApiPackage extends Omit<CreatApiPackege, keyof Base> {
    id: string;
  }

  interface CreatePackageApi {
    apiPath: string;
    redirectPath: string;
    redirectAddr: string;
    method: string;
    allowPassAuth: boolean;
    redirectType: string;
  }

  interface UpdateCredentials {
    consumerId: string;
    authConfig: {
      auths: IAuthConfig[];
    };
  }

  interface updateConsumer {
    data: {
      authConfig: {
        auths: Array<{
          authType: string;
          authData: {
            data: IAuthData_data[];
          };
        }>;
      };
    };
    consumerId: string;
  }

  interface SaveConsumerApi {
    consumerId: string;
    apiList: any[];
  }

  interface SavePoliciesApi {
    list: Array<{
      consumerApiId: string;
      policies: any;
    }>;
  }

  interface Common {
    [k: string]: any;
  }

  interface QueryApiPrefix extends Base {
    app: string;
    service: string;
    runtimeId: string;
  }
}
