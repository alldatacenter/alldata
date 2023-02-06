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

interface RESP<T> {
  success: boolean;
  data: T;
  err: Record<string, any>;
}

const transResponseToStandard = <T>(body: any): T | RESP<null> => {
  return body.error ? { success: false, data: null, err: { msg: body.error } } : body;
};

export const getServiceList = ({
  paths,
  org,
  startLevel,
}: RESOURCE.QueryServiceList): RESOURCE.Instances[] | RESOURCE.ServiceItem[] => {
  const pathArr = paths.map((p) => p.q);
  let projectId;
  let applicationId;
  let runtimeId;
  let serviceName;
  let fetchApiArr = [];
  if (startLevel === 'runtime') {
    [applicationId, runtimeId, serviceName] = pathArr;
    fetchApiArr = [
      `/api/instances-usage?type=runtime&application=${applicationId}&org=${org}`,
      `/api/instances-usage?type=service&runtime=${runtimeId}&org=${org}`,
      `/api/instances/actions/get-service?runtimeID=${runtimeId}&serviceName=${serviceName}&status=running`,
    ];
  } else {
    [projectId, applicationId, runtimeId, serviceName] = pathArr;
    fetchApiArr = [
      `/api/instances-usage?type=application&project=${projectId}&org=${org}`,
      `/api/instances-usage?type=runtime&application=${applicationId}&org=${org}`,
      `/api/instances-usage?type=service&runtime=${runtimeId}&org=${org}`,
      `/api/instances/actions/get-service?runtimeID=${runtimeId}&serviceName=${serviceName}&status=running`,
    ];
  }

  return agent.get(fetchApiArr[pathArr.length - 1]).then((response: any) => transResponseToStandard(response.body));
};

export const getChartData = ({ type, paths, query, startLevel, projectId }: RESOURCE.QuertChart): RESOURCE.CharData => {
  const fetchMap = {
    cpu: {
      api: '/api/projects/resource/cpu/actions/list-usage-histogram',
      query: { last: 'cpu_usage_percent' },
    },
    memory: {
      api: '/api/projects/resource/memory/actions/list-usage-histogram',
      query: { last: 'mem_usage' },
    },
  };
  const pathArr = paths.map((p) => p.q);
  let filter_project_id = projectId;
  let filter_application_id;
  let filter_runtime_id;
  let filter_service_name;
  let queryParam = [];
  if (startLevel === 'runtime') {
    [filter_application_id, filter_runtime_id, filter_service_name] = pathArr;
    queryParam = [
      { filter_project_id, filter_application_id, group: 'container_id', reduce: 'sum' },
      { filter_project_id, filter_application_id, filter_runtime_id, group: 'container_id', reduce: 'sum' },
      {
        filter_project_id,
        filter_application_id,
        filter_runtime_id,
        filter_service_name,
        group: 'container_id',
        reduce: 'sum',
      },
    ];
  } else {
    [filter_project_id, filter_application_id, filter_runtime_id, filter_service_name] = pathArr;
    queryParam = [
      { filter_project_id, group: 'container_id', reduce: 'sum' },
      { filter_project_id, filter_application_id, group: 'container_id', reduce: 'sum' },
      { filter_project_id, filter_application_id, filter_runtime_id, group: 'container_id', reduce: 'sum' },
      {
        filter_project_id,
        filter_application_id,
        filter_runtime_id,
        filter_service_name,
        group: 'container_id',
        reduce: 'sum',
      },
    ];
  }

  const fetchObj = fetchMap[type].query;
  const fetchApi = fetchMap[type].api;
  const queryParamObj = queryParam[pathArr.length - 1];

  const finalQuery = {
    start: Date.now() - 60 * 60 * 1000, // 查询前一分钟内
    end: Date.now(),
    ...fetchObj,
    ...queryParamObj, // 获取对应层级查询条件
    ...query,
  };

  return agent
    .get(fetchApi)
    .query(finalQuery)
    .then((response: any) => transResponseToStandard(response.body));
};
