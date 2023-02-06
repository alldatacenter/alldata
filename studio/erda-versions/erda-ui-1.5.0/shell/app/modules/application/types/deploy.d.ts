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

declare namespace DEPLOY {
  interface Runtime {
    clusterId: number;
    clusterName: string;
    createdAt: string;
    deleteStatus: string;
    deployStatus: string;
    extra: {
      applicationId: number;
      buildId: number;
      workspace: WORKSPACE;
      fakeRuntime: boolean;
    };
    id: number;
    lastOperateTime: string;
    lastOperator: string;
    lastOperatorAvatar: string;
    lastOperatorName: string;
    name: string;
    projectID: number;
    releaseId: string;
    resources: { cpu: number; disk: number; mem: number };
    source: 'PIPELINE';
    status: string;
    timeCreated: string;
    updatedAt: string;
  }

  interface ExtensionAction {
    id: number;
    type: 'action';
    name: string;
    desc: string;
    displayName: string;
    category: string;
    logoUrl: string;
    public: boolean;
    group: string;
    groupDisplayName: string;
  }

  interface IGroupExtensionActionObj {
    name: string;
    displayName: string;
    items: ExtensionAction[];
  }

  interface ActionConfig {
    name: string;
    version: string;
    type: 'action';
    spec: {
      category: string;
      desc: string;
      name: string;
      options: {
        fetchHistoryBPConfig: boolean;
      };
      params: Array<{
        desc: string;
        name: string;
        required: boolean;
      }>;
      public: boolean;
      supportedVersions: string[];
      type: 'action';
      version: string;
    };
    dice: {
      jobs: {
        [keyName: string]: {
          envs: { [s: string]: string };
          image: string;
          resources: {
            cpu: number;
            disk: number;
            mem: number;
          };
        };
      };
    };
    readme: string;
    isDefault: false;
    public: boolean;
  }

  interface AddByRelease {
    releaseId: string;
    workspace: string;
    projectId: number;
    applicationId: number;
  }

  interface IRelease {
    releaseId: string;
    releaseName: string;
    labels: {
      gitCommitId: string;
      gitCommitMessage: string;
    };
    createdAt: string;
  }
  interface IReleaseMap {
    [k: string]: {
      total: number;
      list: IRelease[];
    };
  }

  interface IDeploy {
    commitMessage: string;
    commitId: string;
    branchName: string;
    projectId: number;
    projectName: string;
    applicationId: number;
    applicationName: string;
    runtimeName: string;
    finishedAt: string;
    createdAt: string;
    operatorAvatar: string;
    operatorName: string;
    operator: string;
    approvedAt: string;
    approvedByUser: string;
    needApproval: boolean;
    outdated: boolean;
    failCause: string;
    step: string;
    phase: string;
    status: string;
    type: string;
    releaseName: string;
    releaseId: string;
    buildId: string;
    runtimeId: number;
    id: number;
    approvalStatus: string;
    approvalReason: string;
  }

  interface IUpdateApproveBody {
    id: number;
    reject: boolean;
    reason?: string;
  }
}
