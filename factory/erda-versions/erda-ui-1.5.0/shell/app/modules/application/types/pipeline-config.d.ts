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

declare namespace PIPELINE_CONFIG {
  interface Query {
    namespace_name: string;
    decrypt?: boolean;
  }

  interface NamespaceItem {
    id: string;
    namespace: string;
    workspace: string;
  }

  type ConfigType = 'kv' | 'dice-file';

  interface ConfigItem {
    key: string;
    value: string;
    comment: string;
    status: string;
    source: string;
    type: ConfigType;
    encrypt: boolean;
    createTime: string;
    updateTime: string;
    isFromDefault?: boolean;
  }

  interface AddConfigsQuery {
    apiPrefix?: string;
    appID: string;
    namespace_name: string;
    encrypt: boolean;
  }

  interface AddConfigsBody {
    apiPrefix?: string;
    query: AddConfigsQuery;
    configs: ConfigItem[];
  }

  interface AddConfigsBodyWithoutAppId {
    query: Omit<AddConfigsQuery, 'appID'>;
    configs: ConfigItem[];
  }

  interface importConfigsBody {
    query: Omit<AddConfigsQuery, 'appID' | 'encrypt'>;
    configs: string;
  }

  interface DeleteConfigQuery {
    apiPrefix?: string;
    appID: string;
    namespace_name: string;
    key: string;
  }

  interface ConfigQuery {
    apiPrefix?: string;
    namespace_name: string;
    decrypt: boolean;
  }

  interface ConfigItemMap {
    [k: string]: ConfigItem[];
  }

  interface NameSpaces {
    namespaces: NamespaceItem[];
  }

  interface IConfigItem {
    key: string;
    value: string;
    comment?: string;
  }
}
