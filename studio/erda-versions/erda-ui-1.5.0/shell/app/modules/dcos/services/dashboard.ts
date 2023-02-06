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
import { apiCreator } from 'core/service';

const apis = {
  getResourceGauge: {
    api: '/api/resource-overview/gauge',
  },
  getResourceClass: {
    api: '/api/resource-overview/class',
  },
  getResourceTable: {
    api: '/api/resource-overview/report-table',
  },
  getClusterTrend: {
    api: '/api/resource-overview/cluster-trend',
  },
  getProjectTrend: {
    api: '/api/resource-overview/project-trend',
  },
};

export const getResourceClass = apiCreator<(p: ORG_DASHBOARD.ResourceClassQuery) => ORG_DASHBOARD.ResourceClassData>(
  apis.getResourceClass,
);
export const getResourceGauge = apiCreator<(p: ORG_DASHBOARD.ResourceGaugeQuery) => ORG_DASHBOARD.ResourceGaugeData>(
  apis.getResourceGauge,
);
export const getResourceTable = apiCreator<(p: ORG_DASHBOARD.ResourceTableQuery) => ORG_DASHBOARD.ResourceTableData>(
  apis.getResourceTable,
);
export const getClusterTrend = apiCreator<(p: ORG_DASHBOARD.ClusterTrendQuery) => ORG_DASHBOARD.EchartOption>(
  apis.getClusterTrend,
);
export const getProjectTrend = apiCreator<(p: ORG_DASHBOARD.ProjectTrendQuery) => ORG_DASHBOARD.EchartOption>(
  apis.getProjectTrend,
);

interface IFilterTypeQuery {
  clusterName: string;
  orgName: string;
}
export const getFilterTypes = (payload: IFilterTypeQuery): ORG_DASHBOARD.IFilterType[] => {
  return agent
    .get('/api/cluster/resources/types')
    .query(payload)
    .then((response: any) => response.body);
};

export const getGroupInfos = ({ orgName, ...rest }: ORG_DASHBOARD.IGroupInfoQuery): ORG_DASHBOARD.IGroupInfo => {
  return agent
    .post('/api/cluster/resources/group')
    .query({ orgName })
    .send(rest)
    .then((response: any) => response.body);
};

export interface IInstanceListQuery {
  instanceType: string;
  orgName?: string;
  clusters: Array<{ clusterName: string; hostIPs?: string[] }>;
  filters?: ORG_DASHBOARD.IFilterQuery[];
  start?: string;
}
export const getInstanceList = ({
  instanceType,
  orgName,
  clusters,
  filters,
  start,
}: ORG_DASHBOARD.IInstanceListQuery): ORG_DASHBOARD.IInstance => {
  return agent
    .post(`/api/cluster/resources/containers/${instanceType}`)
    .query({ orgName, start })
    .send({ clusters, filters })
    .then((response: any) => response.body);
};

export const getNodeLabels = (): ORG_DASHBOARD.INodeLabel[] => {
  return agent.get('/api/node-labels').then((response: any) => response.body);
};

// TODO: add type define
export const getChartData = ({ query, clusters, url }: any) => {
  const commonQuery = { limit: 4 };

  const fetchQuery = {
    ...commonQuery,
    ...query,
  };

  return agent
    .post(url)
    .query(fetchQuery)
    .send({ clusters })
    .then((response: any) => response.body);
};
