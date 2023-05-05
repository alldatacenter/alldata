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

declare namespace COMPUTING {
  interface ECS {
    cpu: number;
    id: string;
    hostname: string;
    memory: number;
    innerIpAddress: string;
    regionID: string;
    regionName: string;
    startTime: string;
    expireTime: string;
    osName: string;
    vendor: string;
    status: string;
    chargeType: string;
    tag: {
      [s: string]: boolean;
    };
  }

  interface ECSActionReq {
    instanceIds: string[];
    region: string;
    vendor: string;
    duration?: number;
  }

  interface ECSActionRes {
    failedInstances: Array<{
      message: string;
      instanceId: string;
    }>;
  }
}
