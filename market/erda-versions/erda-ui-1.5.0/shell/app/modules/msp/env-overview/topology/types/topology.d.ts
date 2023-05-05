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

declare namespace TOPOLOGY {
  type INodeType =
    | 'Service'
    | 'Mysql'
    | 'Redis'
    | 'RocketMQ'
    | 'ExternalService'
    | 'InternalService'
    | 'Dubbo'
    | 'SideCar'
    | 'ApiGateway'
    | 'RegisterCenter'
    | 'ConfigCenter'
    | 'NoticeCenter'
    | 'Elasticsearch';

  interface ITopologyQuery {
    startTime: number;
    endTime: number;
    terminusKey: string;
    tags: string[];
  }

  interface IParent {
    id: string;
    name: string;
    metric?: {
      count: number;
      lineCount: number;
      mqCount: boolean;
      rt: number;
    };
  }

  interface IGroup {
    category: string;
    id: string;
    name: string;
    parents: IParent[];
  }

  interface TopoNode {
    label: string;
    isRoot: boolean;
    isParent: boolean;
    isLeaf: boolean;
    hoverStatus: number;
    selectStatus: number;
    childrenCount: number;
    parentCount: number;
    isUnhealthy: boolean;
    isCircular: boolean;
    isAddon: boolean;
    isService: boolean;
    metaData: Omit<INode, 'parents'>;
  }

  interface TopoEdge {
    isCircular: boolean;
    isUnhealthy: boolean;
    isAddon: boolean;
    isService: boolean;
    hoverStatus: number;
    selectStatus: number;
    source: Omit<INode, 'parents'>;
    target: Omit<INode, 'parents'>;
  }

  interface INode {
    applicationId: string;
    applicationName: string;
    group: string;
    id: string;
    dashboardId: string;
    metric: {
      rps: number;
      rt: number;
      count: number;
      http_error: number;
      error_rate: number;
      running: number;
      stopped: number;
    };
    name: string;
    parents: INode[];
    runtimeId: string;
    runtimeName: string;
    serviceMesh: string;
    serviceName: string;
    serviceId: string;
    typeDisplay: string;
    type: INodeType;
  }

  interface ITopologyResp {
    nodes: INode[];
  }

  interface IErrorDetailQuery {
    align: boolean;
    end: number;
    field_gte_http_status_code_min: number;
    filter_error: boolean;
    filter_target_application_id: string;
    filter_target_runtime_name: string;
    filter_target_service_name: string;
    filter_target_terminus_key: string;
    start: number;
    sum: string;
  }

  interface IExceptionQuery {
    sum: string;
    align: boolean;
    start: number;
    end: number;
    filter_terminus_key: string;
    filter_runtime_name: string;
    filter_service_name: string;
    filter_application_id: number;
  }

  interface ILink {
    source: string;
    target: string;
    hasReverse?: boolean;
  }

  interface INodeParent {
    id: string;
  }

  interface IConfig {
    direction: string; // 画布方向: vertical | horizontal
    NODE: {
      width: number; // 节点宽
      height: number; // 节点高
      margin: {
        x: number; // 节点横向间距
        y: number; // 节点纵向间距
      };
    };
    LINK: {
      linkDis: number; // 跨层级线间距
    };
    padding: {
      // 单个独立图的padding
      x: number;
      y: number;
    };
    groupPadding: {
      x: number;
      y: number;
    };
    boxMargin: {
      x: number;
      y: number;
    };
    svgAttr: {
      polyline: object;
      polylineFade: object;
    };
    showBox: boolean; // 是否需要显示box
  }

  interface ILinkRender {
    links: ILink[];
    nodeMap: object;
    boxHeight?: number;
    groupDeepth?: any;
  }

  interface ICircuitBreakerBase {
    id: string;
    type: string;
    maxConnections: string; // 最大连接数
    maxRetries: string; // 最大重试次数
    consecutiveErrors: string; // 连续失败的次数
    interval: string; // 连续失败的检测间隔，单位秒
    baseEjectionTime: string; // 最短隔离时间，单位秒
    maxEjectionPercent: string; // 最大隔离比例
    enable: boolean; // 是否停用
  }

  interface ICircuitBreakerHttp extends ICircuitBreakerBase {
    maxPendingRequests: string; // 最大等待请求数
  }

  interface ICircuitBreakerDubbo extends ICircuitBreakerBase {
    interfaceName: string; // 接口名称
  }

  interface ICircuitBreaker {
    http: ICircuitBreakerHttp;
    dubbo: ICircuitBreakerDubbo[];
  }

  interface IFaultInjectHttp {
    id: string;
    type: string;
    path: string; // 路径前缀
    fixedDelay: string; // 延时时间
    delayPercentage: string; // 延时比例
    abortStatus: string; // 错误码
    abortPercentage: string; // 错误比例
    enable: boolean; // 是否启用，true为启用，false为停用
  }

  interface IFaultInjectDubbo {
    id: string;
    type: string;
    interfaceName: string; // 接口名称
    fixedDelay: string; // 延时时间
    delayPercentage: string; // 延时比例
    enable: boolean; // 是否启用，true为启用，false为停用
  }

  interface IFaultInject {
    http: IFaultInjectHttp[];
    dubbo: IFaultInjectDubbo[];
  }

  interface IQuery {
    projectId: string; // 项目id
    env: string; // 环境
    tenantGroup: string; // 租户组
  }

  interface IServiceMeshQuery extends IQuery {
    runtimeId: string;
    runtimeName: string;
    applicationId: string;
    serverName: string;
    hideNoRule: string;
  }

  interface ICircuitBreakerSave extends IQuery {
    data: ICircuitBreakerHttp | ICircuitBreakerDubbo;
    query: IServiceMeshQuery;
  }

  interface IFaultInjectSave extends IQuery {
    data: IFaultInjectHttp | IFaultInjectDubbo;
    query: IServiceMeshQuery;
  }

  interface IFaultInjectDelete extends IQuery, IServiceMeshQuery {
    id: string;
  }

  interface ITopologyTagsQuery {
    terminusKey: string;
  }

  interface ISingleTopologyTags {
    tag: string;
    label: string;
    type: string;
  }

  interface ITopologyTagOptionQuery {
    startTime: number;
    endTime: number;
    terminusKey: string;
    tag: string;
  }
}
