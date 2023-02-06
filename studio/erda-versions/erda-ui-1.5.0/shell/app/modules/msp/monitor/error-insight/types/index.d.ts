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

declare namespace MONITOR_ERROR {
  interface IErrorQuery {
    startTime: number;
    endTime: number;
    scopeId: string;
  }

  interface IErrorResp {
    errors: IError[];
    limit: number;
    offset: number;
    total: number;
  }

  interface IError {
    id: string;
    className: string;
    method: string;
    type: string;
    eventCount: number;
    exceptionMessage: string;
    file: string;
    applicationID: string;
    runtimeID: string;
    serviceName: string;
    scopeID: string;
    createTime: string;
    updateTime: string;
  }

  interface IEventIdsQuery {
    id: string;
    errorType: 'error-detail' | 'request-detail';
    terminusKey: string;
  }

  interface IEventDetailQuery {
    exceptionEventId: string;
    scopeId: string;
  }

  interface IEventDetail {
    id: string;
    exceptionId: string;
    metadata: Obj<string>;
    requestContext: Obj<string>;
    requestHeaders: Obj<string>;
    requestId?: string;
    stacks: { stack: IStacks }[];
    tags: Obj<string>;
    timestamp: string;
    requestSampled: boolean;
  }

  interface IStacks {
    className: string;
    fileName: string;
    index: number;
    line: number;
    methodName: string;
  }
}
