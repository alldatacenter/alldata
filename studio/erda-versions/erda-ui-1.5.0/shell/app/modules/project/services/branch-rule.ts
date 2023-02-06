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

export const getBranchRules = (query: { scopeId: number; scopeType: string }): PROJECT.IBranchRule[] => {
  return agent
    .get('/api/branch-rules')
    .query(query)
    .then((response: any) => response.body);
};

export const addBranchRule = (data: PROJECT.IBranchRuleCreateBody) => {
  return agent
    .post('/api/branch-rules')
    .send(data)
    .then((response: any) => response.body);
};

export const updateBranchRule = ({ id, ...rest }: PROJECT.IBranchRule) => {
  return agent
    .put(`/api/branch-rules/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteBranchRule = ({ id }: { id: number }) => {
  return agent.delete(`/api/branch-rules/${id}`).then((response: any) => response.body);
};
