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

declare namespace ORG_CLUSTER {
  interface ICluster {
    id: number;
    orgID: number;
    name: string;
    displayName: string;
    type: string;
    logo: string;
    cloudVendor: string;
    description: string;
    wildcardDomain: string;
    urls: {
      colonySoldierPublic: string;
      dingDingWarning: string;
      nexus: string;
      registry: string;
    };
    settings: {
      bpDockerBaseRegistry: string;
      ciHostPath: string;
      nexusUsername: string;
      registryAppID: string;
      registryHostPath: string;
      scriptBlacklist: string;
    };
    scheduler: {
      edasConsoleAddr?: string; // edas集群参数
      accessKey?: string;
      accessSecret?: string;
      clusterID?: string;
      regionID?: string;
      logicalRegionID?: string;
      k8sAddr?: string;
      regAddr?: string;
    };
    config: {
      bpDockerBaseRegistry: string;
      ciHostPath: string;
      colonySoldier: string;
      nexus: string;
      nexusUsername: string;
      registry: string;
      registryHostPath: string;
    };
    createdAt: string;
    updatedAt: string;
  }
  interface IAddClusterQuery {
    name: string;
    displayName?: string;
    wildcardDomain: string;
    type: string;
    description?: string;
    logo?: string;
    urls: any;
    orgId?: number;
    id?: number;
  }

  interface IAliyunCluster {
    orgID: number;
    orgName: string;
    cloudVendor: string;
    clusterName: string;
    displayName: string;
    rootDomain: string;
    enableHttps: boolean;
    region: string;
    clusterSpec: string;
    chargeType: string;
    chargePeriod: number;
    accessKey: string;
    secretKey: string;
    vpcCIDR: string;
    vSwitchCIDR: string;
    serviceCIDR: string;
    podCIDR: string;
    dockerCIDR: string;
  }

  interface ICloudResource {
    name: string;
    displayName: string;
  }

  interface ICloudResourceDetailItem {
    displayName: string;
    labels: {
      [pro: string]: string;
    };
    labelOrder: string[];
    data: any[];
  }

  interface ICloudResourceDetail {
    [pro: string]: ICloudResourceDetailItem;
  }

  interface IViewClusterStatus {
    name: string;
    displayName: string;
    status: number;
    components: any;
  }
}
