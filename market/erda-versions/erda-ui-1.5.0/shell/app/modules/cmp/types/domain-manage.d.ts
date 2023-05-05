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

declare namespace DOMAIN_MANAGE {
  type IWorkspace = 'dev' | 'prod';

  interface IDomain {
    id: number;
    domain: string;
    clusterName: string;
    type: string;
    projectName: string;
    appName: string;
    workspace: IWorkspace;
    link: {
      projectID: string;
      appID: string;
      runtimeID: string;
      serviceName: string;
      workspace: IWorkspace;
      tenantGroup: string;
    };
  }
}
