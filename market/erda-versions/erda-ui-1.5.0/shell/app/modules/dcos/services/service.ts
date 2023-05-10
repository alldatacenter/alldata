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
import moment from 'moment';

export const fetchContainerList = (query: Omit<DCOS_SERVICES.QueryServices, 'status'>): DCOS_SERVICES.Service[] => {
  return agent
    .get('/api/instances/actions/get-service')
    .query({ ...query, status: 'running' })
    .then((response: any) => response.body);
};

export const getRuntimeJson = ({ runtimeId }: { runtimeId: string }) => {
  return agent.get(`/api/runtimes/${runtimeId}/configuration`).then((response: any) => response.body);
};

export const getRuntimeStatus = ({ runtimeIds }: { runtimeIds: string }): DCOS_SERVICES.Bulk => {
  return agent
    .get(`/api/runtimes/actions/bulk-get-status?runtimeIds=${runtimeIds}`)
    .then((response: any) => response.body);
};

export const getServiceList = ({
  paths,
  environment,
  ip,
}: {
  paths: DCOS_SERVICES.path[];
  environment: string;
  ip?: string;
}): DCOS_SERVICES.InstancesUsage[] => {
  const [clusterName, projectId, applicationId, runtimeId] = paths.map((p) => p.q);
  const query: DCOS_SERVICES.QueryServiceList = {
    type: 'project',
    environment,
    ip,
  };
  if (projectId) {
    query.type = 'application';
    query.project = projectId;
    if (applicationId) {
      query.type = 'runtime';
      query.application = applicationId;
      if (runtimeId) {
        query.type = 'service';
        query.runtime = runtimeId;
      }
    }
  }
  return agent
    .get(`/api/cmdb/clusters/${clusterName}/instances-usage`)
    .query(query)
    .then((response: any) => response.body);
};

// 获取度量接口：project/application/runtime/service/container
export const getMetrics = ({ type, paths, filter_workspace }: DCOS_SERVICES.QueryMetrics) => {
  const fetchMap = {
    cpu: {
      api: '/api/spot/metrics/charts/docker_container_cpu',
      query: { filter_cpu: 'cpu-total', last: 'usage_percent', filter_workspace },
    },
    mem: {
      api: '/api/spot/metrics/charts/docker_container_mem',
      query: { last: 'usage', filter_workspace },
    },
  };

  const pathArr = paths.map((p) => p.q);
  const [filter_cluster_name, filter_project_id, filter_application_id, filter_runtime_id, filter_service_name] =
    pathArr;
  const queryParam = [
    { group: ['project_id', 'container_id'], reduce: 'sum', filter_cluster_name },
    { group: ['application_id', 'container_id'], reduce: 'sum', filter_project_id },
    { group: ['runtime_id', 'container_id'], reduce: 'sum', filter_application_id, filter_project_id },
    {
      group: ['service_name', 'container_id'],
      reduce: 'sum',
      filter_application_id,
      filter_project_id,
      filter_runtime_id,
    },
    { group: ['container_id'], filter_application_id, filter_project_id, filter_runtime_id, filter_service_name },
  ];
  const query = {
    start: moment().subtract(2, 'm').valueOf(), // 查询前 2 分钟内
    end: moment().valueOf(),
    ...fetchMap[type].query,
    ...queryParam[pathArr.length - 1], // 获取对应层级查询条件
  };

  return agent
    .get(fetchMap[type].api)
    .query(query)
    .then((response: any) => response.body);
};
