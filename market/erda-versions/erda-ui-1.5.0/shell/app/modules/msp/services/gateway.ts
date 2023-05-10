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

export const getAuthInfo = ({ packageId, apiId }: GATEWAY.GetAuthInfo): GATEWAY.AuthInfoItem[] => {
  return agent
    .get(`/api/gateway/openapi/packages/${packageId}/apis/${apiId}/authz`)
    .then((response: any) => response.body);
};

export const updateAuthInfo = ({ packageId, apiId, consumers }: GATEWAY.UpdateAuthInfo): boolean => {
  return agent
    .post(`/api/gateway/openapi/packages/${packageId}/apis/${apiId}/authz`)
    .send({ consumers })
    .then((response: any) => response.body);
};

export const getConsumer = (params: Merge<GATEWAY.Base, { az: string }>): GATEWAY.Consumer => {
  return agent
    .get('/api/gateway/consumer')
    .query(params)
    .then((response: any) => response.body);
};

export const getRegisterApps = (params: GATEWAY.GetRegisterApp): { apps: GATEWAY.RegisterApp[] } => {
  return agent
    .get('/api/gateway/register-apps')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getApiPackageList = (
  params: Merge<GATEWAY.Base, Partial<GATEWAY.Query>>,
): IPagingResp<GATEWAY.ApiPackageItem> => {
  return agent
    .get('/api/gateway/openapi/packages')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const createApiPackage = ({
  orgId,
  projectId,
  env,
  scene,
  bindDomain,
  name,
  authType,
  aclType,
  description,
  needBindCloudapi,
}: GATEWAY.CreatApiPackege): GATEWAY.ApiPackageItem => {
  return agent
    .post('/api/gateway/openapi/packages')
    .query({ orgId, projectId, env })
    .send({ name, description, scene, bindDomain, authType, aclType, needBindCloudapi })
    .then((response: any) => response.body);
};

export const updateApiPackage = ({
  id,
  scene,
  bindDomain,
  name,
  authType,
  aclType,
  description,
  needBindCloudapi,
}: GATEWAY.UpdataApiPackage): GATEWAY.ApiPackageItem => {
  return agent
    .patch(`/api/gateway/openapi/packages/${id}`)
    .send({ name, description, scene, bindDomain, authType, aclType, needBindCloudapi })
    .then((response: any) => response.body);
};

export const deletePackage = ({ packageId }: { packageId: number }): boolean => {
  return agent.delete(`/api/gateway/openapi/packages/${packageId}`).then((response: any) => response.body);
};

// 调用方授权
export const getConsumerAuthorizes = ({ packageId }: GATEWAY.Package): GATEWAY.AuthInfoItem[] => {
  return agent.get(`/api/gateway/openapi/packages/${packageId}/consumers`).then((response: any) => response.body);
};

export const getApiPackageDetail = ({ id }: { id: string }): GATEWAY.ApiPackageItem => {
  return agent.get(`/api/gateway/openapi/packages/${id}`).then((response: any) => response.body);
};

export const createOpenApiConsumer = ({ description, name, ...restParams }: GATEWAY.CreateOpenApiConsumer): string => {
  return agent
    .post('/api/gateway/openapi/consumers')
    .query(restParams)
    .send({ name, description })
    .then((response: any) => response.body);
};

export const updateConsumerAuthorizes = ({ packageId, data }: GATEWAY.UpdateConsumerAuthByConsumers): boolean => {
  return agent
    .post(`/api/gateway/openapi/packages/${packageId}/consumers`)
    .send(data)
    .then((response: any) => response.body);
};

export const getPackageDetailApiList = ({
  packageId,
  ...restParams
}: GATEWAY.GetPackageDetailApiList): IPagingResp<GATEWAY.PackageDetailApiListItem> => {
  return agent
    .get(`/api/gateway/openapi/packages/${packageId}/apis`)
    .query({ ...restParams })
    .then((response: any) => response.body);
};

export const getImportableApiList = ({
  packageId,
  ...restParams
}: Merge<GATEWAY.GetImportApi, GATEWAY.Package>): { apis: GATEWAY.ImportApiItem[]; routePrefix: string } => {
  return agent
    .get(`/api/gateway/openapi/packages/${packageId}/loadserver`)
    .query({ ...restParams })
    .then((response: any) => response.body);
};

export const importApis = ({ packageId, ...restParams }: { [k: string]: any; packageId: number }) => {
  return agent
    .post(`/api/gateway/openapi/packages/${packageId}/loadserver`)
    .send({ ...restParams })
    .then((response: any) => response.body);
};

export const createPackageApi = ({ packageId, ...rest }: Merge<GATEWAY.CreatePackageApi, GATEWAY.Package>) => {
  return agent
    .post(`/api/gateway/openapi/packages/${packageId}/apis`)
    .send(rest)
    .then((response: any) => response.body);
};

export const updatePackageApi = ({
  packageId,
  apiId,
  ...rest
}: Merge<GATEWAY.CreatePackageApi, { packageId: string; apiId: string }>) => {
  return agent
    .patch(`/api/gateway/openapi/packages/${packageId}/apis/${apiId}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deletePackageApi = ({ packageId, apiId }: { packageId: string; apiId: string }): boolean => {
  return agent
    .delete(`/api/gateway/openapi/packages/${packageId}/apis/${apiId}`)
    .then((response: any) => response.body);
};

export const getServiceRuntime = (params: GATEWAY.GetRuntimeDetail): GATEWAY.RuntimeDetail[] => {
  return agent
    .get('/api/gateway/openapi/service-runtime')
    .query(params)
    .then((response: any) => response.body);
};

export const getApiPackages = (params: GATEWAY.Base): GATEWAY.ApiPackageItem[] => {
  return agent
    .get('/api/gateway/openapi/packages-name')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getApiConsumers = (params: GATEWAY.Base): GATEWAY.ConsumersName[] => {
  return agent
    .get('/api/gateway/openapi/consumers-name')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getOpenApiConsumerList = (
  params: Merge<GATEWAY.Base, Partial<GATEWAY.Query>>,
): IPagingResp<GATEWAY.ConsumersName> => {
  return agent
    .get('/api/gateway/openapi/consumers')
    .query({ ...params })
    .then((response: any) => response.body);
};
export const getConsumerAuthPackages = ({ consumerId }: { consumerId: string }): GATEWAY.ConsumerAuthPackages[] => {
  return agent.get(`/api/gateway/openapi/consumers/${consumerId}/packages`).then((response: any) => response.body);
};
export const updateConsumerAuthPackages = ({ consumerId, ...rest }: GATEWAY.UpdateConsumerAuthByPackages): boolean => {
  return agent
    .post(`/api/gateway/openapi/consumers/${consumerId}/packages`)
    .send({ ...rest })
    .then((response: any) => response.body);
};
export const updateOpenApiConsumer = ({
  id,
  description,
}: {
  id: string;
  description: string;
}): GATEWAY.ConsumersName => {
  return agent
    .patch(`/api/gateway/openapi/consumers/${id}`)
    .send({ description })
    .then((response: any) => response.body);
};

export const getConsumerDetail = (consumerId: string): GATEWAY.ConsumerDetail => {
  return agent.get(`/api/gateway/consumer/${consumerId}`).then((response: any) => response.body);
};

export const getConsumerCredentials = ({ consumerId }: { consumerId: string }): GATEWAY.ConsumerDetail => {
  return agent.get(`/api/gateway/openapi/consumers/${consumerId}/credentials`).then((response: any) => response.body);
};
export const updateConsumerCredentials = ({
  authConfig,
  consumerId,
}: GATEWAY.UpdateCredentials): GATEWAY.IAuthConfig => {
  return agent
    .post(`/api/gateway/openapi/consumers/${consumerId}/credentials`)
    .send({ authConfig })
    .then((response: any) => response.body);
};

export const deleteOpenApiConsumer = ({ consumerId }: { consumerId: string }): boolean => {
  return agent.delete(`/api/gateway/openapi/consumers/${consumerId}`).then((response: any) => response.body);
};

export const getPolicyList = ({
  category,
  ...params
}: Merge<GATEWAY.Base, { category: string }>): {
  category: string;
  description: string;
  policyList: GATEWAY.PolicyListItem[];
} => {
  return agent
    .get(`/api/gateway/policies/${category}`)
    .query(params)
    .then((response: any) => response.body);
};

export const getAPIList = (params: Merge<GATEWAY.GetApiList, GATEWAY.ApiFilter>): GATEWAY.ApiResponse => {
  return agent
    .get('/api/gateway/api')
    .query(params)
    .then((response: any) => response.body);
};

export const addAPI = (data: GATEWAY.AddAPI): { apiId: string } => {
  return agent
    .post('/api/gateway/api')
    .send(data)
    .then((response: any) => response.body);
};

export const updateAPI = (data: Merge<GATEWAY.AddAPI, { apiId: string }>): GATEWAY.ApiListItem => {
  return agent
    .patch(`/api/gateway/api/${data.apiId}`)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteAPI = (apiId: string): boolean => {
  return agent.delete(`/api/gateway/api/${apiId}`).then((response: any) => response.body);
};

export const getApiDomain = (params: GATEWAY.GetDomain): GATEWAY.ApiDomain => {
  return agent
    .get('/api/gateway/domain')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const saveApiDomain = (params: GATEWAY.SaveDomain): GATEWAY.ApiDomain => {
  const { domainPrefix, domainSuffix, ...query } = params;
  return agent
    .put('/api/gateway/domain')
    .query({ ...query })
    .send({ domainPrefix, domainSuffix })
    .then((response: any) => response.body);
};

export const getDeployedBranches = (params: GATEWAY.GetDomain): PROJECT.Detail => {
  return agent
    .get('/api/gateway/service-runtime')
    .query(params)
    .then((response: any) => response.body);
};

export const getApiLimits = ({
  apiConsumer,
  apiPackage,
  ...rest
}: GATEWAY.GetApiLimit): IPagingResp<GATEWAY.ApiLimitsItem> => {
  return agent
    .get('/api/gateway/openapi/limits')
    .query({ consumerId: apiConsumer, packageId: apiPackage, ...rest })
    .then((response: any) => response.body);
};

export const createApiLimit = ({
  orgId,
  projectId,
  env,
  ...rest
}: Merge<GATEWAY.updateLimit, GATEWAY.Base>): boolean => {
  return agent
    .post('/api/gateway/openapi/limits')
    .query({ orgId, projectId, env })
    .send(rest)
    .then((response: any) => response.body);
};

export const updateApiLimit = ({
  ruleId,
  ...rest
}: Merge<GATEWAY.updateLimit, { ruleId: string }>): GATEWAY.ApiLimitsItem => {
  return agent
    .patch(`/api/gateway/openapi/limits/${ruleId}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteLimit = ({ ruleId }: { ruleId: string }): boolean => {
  return agent.delete(`/api/gateway/openapi/limits/${ruleId}`).then((response: any) => response.body);
};

export const getSafetyWaf = (params: GATEWAY.GetSafety): GATEWAY.SafetyWaf => {
  return agent
    .get('/api/gateway/policies/safety-waf')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getSafetyIP = (params: GATEWAY.GetSafety): GATEWAY.SafetyIp => {
  return agent
    .get('/api/gateway/policies/safety-ip')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getSafetyServerGuard = (params: GATEWAY.GetSafety): GATEWAY.SafetyServerGuard => {
  return agent
    .get('/api/gateway/policies/safety-server-guard')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getSafetyCsrf = (params: GATEWAY.GetSafety): GATEWAY.SafetyCsrf => {
  return agent
    .get('/api/gateway/policies/safety-csrf')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getBusinessProxy = (params: GATEWAY.GetBusiness): GATEWAY.BusinessProxy => {
  return agent
    .get('/api/gateway/policies/proxy')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getBusinessCors = (params: GATEWAY.GetBusiness): GATEWAY.BusinessCors => {
  return agent
    .get('/api/gateway/policies/cors')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const getBusinessCustom = (params: GATEWAY.GetBusiness): GATEWAY.BusinessCustom => {
  return agent
    .get('/api/gateway/policies/custom')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const saveSafetyWaf = (params: GATEWAY.SaveWrf): GATEWAY.SafetyWaf => {
  const { wafEnable, removeRules, enable, global, ...query } = params;
  return agent
    .put('/api/gateway/policies/safety-waf')
    .query({ ...query })
    .send({ wafEnable, removeRules, switch: enable, global })
    .then((response: any) => response.body);
};

export const saveSafetyIP = (params: GATEWAY.SaveIp): GATEWAY.SafetyIp => {
  const { global, ipEnable, ipSource, ipAclType, ipAclList, ipMaxConnections, ipRate, ...query } = params;
  return agent
    .put('/api/gateway/policies/safety-ip')
    .query({ ...query })
    .send({ global, switch: ipEnable, ipSource, ipAclType, ipAclList, ipMaxConnections, ipRate })
    .then((response: any) => response.body);
};

export const saveSafetyServerGuard = (params: GATEWAY.SaveServerGuard): GATEWAY.SafetyServerGuard => {
  const { global, serverGuardEnable, maxTps, extraLatency, refuseCode, refuseResponse, ...query } = params;
  return agent
    .put('/api/gateway/policies/safety-server-guard')
    .query({ ...query })
    .send({ global, switch: serverGuardEnable, maxTps, extraLatency, refuseCode, refuseResponse })
    .then((response: any) => response.body);
};

export const saveSafetyCsrf = (params: GATEWAY.SaveCsrf): GATEWAY.SafetyCsrf => {
  const {
    global,
    csrfEnable,
    userCookie,
    excludedMethod,
    tokenName,
    tokenDomain,
    cookieSecure,
    validTTL,
    refreshTTL,
    errStatus,
    errMsg,
    ...query
  } = params;
  return agent
    .put('/api/gateway/policies/safety-csrf')
    .query({ ...query })
    .send({
      global,
      switch: csrfEnable,
      userCookie,
      excludedMethod,
      tokenName,
      tokenDomain,
      cookieSecure,
      validTTL,
      refreshTTL,
      errStatus,
      errMsg,
    })
    .then((response: any) => response.body);
};

export const saveBusinessProxy = (params: GATEWAY.SaveProxy): GATEWAY.BusinessProxy => {
  const { packageId, apiId, ...body } = params;
  return agent
    .put('/api/gateway/policies/proxy')
    .query({ packageId, apiId })
    .send(body)
    .then((response: any) => response.body);
};

export const saveBusinessCors = (params: GATEWAY.SaveCors): GATEWAY.BusinessCors => {
  const { packageId, apiId, ...body } = params;
  return agent
    .put('/api/gateway/policies/cors')
    .query({ packageId, apiId })
    .send(body)
    .then((response: any) => response.body);
};

export const saveBusinessCustom = (params: GATEWAY.SaveCustom): GATEWAY.BusinessCustom => {
  const { packageId, apiId, ...body } = params;
  return agent
    .put('/api/gateway/policies/custom')
    .query({ packageId, apiId })
    .send(body)
    .then((response: any) => response.body);
};

export const addPolicy = ({
  category,
  data,
}: {
  data: GATEWAY.UpdatePolicy;
  category: string;
}): { policyId: string } => {
  return agent
    .post(`/api/gateway/policies/${category}`)
    .send({ ...data, category })
    .then((response: any) => response.body);
};

export const updatePolicy = ({
  category,
  data,
}: {
  data: GATEWAY.UpdatePolicy;
  category: string;
}): GATEWAY.PolicyListItem => {
  return agent
    .patch(`/api/gateway/policies/${category}/${data.policyId}`)
    .send({ ...data, category })
    .then((response: any) => response.body);
};

export const deletePolicy = ({ category, data }: { data: { policyId: string }; category: string }): boolean => {
  return agent.delete(`/api/gateway/policies/${category}/${data.policyId}`).then((response: any) => response.body);
};

export const createConsumer = ({
  consumerName,
  orgId,
  projectId,
  env,
}: Merge<{ consumerName: string }, GATEWAY.Base>): { ConsumerId: string; ConsumerName: string } => {
  return agent
    .post('/api/gateway/consumer')
    .send({ consumerName, orgId, projectId, env })
    .then((response: any) => response.body);
};

export const getConsumerList = (params: GATEWAY.Base): { consumers: GATEWAY.IConsumer[] } => {
  return agent
    .get('/api/gateway/consumer-list')
    .query({ ...params })
    .then((response: any) => response.body);
};

export const updateConsumerDetail = ({ consumerId, data }: GATEWAY.updateConsumer): boolean => {
  return agent
    .patch(`/api/gateway/consumer/${consumerId}`)
    .send(data)
    .then((response: any) => response.body);
};
export const saveConsumerApi = (params: GATEWAY.SaveConsumerApi): boolean => {
  return agent
    .patch('/api/gateway/consumer')
    .send({ ...params })
    .then((response: any) => response.body);
};
export const saveConsumerApiPolicy = (params: GATEWAY.SavePoliciesApi): boolean => {
  return agent
    .patch('/api/gateway/consumer-api')
    .send({ ...params })
    .then((response: any) => response.body);
};
export const deleteConsumer = ({ consumerId }: { consumerId: string }): boolean => {
  return agent.delete(`/api/gateway/consumer/${consumerId}`).then((response: any) => response.body);
};

export const getAPISummary = (data: GATEWAY.Common): GATEWAY.Common => {
  return agent
    .get('/api/spot/kong/request_list')
    .query(data)
    .then((response: any) => response.body);
};

export const getStatusCode = (data: GATEWAY.Common): string[] => {
  return agent
    .get('/api/spot/kong/status_code')
    .query(data)
    .then((response: any) => response.body);
};

export const getStatusCodeChart = (data: GATEWAY.Common): GATEWAY.Common => {
  return agent
    .get('/api/spot/kong/status_code_chart')
    .query(data)
    .then((response: any) => response.body);
};

export const getErrorSummary = (data: GATEWAY.Common): GATEWAY.Common[] => {
  return agent
    .get('/api/spot/kong/error_percentage')
    .query(data)
    .then((response: any) => response.body);
};

export const getServiceApiPrefix = (payload: GATEWAY.QueryApiPrefix): string[] => {
  return agent
    .get('/api/gateway/openapi/service-api-prefix')
    .query(payload)
    .then((response: any) => response.body);
};
