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

export const getMessageList = (query: { pageNo: number; pageSize?: number }): IPagingResp<LAYOUT.IMsg> => {
  return agent
    .get('/api/mboxs')
    .query(query)
    .then((response: any) => response.body);
};

export const getMessageStats = (): { unreadCount: number } => {
  return agent.get('/api/mboxs/actions/stats').then((response: any) => response.body);
};

export const readOneMessage = (id: number) => {
  return agent.get(`/api/mboxs/${id}`).then((response: any) => response.body);
};

export const clearAllMessage = () => {
  return agent.post(`/api/mboxs/actions/read-all`).then((response: any) => response.body);
};
