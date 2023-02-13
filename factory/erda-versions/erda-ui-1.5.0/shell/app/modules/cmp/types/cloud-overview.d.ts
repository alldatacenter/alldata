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

declare namespace CLOUD_OVERVIEW {
  interface Querys {
    vendor?: string;
    region?: string;
  }
  type computeSource = 'ECS';
  type networkSource = 'VPC' | 'VSWITCH';
  type storageSource = 'OSS_BUCKET';
  type cloudServiceSource = 'RDS' | 'ROCKET_MQ' | 'REDIS';

  interface ResourceTypeData<T> {
    resourceTypeData: {
      // @ts-ignore
      [p in T]: {
        totalCount: number;
        displayName: string;
        expireDays: number;
      };
    };
  }

  interface OverviewData {
    COMPUTE: ResourceTypeData<computeSource>;
    NETWORK: ResourceTypeData<networkSource>;
    STORAGE: ResourceTypeData<storageSource>;
    CLOUD_SERVICE: ResourceTypeData<cloudServiceSource>;
  }

  interface ECSTrendingData {
    time: number[];
    results: any[];
  }
}
