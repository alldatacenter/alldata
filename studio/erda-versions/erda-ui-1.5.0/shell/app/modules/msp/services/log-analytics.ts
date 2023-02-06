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

export const getAggregation = ({ addon, ...rest }: LOG_ANALYTICS.QueryAggregation): LOG_ANALYTICS.IAggregation => {
  return agent
    .get(`/api/log-analytics/${addon}/aggregation`)
    .query(rest)
    .then((response: any) => response.body);
};
export const getFields = ({ addon }: { addon: string }): LOG_ANALYTICS.IField[] => {
  return agent.get(`/api/log-analytics/${addon}/fields`).then((response: any) => response.body);
};
export const getLogAnalytics = ({ addon, ...rest }: LOG_ANALYTICS.QuerySearch): LOG_ANALYTICS.SearchResult => {
  return agent
    .get(`/api/log-analytics/${addon}/search`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getLogAnalyticContext = ({
  addon,
  ...rest
}: LOG_ANALYTICS.QueryContext): { data: LOG_ANALYTICS.LogItem[]; total: number } => {
  return agent
    .get(`/api/log-analytics/${addon}/sequentialSearch`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getStatistic = ({
  addon,
  ...rest
}: LOG_ANALYTICS.QueryStatistic): Promise<{ data: LOG_ANALYZE.LogStatistics }> => {
  return agent
    .get(`/api/log-analytics/${addon}/statistic`)
    .query(rest)
    .then((response: any) => response.body);
};

export const downLoadLogApi = (addon: string) => `/api/log-analytics/${addon}/download`;
