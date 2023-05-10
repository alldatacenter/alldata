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

declare namespace ADDON {
  type ADDON_STATUS = 'ATTACHING' | 'ATTACHED' | 'ATTACHFAILED';

  interface Instance {
    tenantOwner: string; // 实例为 tenant 时有值
    instanceId: string;
    name: string;
    tag: string;
    addonName: string;
    displayName: string;
    desc: string;
    logoUrl: string;
    plan: string;
    mysqlAccountState: 'CUR' | 'PRE';
    version: string;
    category: string;
    config: {
      [k: string]: string;
    };
    customAddonType: 'cloud' | 'custom';
    shareScope: string;
    cluster: string;
    orgId: number;
    projectId: number;
    projectName: string;
    workspace: string;
    status: ADDON_STATUS;
    realInstanceId: string;
    reference: number;
    attachCount: number;
    platform: true;
    platformServiceType: 0 | 1 | 2; // 0:中间件  1:微服务  2：通用平台
    canDel: boolean;
    consoleUrl: string | null;
    createdAt: string;
    updatedAt: string;
    recordId: string;
  }

  interface Reference {
    orgId: number;
    projectId: number;
    projectName: string;
    applicationId: number;
    applicationName: string;
    runtimeId: number;
    runtimeName: string;
  }

  interface DataSourceAddon {
    projectId: number;
    displayName: string[];
  }
}
