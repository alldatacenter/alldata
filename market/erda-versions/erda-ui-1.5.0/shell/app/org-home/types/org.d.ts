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

declare namespace ORG {
  interface IOrg {
    createdAt: string;
    creator: string;
    desc: string;
    domain: string;
    isPublic: boolean;
    id: number;
    logo: string;
    name: string;
    displayName: string;
    operation: string;
    selected: boolean;
    status: string;
    type: 'FREE' | 'ENTERPRISE';
    updatedAt: string;
    publisherId: number;
    blockoutConfig: {
      blockDev: boolean;
      blockTest: boolean;
      blockStage: boolean;
      blockProd: boolean;
    };
    openFdp?: boolean;
  }

  interface IOrgReq {
    domain: string;
    orgName: string;
  }
}
