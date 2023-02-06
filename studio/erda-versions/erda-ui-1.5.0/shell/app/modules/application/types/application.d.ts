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

declare namespace APPLICATION {
  type Workspace = 'DEV' | 'TEST' | 'STAGING' | 'PROD';
  interface createBody {
    mode: string;
    name: string;
    desc: string;
    logo: string;
    projectId: number;
    repoConfig?: GitRepoConfig;
    isExternalRepo: boolean;
  }

  interface GitRepoConfig {
    type: string;
    username: string;
    password: string;
    url: string;
    desc: string;
  }

  interface initApp {
    mobileAppName: string;
    mobileDisplayName: string;
    bundleID: string;
    packageName: string;
    applicationID: number;
  }

  interface queryTemplate {
    mode: string;
  }

  type appMode = 'SERVICE' | 'MOBILE' | 'LIBRARY' | 'BIGDATA' | 'ABILITY';

  interface IQuery {
    pageNo: number;
    pageSize: number;
  }

  interface GetAppList {
    pageSize: number;
    pageNo: number;
    projectId: number | string;
    q?: string;
    searchKey?: string;
    loadMore?: boolean;
    memberID?: string;
  }

  interface IBranchInfo {
    artifactWorkspace: string;
    isProtect: boolean;
    name: string;
    workspace: Workspace;
  }
}

interface IApplication {
  config: null;
  createdAt: string;
  creator: string;
  isPublic?: boolean;
  desc: string;
  gitRepo: string;
  gitRepoAbbrev: string;
  gitRepoNew: string;
  id: number;
  logo: string;
  mode: APPLICATION.appMode;
  name: string;
  displayName: string;
  orgId: number;
  orgName: string;
  orgDisplayName: string;
  pined: false;
  projectId: number;
  projectName: string;
  projectDisplayName: string;
  stats: {
    countRuntimes: number;
    countMembers: number;
    timeLastModified: string;
  };
  countMembers: number;
  countRuntimes: number;
  timeLastModified: string;
  updatedAt: string;
  workspaces: IAppWorkspace[];
  clusterName: string;
  configNamespace: string;
  workspace: string;
  token: string;
  isExternalRepo: boolean;
  repoConfig?: APPLICATION.GitRepoConfig;
  unBlockStart: string;
  unBlockEnd: string;
  blockStatus: PROJECT.BlockStatus;
  isProjectLevel: boolean;
}

interface IAppWorkspace {
  clusterName: string;
  workspace: string;
  configNamespace: string;
}
