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

declare namespace APP_SETTING {
  interface PublisherConfigItem {
    Env: string;
    appId: number;
    publisherId: number;
    publisherName: string;
    publishItemId: number;
    publishItemName: string;
  }

  interface PublisherConfig {
    DEV: PublisherConfigItem;
    TEST: PublisherConfigItem;
    STAGING: PublisherConfigItem;
    PROD: PublisherConfigItem;
  }

  interface PublisherUpdateBody {
    appID: string | number;
    DEV: number;
    TEST: number;
    STAGING: number;
    PROD: number;
  }

  interface LibRef {
    id: number;
    appID: number;
    libID: number;
    libName: string;
    libDesc: string;
    approvalStatus: ApprovalStatus;
    createdAt: string;
  }

  interface AddLibRef {
    appID: number;
    appName: string; // 审批展示用
    libID: number;
    libName: string;
    libDesc: string;
  }

  interface CertRefQuery {
    appId: number;
    pageNo: number;
    pageSize: number;
  }

  interface AddCertRef {
    appId: number;
    certificateId: number;
  }

  interface CertRef extends Certificate.Detail {
    appId: number;
    certificateId: number;
    status: ApprovalStatus;
    pushConfig: {
      enable: boolean;
      key: string;
      envs: string[];
    };
  }

  interface PushToConfigBody {
    appId: number;
    certificateId: number;
    key: string;
    envs: string[];
  }
}
