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

declare namespace ORG_MACHINE {
  interface IAddMachineBody {
    clusterName: string; // 操作的集群名
    orgID: number; // org id
    hosts: string[]; // 机器列表
    labels: ORG_DASHBOARD.INodeLabel[]; // 标签列表
    port: number; // SSH 端口
    user: string; // SSH 用户
    password: string; // SSH 密码
    dataDiskDevice?: string; // 数据盘设备名
  }

  interface IAddCloudMachineBody {
    clusterName: string; // 集群名称
    orgID: number; // 企业ID
    cloudVendor: string;
    availabilityZone: string;
    chargeType: string;
    chargePeriod: number;
    accessKey: string;
    secretKey: string;
    cloudResource: string;
    instancePassword: string;
    instanceNum: number;
    instanceType: string;
    diskType: string;
    diskSize: number;
    decurityGroupIds: string[];
    vSwitchId: string;
    labels: string[];
  }

  interface IAddCloudMachineResp {
    recordID: string;
  }

  interface ICloudLogStatusQuery {
    recordID: string;
  }
  interface ICloudLogStatusResp {
    recordID: string;
    conditions: ICondition[];
  }
  interface ICondition {
    phase: string;
    status: string;
  }
  interface IMachine {
    clusterName: string;
    ip: string;
    hostname: string;
    os: string;
    kernelVersion: string;
    labels: string;
    tasks: number;
    cpuUsage: number;
    cpuRequest: number;
    cpuLimit: number;
    cpuOrigin: number;
    cpuTotal: number;
    cpuAllocatable: number;
    memUsage: number;
    memRequest: number;
    memLimit: number;
    memOrigin: number;
    memTotal: number;
    memAllocatable: number;
    diskUsage: number;
    diskLimit: number;
    diskTotal: number;
    load1: number;
    load5: number;
    load15: number;
    health: true;
    abnormalMsg: string;
    status: string;
    cpuUsagePercent: number;
    memUsagePercent: number;
    diskUsagePercent: number;
    loadPercent: number;
    cpuDispPercent: number;
    memDispPercent: number;
  }

  interface IMachineLabelBody {
    clusterName: string; // 操作的集群名
    orgID: number; // org id
    hosts: string[]; // 机器列表
    labels: string[]; // 标签列表
  }

  interface IClusterOperateRecord {
    clusterName: string;
    createTime: string;
    detail: string;
    orgID: string;
    recordID: string;
    recordType: string;
    status: string;
    userID: string;
  }

  interface IClusterOperateType {
    rawRecordType: string;
    recordType: string;
  }

  interface IDeleteMachineBody {
    clusterName: string;
    hosts: string[];
    port: number;
    user: string;
    password: string;
    force: boolean;
  }
  interface IDeleteResp {
    recordID: string;
  }
}
