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

export /**
 *获取节点列表 https://yuque.antfin-inc.com/terminus_paas_dev/vdffob/ud5xnm/edit
 *from 张海彬
 *
 * @param {{
 *     projectId: string;
 *     env: string;
 *     az: string;
 *     appid: string;
 *     tenantid?: string;
 *   }} { projectId, env, ...rest }
 * @returns {MS_ZK.INodeData}
 */
const getNodeList = ({
  projectId,
  env,
  ...rest
}: {
  projectId: string;
  env: string;
  az: string;
  appid: string;
  tenantid?: string;
}): MS_ZK.INodeData => {
  return agent
    .get(`/api/tmc/mesh/listhostinterface/${projectId}/${env}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const updateNodeRule = ({
  projectId,
  env,
  host,
  ruleData,
  ...rest
}: {
  projectId: string;
  env: string;
  host: string;
  az: string;
  tenantid?: string;
  ruleData: MS_ZK.INodeListItem;
}) => {
  return agent
    .post(`/api/tmc/mesh/rule/host/${projectId}/${env}/${host}`)
    .query(rest)
    .send(ruleData)
    .then((response: any) => response.body);
};

export const getAppDetail = (appId: number) => {
  return agent.get(`/api/applications/${appId}`).then((response: any) => response.body);
};

export const getRunTimes = (appId: number) => {
  return agent
    .get('/api/runtimes')
    .query({ applicationId: appId })
    .then((response: any) => response.body);
};

export const getBranchesRule = ({
  projectId,
  env,
  appId,
  az,
}: {
  projectId: number;
  env: string;
  appId: string;
  az: string;
}) => {
  return agent
    .get(`/api/tmc/mesh/rule/branch/${projectId}/${env}/${appId}`)
    .query({ az })
    .then((response: any) => response.body);
};

export const updateBranchesRule = ({
  projectId,
  env,
  appId,
  az,
  body,
}: {
  projectId: number;
  env: string;
  appId: string;
  az: string;
  body: any;
}) => {
  return agent
    .post(`/api/tmc/mesh/rule/branch/${projectId}/${env}/${appId}`)
    .query({ az })
    .send(body)
    .then((response: any) => response.body);
};

export const clearBranchesRule = ({
  projectId,
  env,
  appId,
  az,
}: {
  projectId: number;
  env: string;
  appId: string;
  az: string;
}) => {
  return agent
    .delete(`/api/tmc/mesh/rule/branch/${projectId}/${env}/${appId}`)
    .query({ az })
    .then((response: any) => response.body);
};

export const getZkInterfaceList = ({
  projectId,
  env,
  tenantGroup,
  az,
  runtimeId,
}: {
  projectId: number;
  env: string;
  tenantGroup: string;
  az: string;
  runtimeId: number;
}) => {
  return agent
    .get(`/api/tmc/mesh/listinterface/${projectId}/${env.toLocaleLowerCase()}`)
    .query({ az, runtimeId, tenantGroup })
    .then((response: any) => response.body);
};

// @deprecated
// export const getZkInterfaceConfig = (
//   { projectId, env, az, interfacename }:
//   { projectId: number, env: string, az: string, interfacename: string }
// ) => {
//   return agent.get(`/api/tmc/mesh/interface/route/${interfacename}/${projectId}/${env}`)
//     .query({ az })
//     .then((response: any) => response.body);
// };

// export const addZkInterfaceConfig = (
//   { projectId, env, az, interfacename, ...data }:
//   { projectId: number, env: string, az: string, interfacename: string }
// ) => {
//   return agent.post(`/api/tmc/mesh/interface/route/${interfacename}/${projectId}/${env}`)
//     .send(data)
//     .query({ az })
//     .then((response: any) => response.body);
// };

// export const deleteZkInterfaceConfig = (
//   { projectId, env, az, interfacename }:
//   { projectId: number, env: string, az: string, interfacename: string }
// ) => {
//   return agent.delete(`/api/tmc/mesh/interface/route/${interfacename}/${projectId}/${env}`)
//     .query({ az })
//     .then((response: any) => response.body);
// };

export const getClusterDetail = ({ clusterName }: { clusterName: string }) => {
  return agent.get(`/api/clusters/${clusterName}`).then((response: any) => response.body);
};

export const getServiceByIp = ({ projectID, workspace, ip }: MS_ZK.IServiceQuery) => {
  return agent
    .get('/api/tmc/service-ip')
    .query({ projectID, workspace, ip })
    .then((response: any) => response.body);
};
