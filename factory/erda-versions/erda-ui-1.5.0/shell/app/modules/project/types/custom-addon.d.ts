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

declare namespace CUSTOM_ADDON {
  interface Plan {
    label: string;
    value: string;
  }
  interface Item {
    addonName: string;
    category: string;
    createdAt: string;
    desc: string;
    displayName: string;
    id: number;
    logoUrl: string;
    name: string;
    public: true;
    supportTenant: true;
    plan: Plan[];
    type: string;
    updatedAt: string;
    vars: string[] | null;
    tenantVars: string[] | null;
    version: string;
  }

  interface InsQuery {
    type: string;
    vendor: string;
    projectID: string;
    workspace: string;
  }

  interface AddBody {
    addonName: string;
    configs: Obj;
    name: string;
    customAddonType: 'cloud' | 'custom';
    extra: Obj;
    projectId: number;
    tag: string;
    workspace: string;
  }

  interface AddDiceAddOns {
    addons: {
      [k: string]: Obj<string>;
    };
    shareScope: string;
    workspace: string;
    projectId: number;
    clusterName: string;
  }

  interface AddTenantAddon {
    addonInstanceRoutingId: string;
    name: string;
    configs: Obj<string>;
  }

  interface UpdateBody {
    projectId: number;
    instanceId: string;
    orgId: number;
    config: Obj;
    operatorId: number;
  }

  interface CloudInstance {
    chargeType: string;
    id: string;
    name: string;
    spec: string;
    status: string;
    version: string;
  }

  interface QueryCustoms {
    org_id: number;
    project_id?: string;
  }

  interface QueryCloudGateway {
    vendor: string;
    projectID: string;
    workspace: string;
  }

  interface GatewayInstance {
    name: string;
    instanceID: string;
  }

  interface CloudGateway {
    slbs: GatewayInstance[];
    gateways: GatewayInstance[];
  }
}
