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

declare namespace CLOUD_SERVICE {
  interface ICloudServiceQuery {
    vendor: string;
    region: string;
  }

  interface IRDS {
    chargeType: string;
    id: string;
    name: string;
    spec: string;
    status: string;
    tag: {
      [s: string]: boolean;
    };
    version: string;
    createTime: string;
    region: string;
  }

  interface IRDSCreateBody {
    autoRenew: boolean;
    chargePeriod: string;
    chargeType: string;
    clientToken: string;
    instanceName: string;
    region: string;
    spec: string;
    source: string;
    specType: string;
    storageSize: number;
    storageType: string;
    vendor: string;
    vpcID: string;
    zoneID: string;
  }

  interface IRDSAccountBody {
    account: string;
    password: string;
    instanceID: string;
    region: string;
    vendor: string;
  }

  interface IRDSCreateAccountBody extends IRDSAccountBody {
    description: string;
  }

  interface IRDSAccountPriv {
    dBName: string;
    accountPrivilege: string;
  }

  interface IRDSRevokeAccountPrivBody {
    instanceID: string;
    region: string;
    vendor: string;
    account: string;
    oldAccountPrivileges: IRDSAccountPriv[];
    accountPrivileges: IRDSAccountPriv[];
  }

  interface IUserQuery {
    id: string;
    query: {
      vendor?: string;
      region?: string;
      cluster?: string;
    };
  }

  interface IDatabaseQuery {
    id: string;
    query: {
      vendor?: string;
      region?: string;
      cluster?: string;
    };
  }

  interface IRDSDatabaseBody {
    vendor: string;
    region: string;
    source: string;
    instanceID: string;
    databases: Array<{
      dbName: string;
      description: string;
      characterSetName: string;
      account: string;
      accountPrivilege?: string;
    }>;
  }

  interface IRDSAccountResp {
    accountName: string;
    accountStatus: string;
    accountType: string;
    accountDescription: string;
    databasePrivileges: IRDSAccountPriv[];
  }

  interface IRDSDatabaseResp {
    dBName: string;
    dBStatus: string;
    characterSetName: string;
    dBDescription: string;
    accounts: Array<{
      account: string;
    }>;
  }

  interface IMQ {
    id: string;
    instanceType: string;
    name: string;
    status: string;
    region: string;
    tags: {
      [s: string]: boolean;
    };
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-interface
  interface IMQCreateBody {}

  interface IMQManageQuery {
    vendor?: string;
    region?: string;
    instanceID: string;
  }

  interface IMQGroupQuery extends IMQManageQuery {
    groupType?: string;
  }

  interface IMQTopic {
    topicName: string;
    messageType: string;
    relationName: string;
    remark: string;
    createTime: string;
    tags: {
      [tag: string]: boolean;
    };
  }

  interface IMQTopicBody {
    vendor: string;
    region: string;
    source: string;
    instanceID: string;
    topics: Array<{
      topicName: string;
      messageType: number;
      remark: string;
    }>;
  }

  interface IMQGroup {
    groupId: string;
    remark: string;
    groupType: string;
    instanceId: string;
    createTime: string;
    tags: {
      [tag: string]: boolean;
    };
  }

  interface IMQGroupBody {
    vendor: string;
    region: string;
    source: string;
    instanceID: string;
    groups: Array<{
      groupID: string;
      groupType: string;
      remark: string;
    }>;
  }

  interface IRedis {
    capacity: number;
    chargeType: string;
    createTime: string;
    id: string;
    name: string;
    tags: {
      [s: string]: boolean;
    };
    region: string;
    spec: string;
    status: string;
    version: string;
  }

  interface IRedisCreateBody {
    chargeType: string;
    name: string;
    region: string;
    source: string;
    spec: string;
    version: string;
    vpcID: string;
    autoRenew: boolean;
    chargePeriod: string;
  }

  interface IDetailsQuery {
    id: string;
    query: {
      vendor?: string;
      region: string;
    };
  }

  interface IDetailsResp {
    label: 'string';
    items: Array<{ name: string; value: string }>;
  }
}
