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

export const getErrorTrend = ({
  publisherItemId,
  ...rest
}: Merge<{ publisherItemId: string }, PUBLISHER.MonitorKey>): PUBLISHER.IErrorTrend => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/err/trend`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getErrorChart = ({ publisherItemId, ...rest }: PUBLISHER.IChartQuery) => {
  const { start, end, points, filter_av, ai, ak } = rest;
  let api = '';
  let query = {};
  if (rest.cardinality === 'affectUser') {
    api = `/api/publish-items/${publisherItemId}/err/effacts`;
    query = { start, end, points, filter_av, ai, ak };
  } else if (rest.cardinality === 'crashRate') {
    api = `/api/publish-items/${publisherItemId}/err/rate`;
    query = { start, end, points, filter_av, ai, ak };
  } else {
    api = `/api/publish-items/${publisherItemId}/metrics/ta_error_mobile/histogram`;
    query = rest;
  }
  return agent
    .get(api)
    .query(query)
    .then((response: any) => response.body);
};

export const getErrorList = ({ artifactsId, ...rest }: PUBLISHER.ErrorListQuery): PUBLISHER.ErrorItem[] => {
  return agent
    .get(`/api/publish-items/${artifactsId}/err/list`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getErrorDetail = ({ artifactsId, ...rest }: PUBLISHER.ErrorDetailQuery): PUBLISHER.ErrorDetail => {
  return agent
    .get(`/api/publish-items/${artifactsId}/metrics/ta_error_mobile`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getAllVersion = ({ publisherItemId, ...rest }: PUBLISHER.AllVersionQuery) => {
  return agent
    .get(`/api/publish-items/${publisherItemId}/metrics/ta_metric_mobile_metrics`)
    .query(rest)
    .then((response: any) => response.body);
};
