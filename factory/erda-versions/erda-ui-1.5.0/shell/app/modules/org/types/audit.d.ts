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

declare namespace AUDIT {
  interface Item {
    id: number;
    userId: string;
    action: string;
    scopeType: string;
    scopeId: number;
    appId: number;
    projectId: number;
    orgId: number;
    resourceType: string;
    resourceId: number;
    context: {
      [k: string]: string;
    };
    templateName: string;
    auditLevel: string;
    result: string;
    errorMsg: string;
    startTime: number;
    clientIp: string;
    userAgent: string;
  }

  interface ListQuery {
    sys?: boolean; // 如果为true，为查看平台后台日志，为false时需要传orgId
    orgId?: number; // 企业id
    startAt: number; // 开始时间，YYYY-MM-DD HH:mm:ss格式，必选参数
    endAt: number; // 结束时间，YYYY-MM-DD HH:mm:ss格式，必选参数
    userId?: string; // 根据用户id过滤事件，可选参数
    pageNo: number;
    pageSize: number;
    resourceType?: string; // 根据资源类型过滤时间，可选参数
    auditLevel?: string; // 根据审计级别过滤事件，可选参数
  }

  interface LogSetting {
    interval: number;
  }

  interface LogSettingBody {
    orgId: number;
    interval: number;
  }
}
