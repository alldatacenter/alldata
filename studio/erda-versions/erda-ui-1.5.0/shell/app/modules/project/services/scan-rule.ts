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

export const getAppendedScanRules = (query: SCAN_RULE.IAppendedQuery) => {
  return agent
    .get('/api/sonar-metric-rules')
    .query(query)
    .then((response: any) => response.body);
};

export const getOptionalScanRules = (query: SCAN_RULE.IOptionalQuery) => {
  return agent
    .get('/api/sonar-metric-rules/actions/query-metric-definition')
    .query(query)
    .then((response: any) => response.body);
};

export const updateScanRule = ({ id, ...rest }: SCAN_RULE.IUpdateBody) => {
  return agent
    .put(`/api/sonar-metric-rules/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const batchInsertScanRule = (data: SCAN_RULE.IBatchInsertBody) => {
  return agent
    .post('/api/sonar-metric-rules/actions/batch-insert')
    .send(data)
    .then((response: any) => response.body);
};

export const deleteScanRule = ({ id, ...rest }: SCAN_RULE.IDeleteBody) => {
  return agent
    .delete(`/api/sonar-metric-rules/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const batchDeleteScanRule = (query: SCAN_RULE.IBatchDeleteBody) => {
  return agent
    .delete('/api/sonar-metric-rules/actions/batch-delete')
    .send(query)
    .then((response: any) => response.body);
};
