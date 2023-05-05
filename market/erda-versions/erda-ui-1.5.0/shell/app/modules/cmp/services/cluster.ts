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

export const getClusterList = ({ orgId, sys }: { orgId?: number; sys?: boolean }): ORG_CLUSTER.ICluster[] => {
  return agent
    .get('/api/clusters')
    .query({ orgID: orgId, sys })
    .then((response: any) => response.body);
};

export const addCluster = (data: ORG_CLUSTER.IAddClusterQuery) => {
  return agent
    .post('/api/clusters')
    .query({ orgID: data.orgId })
    .send(data)
    .then((response: any) => response.body);
};

export const updateCluster = (data: ORG_CLUSTER.IAddClusterQuery) => {
  return agent
    .put('/api/clusters')
    .send(data)
    .then((response: any) => response.body);
};

export const getClusterDetail = ({
  clusterId,
  clusterName,
}: {
  clusterId?: number;
  clusterName?: string;
}): ORG_CLUSTER.ICluster => {
  return agent.get(`/api/clusters/${clusterId || clusterName}`).then((response: any) => response.body);
};

export const getClusterNewDetail = (query: { clusterName?: string }): ORG_CLUSTER.ICluster => {
  return agent
    .get('/api/cluster')
    .query(query)
    .then((response: any) => response.body);
};

// 部署集群
// 获取当前org在部署的物理集群，后端约定同一个时刻，org上只会有一个正在部署
export const getCurDeployCluster = (): any => {
  return agent.get('/api/deploy/cluster').then((response: any) => response.body);
};
// 请求开始部署物理集群
export const deployCluster = (data: any): any => {
  return agent
    .post('/api/deploy/cluster')
    .send(data)
    .then((response: any) => response.body);
};
// 请求部署集群的日志
export const getDeployClusterLog = (query: any): any => {
  return agent
    .get('/api/deploy/cluster/log')
    .query(query)
    .then((response: any) => response.text);
};
// 停止部署
export const killDeployCluster = (data: any) => {
  return agent
    .post('/api/deploy/cluster/kill')
    .send(data)
    .then((response: any) => response.body);
};

export const getCloudPreview = (data: ORG_CLUSTER.IAliyunCluster) => {
  return agent
    .post('/api/cluster-preview')
    .send(data)
    .then((response: any) => response.body);
};

export const addCloudCluster = (data: ORG_CLUSTER.IAliyunCluster) => {
  return agent
    .post('/api/cloud-clusters')
    .send(data)
    .then((response: any) => response.body);
};

export const getClusterLogTasks = (data: { recordIDs: string }) => {
  return agent
    .get('/api/records')
    .query(data)
    .then((response: any) => response.body);
};

export const upgradeCluster = (payload: { orgID: number; clusterName: string; precheck: boolean }) => {
  return agent
    .post('/api/cluster/actions/upgrade')
    .send(payload)
    .then((response: any) => response.body);
};

export const deleteCluster = (payload: { orgID: number; clusterName: string }) => {
  return agent
    .delete('/api/cluster')
    .send(payload)
    .then((response: any) => response.body);
};

export const getClusterResourceList = (query: { cluster: string }): ORG_CLUSTER.ICloudResource[] => {
  return agent
    .get('/api/ops/cloud-resource-list')
    .query(query)
    .then((response: any) => response.body);
};

export const getClusterResourceDetail = (query: {
  cluster: string;
  resource: string;
}): ORG_CLUSTER.ICloudResourceDetailItem => {
  return agent
    .get('/api/ops/cloud-resource')
    .query(query)
    .then((response: any) => response.body);
};

export const getRegisterCommand = ({ clusterName }: { clusterName: string }) => {
  return agent
    .get(`/api/cluster/init-command`)
    .query({ clusterName })
    .then((response: any) => response.body);
};

export const clusterInitRetry = ({ clusterName }: { clusterName: string }) => {
  return agent
    .post(`/api/cluster/actions/init-retry`)
    .send({ clusterName })
    .then((response: any) => response.body);
};

export const getUseableK8sCluster = () => {
  return agent.get(`/api/k8s/clusters`).then((response: any) => response.body);
};
