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

declare namespace DCOS_SERVICES {
  type MetricsType = 'cpu' | 'mem';
  type ServicesType = 'project' | 'application' | 'runtime' | 'service';

  interface metric {
    loading: boolean;
    data: any[];
  }

  interface IState {
    containerList: Service[];
    serviceList: InstancesUsage[];
    runtimeJson: null;
    runtimeStatus: {};
    serviceReqStatus: boolean;
    metrics: {
      cpu: metric;
      mem: metric;
    };
  }

  interface QueryServices {
    type: string;
    project: string;
    app: string;
    runtimeID: string;
    serviceName: string;
    status: string;
  }

  interface Service {
    containerId: string;
    ipAddress: string;
    host: string;
    image: string;
    cpu: number;
    memory: number;
    disk: number;
    status: string;
    exitCode: number;
    message: string;
    startedAt: string;
    service: string;
    clusterName: string;
  }

  interface QueryServiceList {
    type: ServicesType;
    environment: string;
    ip?: string;
    project?: string;
    application?: string;
    runtime?: string;
  }

  interface path {
    q: string;
    name: string;
  }

  interface InstancesUsage {
    id: string;
    name: string;
    instance: number;
    unhealthy: number;
    memory: number;
    disk: number;
    cpu: number;
  }

  interface QueryMetrics {
    type: MetricsType;
    paths: path[];
    filter_workspace: string;
  }

  interface Bulk {
    [k: string]: {
      namespace: string;
      name: string;
      status: string;
      more: {
        'apm-demo-api': string;
        'apm-demo-ui': string;
      };
    };
  }
}
