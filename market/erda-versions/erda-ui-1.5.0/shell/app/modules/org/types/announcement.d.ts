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

declare namespace ORG_ANNOUNCEMENT {
  type Status = 'unpublished' | 'published' | 'deprecated';

  interface Action {
    id: number;
  }

  interface SaveNew {
    content: string;
  }

  interface SaveEdit {
    id: string;
    content: string;
  }

  interface QueryList {
    pageNo?: number;
    pageSize?: number;
    content?: string;
    status?: Status;
  }

  interface Item {
    id: number;
    orgID: number;
    content: string;
    status: string;
    createdAt: string;
    updatedAt: string;
  }
}
