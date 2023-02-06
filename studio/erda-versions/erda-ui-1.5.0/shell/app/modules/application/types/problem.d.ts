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

declare namespace PROBLEM {
  interface Ticket {
    id: number;
    title: string;
    content: string;
    type: string;
    priority: string;
    status: string;
    key: string;
    orgID: string;
    metric: string;
    metricID: string;
    count: 1;
    creator: string;
    lastOperator: string;
    label: {
      branch: string;
      code: string;
      endLine: string;
      line: string;
      lineCode: string;
      message: string;
      path: string;
      rule: string;
      severity: string;
      startLine: string;
      status: string;
    };
    targetType: string;
    targetID: string;
    createdAt: string;
    updatedAt: string;
    closedAt: string;
    triggeredAt: string;
    author: string; // userMap注入
    lastOperatorUser: string;
  }

  type Status = 'all' | 'open' | 'closed';

  interface ListQuery {
    pageNo: number;
    pageSize: number;
    targetType: string;
    targetID: number;
    status?: string;
    q?: string;
    priority?: string;
    type?: string;
  }

  interface CreateBody {
    title: string;
    content: string;
    type: string;
    priority: string;
    status: string;
    userID: string;
    targetID: string;
    targetType: string;
  }

  interface CommentBody {
    content?: string;
    irComment?: {
      issueID: number;
      issueTitle: string;
      projectID: number;
      iterationID: number;
      issueType: string;
    };
    commentType: string;
    ticketID: number;
    userID: string;
  }

  interface Comment {
    commentType: string;
    content: string;
    createdAt: string;
    id: number;
    irComment: {
      issueID: number;
      issueTitle: string;
      issueType: string;
      iterationID: number;
      projectID: number;
    };
    ticketID: number;
    updatedAt: string;
    userID: string;
    author?: string; // userMap注入
  }

  interface TicketType {
    value: string;
    name: string;
  }
}
