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

declare namespace PURCHASE {
  type EcsType = 'master' | 'pubilc' | 'private';
  type PayType = 'PrePaid' | 'PostPaid';
  type NetworkPayType = 'PayByTraffic' | 'PayByBandwidth';
  type NatSpec = 'Small' | 'Middle' | 'Large' | 'XLarge';
  type NasStorageType = 'Performance' | 'Capacity';
  type PurchaseItemStatusKey = 'ecs' | 'nat' | 'nas' | 'rds' | 'redis' | 'slb' | 'vpc';

  interface AddResource {
    [k: string]: any;
    type: string;
  }

  interface PurchaseItem {
    [k: string]: any;
    createdAt: string;
    type: string;
    status: {
      [k in PurchaseItemStatusKey]: any;
    };
    error: {
      [k in PurchaseItemStatusKey]: any;
    };
    info: {
      [k in PurchaseItemStatusKey]: any;
    };
  }

  interface QueryZone {
    regionId: string;
    accessKeyId: string;
    accessKeySecret: string;
  }

  interface CheckEcs {
    clusterName: string;
    accessKeyId: string;
    accessKeySecret: string;
    regionId: string;
    zoneId: string;
    instanceType: string;
  }

  interface CheckEscRes {
    available: boolean;
    description: string;
  }

  interface Zone {
    zoneId: string;
    localName: string;
  }

  interface Region {
    regionId: string;
    localName: string;
  }

  interface EcsSetting<T> {
    nodeType?: T;
    instanceType?: string;
    instanceChargeType?: PayType;
    systemDiskSize?: number;
    amount: number;
    periodUnit?: string;
    period?: string;
  }

  type EcsSettings<T> = {
    // @ts-ignore
    [k in T]: EcsSetting<k>;
  };

  interface NatSetting {
    natSpec: NatSpec;
    NatBandwidth: number;
    EipInternetChargeType: NetworkPayType;
    EipInstanceChargeType: PayType;
  }

  interface NasSetting {
    nasStorageType: NasStorageType;
  }

  interface RedisSettings {
    chargeType: PayType;
    instanceClass: string;
    engineVersion: string;
    period: string;
  }

  interface RdsSettings {
    dbInstanceClass: string;
    payType: PayType;
    engineVersion: string;
    dbInstanceStorage: number;
    accountName: string;
    dbName: string;
    Password: string;
    parameters: {
      character_set_server: 'utf8' | 'gbk' | 'latin1' | 'utf8mb4';
    };
  }

  interface PageFormData {
    clusterName: string;
    vpcSetting: {
      vpcCidr: string;
    };
    accessKeyId: string;
    accessKeySecret: string;
    ecsSettings: EcsSettings<EcsType>;
    natSetting?: NatSetting;
    nasSetting?: NasSetting;
    redisSettings: RedisSettings;
    rdsSettings: RdsSettings;
  }

  interface AddPhysicalCluster {
    clusterName: string;
    vpcSetting: {
      vpcCidr: string;
    };
    accessKeyId: string;
    accessKeySecret: string;
    natSetting?: NatSetting;
    nasSetting?: NasSetting;
    ecsSettings: Array<EcsSetting<EcsType>>;
    redisSettings: RedisSettings[];
    rdsSettings: RdsSettings[];
  }
}
