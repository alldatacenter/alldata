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

export const getTestTypes = () => {
  return agent.get('/api/qa/actions/all-test-type').then((response) => response.body);
};

export const getTestList = (query) => {
  return agent
    .get('/api/qa/actions/test-list')
    .query(query)
    .then((response) => response.body);
};

export const getTestDetail = (id) => {
  return agent.get(`/api/qa/test/${id}`).then((response) => response.body);
};
