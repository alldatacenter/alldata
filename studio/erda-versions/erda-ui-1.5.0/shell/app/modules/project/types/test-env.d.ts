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

declare namespace TEST_ENV {
  type EnvType = 'case' | 'project';

  interface Item extends CreateBody {
    id: number;
  }

  interface ICommonCreateQuery {
    name: string;
    domain: string;
    header: {
      [k: string]: string;
    };
    global: {
      [k: string]: {
        value: string;
        type: string;
      };
    };
  }

  interface CreateBody extends ICommonCreateQuery {
    envType: EnvType;
    envID: number;
  }

  interface ICreateAutoTestEnv {
    ns?: number;
    scopeID: string;
    scope: string;
    apiConfig: ICommonCreateQuery;
    displayName: string;
    desc: string;
  }

  interface IAutoEnvItem {
    ns: string;
    name: string;
    domain: string;
    header: {
      [k: string]: string;
    };
    global: {
      [k: string]: {
        value: string;
        type: string;
      };
    };
    projectID: number;
    orgID: number;
    displayName: string;
    desc: string;
  }

  interface IAutoEnvData {
    apiConfig: {
      name: string;
      domain: string;
      header: {
        [k: string]: string;
      };
      global: {
        [k: string]: {
          value: string;
          type: string;
        };
      };
    };
    projectID: number;
    orgID: number;
    displayName: string;
    desc: string;
    ns: string;
  }

  interface EnvListQuery {
    envID: number;
    envType: EnvType;
  }

  interface IEnv {
    id?: number;
    name: string;
    domain: string;
    header: object;
    global: object;
  }

  interface IAutoEnvQuery {
    scope: string;
    scopeID: number;
  }
}
