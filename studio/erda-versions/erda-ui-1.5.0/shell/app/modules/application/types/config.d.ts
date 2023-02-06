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

declare namespace APP_CONFIG {
  interface IBaseQuery {
    namespace_name: string;
    decrypt?: boolean;
    encrypt?: boolean;
  }

  interface IBaseResponse {
    body: object;
  }

  interface IConfigItem {
    key: string;
    value: string;
    comment?: string;
  }
  interface QueryConfig {
    namespaceParams: IBaseQuery[];
  }

  interface AddConfig {
    query: IBaseQuery;
    configs: IConfigItem[];
  }

  interface DeleteConfig {
    namespace_name: string;
    key: string;
  }
}
