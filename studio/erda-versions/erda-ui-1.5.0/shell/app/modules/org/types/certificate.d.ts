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

declare namespace Certificate {
  type Type = 'IOS' | 'Android' | 'Message';

  interface Detail {
    id?: string;
    androidInfo?: {
      autoInfo?: {
        city: string;
        ou: string;
        org: string;
        province: string;
        state: string;
        name: string;
        debugKeyStore: {
          keyPassword: string;
          storePassword: string;
        };
        releaseKeyStore: {
          keyPassword: string;
          storePassword: string;
        };
      };
      manualCreate: boolean;
      manualInfo?: {
        debugKeyStore: {
          fileName: string;
          keyPassword: string;
          storePassword: string;
          uuid: string;
        };
        releaseKeyStore: {
          fileName: string;
          keyPassword: string;
          storePassword: string;
          uuid: string;
        };
      };
    };
    iosInfo?: {
      debugProvision: {
        fileName: string;
        uuid: string;
      };
      keyChainP12: {
        fileName: string;
        password: string;
        uuid: string;
      };
      releaseProvision: {
        fileName: string;
        uuid: string;
      };
    };
    messageInfo?: {
      fileName: string;
      uuid: string;
    };
    name: string;
    orgId: number;
    type: string;
    desc?: string;
  }

  interface UpdateBody {
    id: number | string;
    desc: string;
    uuid: string; // 证书上传返回的地址
    filename: string;
  }

  interface ListQuery {
    appID: number;
    type: Type;
    pageNo: number;
    pageSize: number;
  }
}

type ApprovalStatus = 'pending' | 'approved' | 'denied';
