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

declare namespace RUNTIME {
  type Status = 'Init' | 'Progressing' | 'UnHealthy' | 'Healthy';
  type DeployStatus = 'INIT' | 'WAITING' | 'DEPLOYING' | 'CANCELING' | 'CANCELED' | 'FAILED' | 'OK';

  interface Detail {
    id: number;
    name: string;
    serviceGroupName: string;
    serviceGroupNamespace: string;
    source: string;
    status: string;
    deployStatus: DeployStatus;
    deleteStatus: string;
    releaseId: string;
    clusterId: number;
    clusterName: string;
    clusterType: string;
    resources: CPU_MEM_DISK;
    extra: {
      applicationId: number;
      buildId: number;
      workspace: WORKSPACE;
    };
    projectID: number;
    services: {
      [k: string]: RUNTIME_SERVICE.Detail;
    };
    endpoints: {
      // 前端判断有expose的service
      [k: string]: RUNTIME_SERVICE.Detail;
    };
    timeCreated: string;
    createdAt: string;
    updatedAt: string;
    errors: null | string;
  }

  interface AddonQuery {
    projectId: number;
    workspace: WORKSPACE;
    value: string;
  }

  interface DeployRecord {
    buildId: number;
    createdAt: string;
    failCause: string;
    finishedAt: null | string;
    id: number;
    operator: string;
    operatorAvatar: string;
    operatorName: string;
    outdated: boolean;
    phase: string;
    releaseId: string;
    releaseName: string;
    rollbackFrom: number;
    runtimeId: number;
    status: string;
    step: string;
    type: string;
  }

  interface DeployListQuery {
    runtimeId: number;
    pageSize: number;
    pageNo: number;
  }

  interface RollbackBody {
    runtimeId: number;
    deploymentId: number;
  }

  interface CancelDeployBody {
    runtimeId: string;
    operator: string;
    force: boolean;
  }
}

interface CPU_MEM_DISK {
  cpu: number;
  mem: number;
  disk: number;
}
