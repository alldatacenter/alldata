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

export const getTicketList = (params: PROBLEM.ListQuery): IPagingResp<PROBLEM.Ticket> => {
  return agent
    .get('/api/tickets')
    .query(params)
    .then((response: any) => response.body);
};

export const getTicketDetail = (ticketId: number): PROBLEM.Ticket => {
  return agent.get(`/api/tickets/${ticketId}`).then((response: any) => response.body);
};

export const addTicket = (data: PROBLEM.CreateBody) => {
  return agent
    .post('/api/tickets')
    .send(data)
    .then((response: any) => response.body);
};

// export const updateTicket = (data: PROBLEM.CreateBody) => {
//   return agent.put('/api/tickets')
//     .send(data)
//     .then((response: any) => response.body);
// };

export const closeTicket = (ticketId: number) => {
  return agent.put(`/api/tickets/${ticketId}/actions/close`).then((response: any) => response.body);
};

export const reopenTicket = (ticketId: number) => {
  return agent.put(`/api/tickets/${ticketId}/actions/reopen`).then((response: any) => response.body);
};

// 这个接口不用传分页参数，返回类型需自定义
export const getComments = (ticketId: number): { comments: PROBLEM.Comment[]; total: number } => {
  return agent
    .get('/api/comments')
    .query({ ticketID: ticketId })
    .then((response: any) => response.body);
};

export const createComment = (payload: PROBLEM.CommentBody) => {
  return agent
    .post('/api/comments')
    .send(payload)
    .then((response: any) => response.body);
};
