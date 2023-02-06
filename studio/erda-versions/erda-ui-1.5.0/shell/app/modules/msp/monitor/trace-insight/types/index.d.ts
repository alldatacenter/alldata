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

declare namespace MONITOR_TRACE {
  interface ITraceRequestResp {
    limit: number;
    offset: number;
    total: number;
    history: IHistory[];
  }

  interface IHistory {
    body: string;
    createTime: string;
    header: Obj<string>;
    method: string;
    query: Obj<string>;
    requestId: string;
    responseBody: string;
    responseCode: number;
    status: number;
    statusName: string;
    terminusKey: string;
    updateTime: string;
    url: string;
  }

  interface ITraceRequestBody {
    createTime: string;
    requestId: string;
    status: number;
    statusName: string;
    updateTime: string;
    startTime: number;
  }

  interface IStatus {
    requestId: string;
    status: number;
    statusName: string;
    terminusKey: string;
  }

  interface ITrace {
    spans: Array<ITraceSpan>;
    duration: number;
    serviceCount: number;
    depth: number;
    spanCount: number;
  }

  interface ITraceSpan {
    id: string;
    traceId: string;
    operationName: string;
    startTime: number;
    endTime: number;
    parentSpanId: string;
    timestamp: number;
    duration: number;
    selfDuration: number;
    tags: ITag;
  }

  interface ITag {
    application_id: string;
    application_name: string;
    cluster_name: string;
    component: string;
    host: string;
    host_ip: string;
    http_method: string;
    http_path: string;
    http_status_code: string;
    http_url: string;
    instance_id: string;
    operation_name: string;
    org_id: string;
    org_name: string;
    parent_span_id: string;
    peer_hostname: string;
    project_id: string;
    project_name: string;
    runtime_id: string;
    runtime_name: string;
    service_id: string;
    service_instance_id: string;
    service_ip: string;
    service_name: string;
    'source-addon-id': string;
    'source-addon-type': string;
    span_host: string;
    span_id: string;
    span_kind: string;
    span_layer: string;
    terminus_app: string;
    terminus_key: string;
    terminus_logid: string;
    trace_id: string;
    workspace: string;
    error?: string;
  }

  interface ISpanChartData {
    dashboardId: string;
    conditions: string[];
  }

  interface ISpanRelationChart {
    callAnalysis: ISpanChartData;
    serviceAnalysis: ISpanChartData;
  }

  interface ISpanItem {
    id: string;
    traceId: string;
    operationName: string;
    startTime: number;
    endTime: number;
    parentSpanId: string;
    timestamp: number;
    duration: number;
    selfDuration: number;
    tags: ITag;
    children?: ISpanItem[];
    depth?: number;
    key?: string;
    title?: React.ReactNode;
  }

  interface ITraceDetail {
    depth: number;
    duration: string;
    serviceCounts: Array<{ name: string; count: number; max: number }>;
    services: number;
    spans: ISpan[];
    spansBackup: ISpan[];
    timeMarkers: Array<{ index: number; time: string }>;
    timeMarkersBackup: Array<{ index: number; time: string }>;
    totalSpans: number;
    traceId: string;
  }

  interface IQuerySpan {
    traceId: string;
    scopeId: string;
    limit?: number;
    startTime?: number;
  }

  interface ISpan extends ITraceSpan {
    depth: number;
    depthClass: number;
    duration: number;
    durationStr: string;
    errorType: string;
    isExpand: boolean;
    isShow: boolean;
    left: number;
    parentId: string;
    serviceName: string;
    serviceNames: string;
    spanId: string;
    spanName: string;
    width: number;
  }

  interface SpanEvent {
    spanEvents: Array<{ timestamp: number; events: object }>;
  }

  interface FlameChartData {
    name: string;
    value: number;
    children: FlameChartData[];
    serviceName: string;
    selfDuration: number;
    spanKind: string;
    component: string;
  }

  type IFixedConditionType = 'sort' | 'limit' | 'traceStatus';

  type IFixedCondition = {
    [k in IFixedConditionType]: {
      key: string;
      value: string;
      displayName: string;
    }[];
  };

  interface TraceConditions extends IFixedCondition {
    others: {
      key: string;
      value: string;
      displayName: string;
      type: string;
      paramKey: string;
    }[];
  }
}
