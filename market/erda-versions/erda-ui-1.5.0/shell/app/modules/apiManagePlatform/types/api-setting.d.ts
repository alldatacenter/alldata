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

declare namespace API_SETTING {
  type PropertyType = 'object' | 'string' | 'number' | 'boolean' | 'array';
  type ApiMethod = 'get' | 'post' | 'put' | 'delete' | 'options' | 'head' | 'patch';

  enum Method {
    get = 'get',
    post = 'post',
    delete = 'delete',
    put = 'put',
    patch = 'patch',
    head = 'head',
  }

  interface ICreateTreeNode {
    type: string;
    pinode: string;
    name: string;
    meta: {
      content: string | undefined;
    };
  }

  interface ITreeNode {
    type: string;
    inode: string;
    pinode: string;
    name: string;
    desc: string;
    scope: string;
    scopeID: number;
    creatorID: number;
    updaterAt: string;
  }

  interface ITreeListQuery {
    pinode: string;
    scope?: string;
    scopeID?: number;
  }
  interface ICommonTreeData {
    type: string;
    inode: string;
    pinode: string;
    name: string;
    scope: string;
    scopeID: number;
    creatorID: number;
    createdAt: string;
    updaterID: number;
    updatedAt: string;
  }
  interface IMetaLock {
    locked: boolean;
    userID: number;
    nickName: string;
  }
  interface IFileTree extends ICommonTreeData {
    meta: {
      lock: IMetaLock;
      readOnly?: boolean;
      valid?: boolean;
      error?: string;
      hasDoc?: boolean;
    };
    key?: string;
    children?: any[];
  }
  interface ISchemaParams extends ICommonTreeData {
    meta: {
      lock: IMetaLock;
      schemas: {
        [props: string]: {
          required: string[];
          type: 'object';
          properties: {
            [props: string]: {
              type: PropertyType;
              example: any;
            };
          };
          list: any[];
        };
      };
    };
  }

  interface IProperty {
    example: string;
    type: PropertyType;
  }

  interface IResponse {
    [prop: string]: {
      description: string;
      body: Obj;
    };
  }

  type IApiResource = {
    [prop in Method]: {
      queryParameters: {
        [prop: string]: IProperty;
      };
      displayName: string;
      description: string;
    };
  } & {
    responses?: IResponse;
    apiName?: string;
  };

  interface IDataType {
    id: number;
    name: string;
  }

  interface IApiDetail extends ICommonTreeData {
    meta: {
      lock: IMetaLock;
      blob: {
        binary: boolean;
        content: string;
        refName: string;
        path: string;
        size: number;
      };
      asset: IApiAsset;
      readOnly?: boolean;
      valid?: boolean;
      error?: string;
    };
  }
  interface IQuotePath {
    [props: string]: string[][];
  }
  interface IResourceProps {
    apiName: string;
    apiDetail: Obj;
    quotePathMap: IQuotePath;
    onQuoteChange: (e: IQuotePath) => void;
    onApiNameChange: (name: string) => void;
  }

  interface IPublishAPi {
    assetName: string;
    assetID: string;
    source: string;
    projectID: number;
    appID: number;
    orgId: number;
    versions?: Obj[];
  }

  interface IApiAssetsQuery {
    hasProject: boolean;
    paging: boolean;
    latestVersion: boolean;
    latestSpec: boolean;
    scope: string;
  }

  interface IApiAsset {
    assetID: string;
    assetName: string;
    major: number;
    minor: number;
    patch: number;
  }

  interface ITreeNodeData {
    inode: string;
    asset: API_SETTING.IApiAsset;
    apiDocName: string;
  }

  interface IApiDocDetail {
    openApiDoc: Obj;
    name: string;
    asset: IApiAsset;
    readOnly: boolean;
  }

  interface ISwaggerValidator {
    success: boolean;
    err?: string;
  }
}
