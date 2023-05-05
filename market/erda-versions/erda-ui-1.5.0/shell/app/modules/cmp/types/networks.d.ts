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

declare namespace NETWORKS {
  interface ICloudVpcQuery {
    vendor?: string;
    region?: string;
    vpcID?: string;
  }

  interface ICloudVpc {
    cidrBlock: string;
    regionID: string;
    tags: Obj<boolean>;
    vpcID: string;
    vpcName: string;
    vendor: string;
    description: string;
  }

  interface IVpcCreateBody {
    cidrBlock: string;
    description: string;
    region: string;
    vendor: string;
    vpcName: string;
  }

  interface ICloudVswQuery {
    vendor?: string;
    region?: string;
    vpcID?: string;
  }
  interface ICloudVsw {
    cidrBlock: string;
    status: string;
    vSwitchID: string;
    vpcID: string;
    zoneID: string;
    zoneName: string;
    vswName: string;
    vendor: string;
    region: string;
    tags: Obj<boolean>;
  }

  interface IVswCreateBody {
    cidrBlock: string;
    description: string;
    region: string;
    vendor: string;
    vpcID: string;
    vswName: string;
    zoneID: string;
  }

  interface ICloudZoneQuery {
    vendor: string;
    region?: string;
  }

  interface ICloudZone {
    localName: string;
    zoneID: string;
  }
}
