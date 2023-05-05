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

export const getLabels = (query: LABEL.ListQuery): IPagingResp<LABEL.Item> => {
  return agent
    .get('/api/labels')
    .query({ ...query, pageNo: 1, pageSize: 300 }) // 页面上不分页
    .then((response: any) => response.body);
};

export const createLabel = (form: LABEL.CreateBody) => {
  return agent
    .post('/api/labels')
    .send(form)
    .then((response: any) => response.body);
};

export const updateLabel = ({ id, ...values }: LABEL.Item) => {
  return agent
    .put(`/api/labels/${id}`)
    .send(values)
    .then((response: any) => response.body);
};

export const deleteLabel = (labelId: number) => {
  return agent.delete(`/api/labels/${labelId}`).then((response: any) => response.body);
};
