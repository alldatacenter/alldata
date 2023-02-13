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

export const listMetricByResourceType = ({ resourceType, query = {} }: METRICS.ChartMetaQuerys): METRICS.ChartMeta => {
  return agent
    .get(`/api/spot/chart/meta?type=${resourceType}`)
    .query(query)
    .then((response: any) => {
      return { resourceType, metrics: response.body || {} };
    });
};

export const loadMetricItem = ({
  type,
  resourceType,
  resourceId,
  chartQuery,
  ...rest
}: METRICS.LoadMetricItemQuerys): { id: string; data: METRICS.MetricItem } => {
  const { fetchMetricKey, customAPIPrefix, ...query } = chartQuery;
  const apiPrefix = customAPIPrefix || '/api/metrics/charts/';
  return agent
    .get(`${apiPrefix}${fetchMetricKey}/histogram`)
    .query({ ...rest, ...query })
    .then((response: any) => {
      return { id: `${resourceType}-${resourceId}-${type}`, data: response.body };
    });
};

export const loadGatewayMetricItem = ({
  type,
  resourceType,
  resourceId,
  chartQuery,
  ...rest
}: METRICS.GetGateway): METRICS.GatewayData => {
  const { fetchMetricKey, ...query } = chartQuery;
  return agent
    .get(`/api/gateway/openapi/metrics/charts/${fetchMetricKey}/histogram`)
    .query({ ...rest, ...query })
    .then((response: any) => {
      return { id: `${resourceType}-${resourceId}-${type}`, data: response.body };
    });
};
