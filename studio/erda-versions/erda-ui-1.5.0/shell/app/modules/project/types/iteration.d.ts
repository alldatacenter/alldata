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

declare namespace ITERATION {
  interface ListQuery {
    pageNo?: number;
    pageSize?: number;
    projectID: number;
    deadline?: string;
    state?: string;
    withoutIssueSummary?: boolean;
  }

  interface DetailQuery {
    id: number;
    projectID: number;
  }

  interface Detail {
    id: number;
    projectID: number;
    content: string;
    createdAt: string;
    creator: string;
    finishedAt: string;
    startedAt: string;
    title: string;
    updatedAt: string;
    issueSummary: {
      bug: IIssueSummary;
      requirement: IIssueSummary;
      task: IIssueSummary;
    };
    state: string;
  }

  interface IIssueSummary {
    done: number;
    undone: number;
  }

  interface CreateBody {
    title: string;
    content: string;
    progress?: string;
    startedAt: string;
    finishedAt: string;
    state: string;
  }

  interface UpdateBody {
    projectID: number;
    id: number;
    title: string;
    content: string;
    progress?: string;
    startedAt: string;
    finishedAt: string;
    state: string;
  }
}
