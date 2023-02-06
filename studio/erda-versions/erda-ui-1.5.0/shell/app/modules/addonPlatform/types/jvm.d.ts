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

declare namespace JVM {
  interface ServiceIns {
    applicationId: string;
    applicationName: string;
    services: Array<{
      serviceId: string;
      serviceName: string;
      instances: Array<{
        instanceId: string;
        instanceName: string;
      }>;
    }>;
  }

  type ProfileState = 'pending' | 'running' | 'completed' | 'failed' | 'terminating';

  interface ProfileListQuery {
    insId: string;
    state: ProfileState;
    pageNo?: number;
    pageSize?: number;
  }

  interface ProfileItem {
    profiling: string;
    state: {
      state: ProfileState;
      message: string;
    };
    message: string;
    applicationName: string;
    serviceName: string;
    serviceInstanceName: string;
    createTime: number;
    finishTime: number; // pending、running状态时为0
  }

  interface PendingProfile {
    id: string;
    resource: string;
    state: string;
    message: string;
    createTime: number;
    finishTime: number;
  }

  interface StartProfileBody {
    insId: string;
    applicationId: string;
    serviceId: string;
    serviceInstanceId: string;
  }

  interface ProfileStatusQuery {
    insId: string;
    profileId: string;
  }

  interface JVMInfoQuery extends ProfileStatusQuery {
    scope: string;
  }
}
