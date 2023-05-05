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

declare namespace CLOUD {
  interface Region {
    localName: string;
    regionID: string;
  }

  interface RegionQuery {
    vendor: string;
  }

  // 一级资源
  //  VPC：VPC实例
  //  VSWITCH：交换机实例
  //  ROUTETABLE：路由表实例
  //  EIP：弹性公网IP实例
  //  OSS
  //  ONS
  //  REDIS
  //  RDS
  //  ECS
  // 二级资源
  //  ONS_TOPIC
  //  ONS_GROUP
  type SetTagType =
    | 'VPC'
    | 'VSWITCH'
    | 'ROUTETABLE'
    | 'EIP'
    | 'OSS'
    | 'ONS'
    | 'REDIS'
    | 'RDS'
    | 'ECS'
    | 'ONS_TOPIC'
    | 'ONS_GROUP';

  interface SetTagBody {
    resourceType: SetTagType;
    instanceID?: string;
    tags: string[];
    items: TagItem[];
  }

  interface TagItem {
    vendor: string;
    region: string;

    // Tag一级资源时，此处为空
    // Tag二级资源时，此处指定instance id, 如指定ons id, 然后在resource ids 中指定ons_group/ons_topic
    instanceID?: string;
    resourceID: string;
    oldTags: string[];
  }
}
