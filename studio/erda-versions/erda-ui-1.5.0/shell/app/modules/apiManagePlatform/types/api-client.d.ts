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

declare namespace API_CLIENT {
  type ContractStatue = 'proving' | 'proved' | 'disproved';

  interface QueryClient {
    pageSize?: number;
    pageNo?: number;
    paging: boolean;
    keyword?: string;
  }

  interface CreateClient {
    name: string;
    desc: string;
    displayName: string;
  }

  interface UpdateClient extends CreateClient {
    clientID: number;
    resetClientSecret: boolean;
  }

  interface Common {
    clientID: number;
  }

  interface Client {
    id: number;
    orgID: number;
    name: string;
    displayName: string;
    desc: string;
    clientID: string;
    creatorID: string;
    updaterID: string;
    createdAt: string;
    updatedAt: string;
  }

  interface ClientSk {
    clientID: string;
    clientSecret: string;
  }

  interface ClientItem {
    client: Client;
    sk: ClientSk;
  }

  interface QueryContract extends Common {
    contractID: number;
  }

  interface QueryContractList extends Common {
    status: ContractStatue;
    pageNo?: number;
    pageSize?: number;
    paging: boolean;
  }

  interface Contract {
    id: number;
    assetID: string;
    orgID: number;
    major: number;
    minor: number;
    clientID: string;
    creatorID: number;
    updaterID: number;
    status: ContractStatue;
    createAt: string;
    updateAt: string;
    swaggerVersion: string;
    curSLAID: number;
    curSLAName: string;
    requestSLAID: number;
    requestSLAName: string;
    slaCommittedAt: string;
    clientDisplayName: string;
    workspace: API_ACCESS.Workspace;
    projectID: number;
    endpointName: string;
    clientName: string;
  }

  type ContractItem = Merge<ClientItem, Contract>;

  interface CreteContract {
    slaID: number;
    assetID: string;
    clientID: number;
    swaggerVersion: string;
  }
}
