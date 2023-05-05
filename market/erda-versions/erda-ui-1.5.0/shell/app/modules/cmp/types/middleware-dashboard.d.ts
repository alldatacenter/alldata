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

declare namespace MIDDLEWARE_DASHBOARD {
  interface IMiddlewaresQuery {
    pageNo?: number;
    pageSize?: number;
    workspace?: string;
    addonName?: string;
    projectId?: number;
  }

  interface IMiddlewareDetail {
    instanceId: string;
    addonName: string;
    clusterName: string;
    projectName: string;
    env: string;
    name: string;
    cpu: number;
    mem: number;
    nodes: number;
    attachCount: number;
    isOperator: boolean;
  }

  interface IOverview {
    cpu: number;
    mem: number;
    nodes: number;
  }

  interface IMiddlewaresResp {
    total: number;
    overview: IOverview;
    list: IMiddlewareDetail[];
  }

  interface IBaseInfo {
    instanceId: string;
    addonName: string;
    logoUrl: string;
    plan: string;
    version: string;
    projectId: string;
    category: string;
    workspace: string;
    status: string;
    attachCount: number;
    config: { [k: string]: string };
    referenceInfos: ADDON.Reference[];
    cluster: string;
    createdAt: string;
    updatedAt: string;
    isOperator: boolean;
    name: string;
    projectName: string;
  }

  interface IResource {
    instanceId: string;
    containerId: string;
    containerIP: string;
    hostIP: string;
    image: string;
    clusterName: string;
    cpuRequest: number;
    cpuLimit: number;
    memRequest: number;
    memLimit: number;
    status: string;
    startedAt: string;
  }

  interface AddonUsageQuery {
    projectId?: string;
    addonName?: string;
    workspace?: string;
  }

  interface AddonUsage {
    [k: string]: {
      cpu: number;
      mem: number;
    };
  }

  interface AddonDailyUsage {
    abscissa: string[];
    resource: Array<{
      cpu: number;
      mem: number;
    }>;
  }

  interface IScaleData {
    addonName: string;
    clusterName: string;
    addonID: string;
    cpu: number;
    mem: number;
    nodes: number;
    projectID: string;
  }

  interface IMiddleBase {
    addonName: string;
    clusterName: string;
    addonID: string;
  }

  interface IBackupFile {
    name: string;
    status: string;
  }

  interface IBackupFiles extends IMiddleBase {
    backupfile: string[];
  }

  interface IUpdateBackup extends IMiddleBase {
    backupName: string;
    backupRules: string;
    backupClean: string;
    backupOn: boolean;
  }

  interface IUpdateConfig extends IMiddleBase {
    config: {
      [key: string]: string | boolean | number;
    };
  }

  interface IActionsConfig {
    config: {
      [key: string]: string | boolean | number;
    };
    cpu: number;
    mem: number;
    nodes: number;
  }

  interface IAddonStatus extends IMiddleBase {
    nodes: number;
    status: string;
    restoreStatus: string;
  }
  interface LeftResources {
    totalCpu: number;
    totalMem: number;
    availableCpu: number;
    availableMem: number;
  }
}
