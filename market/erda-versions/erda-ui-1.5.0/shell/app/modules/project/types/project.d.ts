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

declare namespace PROJECT {
  type ProjectType = 'DevOps' | 'codeHostingProject' | 'agileProject' | 'MSP';

  type BlockStatus = 'unblocking' | 'unblocked' | 'blocked';

  interface EnvConfig<T> {
    DEV: T;
    PROD: T;
    STAGING: T;
    TEST: T;
    DEFAULT: T;
  }

  interface ListQuery {
    orgId?: number;
    pageNo: number;
    pageSize: number;
    query?: string;
    orderBy?: 'cpuQuota' | 'memQuota' | 'activeTime';
    asc?: boolean;
  }

  interface Detail {
    id: number;
    joined: boolean;
    logo: string;
    name: string;
    displayName: string;
    isPublic: boolean;
    creator: string;
    ddHook: string;
    desc: string;
    orgId: number;
    stats: ProjectStats;
    owners: string[];
    cpuQuota: number;
    memQuota: number;
    cpuServiceUsed: number;
    memServiceUsed: number;
    cpuAddonUsed: number;
    memAddonUsed: number;
    clusterConfig: EnvConfig<string>;
    resourceConfig: EnvConfig<ICluster>;
    rollbackConfig: EnvConfig<number>;
    createdAt: string;
    updatedAt: string;
    canUnblock?: boolean;
    blockStatus: BlockStatus;
    type: ProjectType;
  }

  interface ProjectStats {
    doneBugPercent: number;
    countApplications: number;
    countMembers: number;
    doneBugCount: number;
    planningIterationsCount: number;
    planningManHourCount: number;
    runningIterationsCount: number;
    totalApplicationsCount: number;
    totalBugCount: number;
    totalIterationsCount: number;
    totalManHourCount: number;
    totalMembersCount: number;
    usedManHourCount: number;
  }

  interface CreateBody {
    orgId: number;
    name: string;
    clusterId: number;
    logo?: string;
    desc?: string;
  }

  interface UpdateBody {
    name?: string;
    logo?: string;
    desc?: string;
    ddHook?: string;
    cpuQuota?: number;
    memQuota?: number;
    isPublic?: boolean;
    clusterConfig?: ClusterConfig;
  }

  interface ClusterConfig {
    DEV: string;
    PROD: string;
    STAGING: string;
    TEST: string;
  }

  interface GetAppsQuery {
    pageNo: number;
    pageSize?: number;
    projectId: number | string;
    q?: string;
    searchKey?: string;
    loadMore?: boolean;
  }

  interface LeftResources {
    totalCpu: number;
    totalMem: number;
    availableCpu: number;
    availableMem: number;
    clusterList: ICluster[];
  }

  interface ICluster {
    clusterName: string;
    cpuAvailable: number;
    cpuQuota: number;
    cpuQuotaRate: number;
    cpuRequest: number;
    cpuRequestRate: number;
    cpuRequestByService: number;
    cpuRequestByServiceRate: number;
    cpuRequestByAddon: number;
    cpuRequestByAddonRate: number;
    memAvailable: number;
    memQuota: number;
    memQuotaRate: number;
    memRequest: number;
    memRequestRate: number;
    memRequestByService: number;
    memRequestByServiceRate: number;
    memRequestByAddon: number;
    memRequestByAddonRate: number;
    workspace: string;
    tips?: string;
  }

  interface IBranchRule extends IBranchRuleCreateBody {
    id: number;
  }

  interface IBranchRuleCreateBody {
    artifactWorkspace: string | string[]; // 制品部署环境
    desc: string; // 描述
    isProtect: boolean; // 是否保护
    isTriggerPipeline: boolean; // 是否推送
    rule: string; // 规则
    workspace: string; // 部署环境
    needApproval: boolean; // 应用发布确认
    scopeId: number;
    scopeType: string;
  }

  interface Approves {
    targetId: number;
    extra: {
      appIDs: string;
      start: string;
      end: string;
    };
    type: string;
    desc: string;
  }

  interface ITestReportBody {
    name: string;
    summary: string;
    iterationID: number;
    reportData: {
      'test-dashboard': Obj;
      'issue-dashboard': Obj;
    };
  }
}
