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

import agent from 'agent';
import { RES_BODY } from 'core/service';

export const requestTrace = (data?: MONITOR_TRACE.ITraceRequestBody): { requestId: string } => {
  return agent
    .post('/api/spot/trace-requests')
    .send(data)
    .then((response: any) => response.body);
};

// https://yuque.antfin-inc.com/docs/share/e59c5eb3-fbfe-44f7-81a2-567e2fc26703
// request列表（history）
export const getTraceHistoryList = (query: { terminusKey: string }): MONITOR_TRACE.ITraceRequestResp => {
  return agent
    .get('/api/spot/trace-requests')
    .query({ limit: 200, ...query })
    .then((response: any) => response.body);
};

// https://yuque.antfin-inc.com/docs/share/e59c5eb3-fbfe-44f7-81a2-567e2fc26703#gsiniv
// request详情
export const getTraceDetail = ({
  requestId,
  ...query
}: {
  requestId: string;
  terminusKey: string;
}): MONITOR_TRACE.IHistory => {
  return agent
    .get(`/api/spot/trace-requests/${requestId}`)
    .query(query)
    .then((response: any) => response.body);
};

// https://yuque.antfin-inc.com/docs/share/e59c5eb3-fbfe-44f7-81a2-567e2fc26703#d6hcaf
// 查看Request的状态
export const getTraceStatus = ({
  requestId,
  ...query
}: {
  requestId: string;
  terminusKey: string;
}): MONITOR_TRACE.IStatus => {
  return agent
    .get(`/api/spot/trace-requests/${requestId}/status`)
    .query(query)
    .then((response: any) => response.body);
};

// https://yuque.antfin-inc.com/docs/share/e59c5eb3-fbfe-44f7-81a2-567e2fc26703#i9e3gi
// 主动取消status
export const cancelTraceStatus = ({ requestId, ...query }: { requestId: string; terminusKey: string }) => {
  return agent
    .put(`/api/spot/trace-requests/${requestId}/actions/cancel`)
    .query(query)
    .then((response: any) => response.body);
};

export const getTraceDetailContent = ({ traceId, ...query }: MONITOR_TRACE.IQuerySpan): MONITOR_TRACE.ITrace => {
  return agent
    .get(`/api/msp/apm/traces/${traceId}/spans`)
    .query(query)
    .then((response: any) => response.body);
};

export const getSpanDetailContent = ({ span, visible }: { span: any; visible: boolean }) => {
  return {
    visible,
    span,
  };
};

export const getQueryConditions = (): Promise<RES_BODY<MONITOR_TRACE.TraceConditions>> => {
  return agent.get('/api/msp/apm/trace/conditions').then((response: any) => response.body);
};
