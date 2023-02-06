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

declare namespace API_ACCESS {
  type EnvEnum = 'dev' | 'test' | 'staging' | 'pro';
  type AuthenticationEnum = 'key-auth' | 'sign-auth';
  type AuthorizationEnum = 'auto' | 'manual';
  type Workspace = 'DEV' | 'TEST' | 'STAGING' | 'PROD';
  type ContractStatue = 'proving' | 'proved' | 'disproved' | 'unproved';
  type SlaApproval = 'auto' | 'manual';
  type SLASource = 'system' | 'user';
  type SlaLimitUnit = 's' | 'm' | 'h' | 'd';
  type AddonStatus =
    | 'PENDING'
    | 'ATTACHING'
    | 'ATTACHED'
    | 'ATTACHFAILED'
    | 'DETACHING'
    | 'DETACHED'
    | 'OFFLINE'
    | 'UPGRADE'
    | 'ROLLBACK'
    | 'UNKNOWN';

  interface QueryAccess {
    paging: boolean;
    pageNo: number;
    pageSize?: number;
    keyword?: string;
  }

  interface AccessID {
    accessID: number;
  }

  interface AccessPermission {
    edit: boolean;
    delete: boolean;
  }

  interface SubAccess {
    id: number;
    swaggerVersion: string;
    appCount: number;
    createdAt: string;
    permission: AccessPermission;
  }

  interface AccessListItem {
    accessName: string;
    assetID: string;
    children: SubAccess[];
    totalChildren: number;
  }

  type ITableSubAccess = Merge<API_ACCESS.SubAccess, { parents: Omit<API_ACCESS.AccessListItem, 'children'> }>;

  type ITableData = Merge<Omit<API_ACCESS.AccessListItem, 'children'>, { subData: ITableSubAccess[] }>;

  interface CreateAccess {
    assetID: string;
    major: number;
    minor: number;
    projectID: number;
    workspace: EnvEnum;
    addonInstanceID: string;
    authentication: AuthenticationEnum;
    authorization: AuthorizationEnum;
    bindDomains: string[];
  }

  interface Access {
    id: number;
    accessID: number;
    assetID: string;
    swaggerVersion: string;
    orgID: number;
    major: number;
    minor: number;
    projectID: number;
    appID: number;
    workspace: API_ACCESS.Workspace;
    endpointID: string;
    addonInstanceID: string;
    authentication: AuthenticationEnum;
    authorization: AuthorizationEnum;
    bindDomain: string[];
    creatorID: number;
    updaterID: number;
    createAt: string;
    updaterAt: string;
    projectName: string;
    appName: string;
    assetName: string;
    endpointName: string;
  }

  interface QueryClient {
    assetID: string;
    swaggerVersion: string;
    pageNo: number;
    pageSize?: number;
    paging: boolean;
  }

  interface AccessDetail {
    access: Access;
    tenantGroup: {
      TenantGroupID: string;
    };
    permission: {
      delete: boolean;
      edit: boolean;
    };
  }

  interface ClientPermission {
    edit: boolean;
  }

  interface Client {
    client: API_CLIENT.Client;
    contract: API_CLIENT.Contract;
    permission: ClientPermission;
  }

  interface QueryContractInfo {
    clientID: number;
    contactID: number;
  }

  interface ContractInfo {
    client: API_CLIENT.Client;
    contract: API_CLIENT.Contract;
    sk: API_CLIENT.ClientSk;
  }

  interface OperationRecord {
    id: number;
    orgID: number;
    contractID: number;
    action: string;
    creatorID: string;
    createdAt: string;
  }

  interface ApiGateway {
    addonInstanceID: string;
    status: AddonStatus;
    workspace: Workspace;
  }

  interface OperateContract {
    clientID: string;
    contractID: number;
    status?: ContractStatue;
    curSLAID?: number;
    requestSLAID?: number;
  }

  interface BaseSlaLimit {
    limit: number;
    unit: SlaLimitUnit;
  }

  interface SlaLimit {
    sla_id: string;
    createdAt: string;
    id: number;
    unit: SlaLimitUnit;
    updatedAt: string;
    creatorID: number;
    orgID: number;
    limit: number;
    updaterID: number;
  }

  interface GetSlaList {
    assetID: string;
    swaggerVersion: string;
  }

  interface GetSla {
    assetID: string;
    swaggerVersion: string;
    slaID: number;
  }

  interface SlaItem {
    name: string;
    id: number;
    desc: string;
    createdAt: string;
    approval: SlaApproval;
    assetID: string;
    default: boolean;
    updatedAt: string;
    creatorID: number;
    swaggerVersion: string;
    accessID: number;
    orgID: number;
    limits: SlaLimit[];
    usedTo: string;
    updaterID: number;
    assetName: string;
    clientCount: number;
    source: SLASource;
  }

  interface SlaData {
    total: number;
    list: SlaItem[];
  }

  interface AddSla {
    assetID: string;
    swaggerVersion: string;
    name: string;
    desc: string;
    approval: SlaApproval;
    default: boolean;
    limits: BaseSlaLimit[];
  }

  type UpdateSla = Merge<AddSla, { slaID: number }>;

  interface DeleteSla {
    assetID: string;
    swaggerVersion: string;
    slaID: number;
  }

  type DashboardType = 'apim_summary' | 'apim_client';

  interface QueryDashboard {
    type: DashboardType;
    endpoint?: string;
    client?: string;
    workSpace?: Workspace;
    projectID?: number;
  }
}
