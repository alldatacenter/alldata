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

declare namespace CLUSTER_TASK {
  interface Detail {
    applicationID: number;
    applicationName: string;
    clusterName: string;
    costTimeSec: number;
    createdAt: string;
    env: string;
    orgID: number;
    pipelineID: number;
    projectID: number;
    projectName: string;
    queueTimeSec: number;
    releaseID: string;
    runtimeID: string;
    status: string;
    taskID: number;
    taskName: string;
    taskType: string;
    userID: string;
  }

  interface ListQuery {
    pageSize: number;
    pageNo: number;
    type: string;
    cluster?: string;
  }
}
