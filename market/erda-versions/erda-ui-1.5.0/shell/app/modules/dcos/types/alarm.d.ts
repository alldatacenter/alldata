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

declare namespace ORG_ALARM {
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
    count: number;
    creator: string;
    lastOperator: string;
    label: {
      Path: string;
      Time: string;
      Type: string;
      cluster_name: string;
      host_ip: string;
      load5: number;
      type_id: string;
      path: string;
    };
    targetType: string;
    targetID: string;
    createdAt: string;
    updatedAt: string;
    closedAt: string;
    triggeredAt: string;
  }
}
