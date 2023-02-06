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

export const getStatisticsTrend = ({
  publisherItemId,
  ...rest
}: Merge<{ publisherItemId: string }, PUBLISHER.MonitorKey>): PUBLISHER.IStatisticsTrend => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/statistics/trend`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getStatisticsChart = ({ publisherItemId, ...rest }: PUBLISHER.IChartQuery): PUBLISHER.IStatisticsTrend => {
  const { cardinality, start, end, points, ai, ak } = rest;
  const [api, query] =
    cardinality === 'tags.uid'
      ? [`/api/publish-items/${publisherItemId}/statistics/users`, { start, end, points, ak, ai }]
      : [`/api/publish-items/${publisherItemId}/metrics/ta_metric_mobile_metrics/histogram`, rest];
  return agent
    .get(api)
    .query(query)
    .then((response: any) => response.body);
};

export const getStatisticsPieChart = ({
  publisherItemId,
  ...rest
}: PUBLISHER.IChartQuery): PUBLISHER.IStatisticsTrend => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/metrics/ta_metric_mobile_metrics`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getVersionStatistics = ({
  artifactsId,
  ...rest
}: PUBLISHER.VersionStatisticQuery): PUBLISHER.VersionStatistic[] => {
  return agent
    .get(`/api/publish-items/${artifactsId}/statistics/versions`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getAllGroup = ({ publisherItemId, ...rest }: PUBLISHER.IChartQuery) => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/metrics/ta_metric_mobile_metrics`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getTopLineChart = ({ publisherItemId, ...rest }: PUBLISHER.IChartQuery) => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/metrics/ta_metric_mobile_metrics/histogram`)
    .query(rest)
    .then((response: any) => response.body);
};
