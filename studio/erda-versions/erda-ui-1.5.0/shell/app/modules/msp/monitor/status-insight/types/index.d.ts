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

declare namespace MONITOR_STATUS {
  interface IDashboardQuery {
    projectId: string;
    env: string;
    tenantId: string;
  }

  interface IChart {
    latency: number[];
    status: string[];
    time: number[];
  }

  interface IDashboard {
    accountId: number;
    apdex: number;
    avg: number;
    chart: IChart;
    downDuration: string;
    downtime: string;
    expects: string;
    extra: string;
    lastUpdate: string;
    latency: number;
    max: number;
    min: number;
    mode: string;
    name: string;
    pause: number;
    requestId: string;
    serviceId: string;
    serviceName: string;
    status: string;
    uptime: string;
    url: string;
    config: Obj;
  }

  interface IDashboardResp {
    downCount?: number;
    metrics?: Obj<IDashboard>;
  }

  interface IDashboardDetailQuery {
    id: string;
    period?: string;
  }

  interface IMetrics {
    status?: string[];
    time?: number[];
  }

  interface ICreateMetricsBody {
    data: IMetricsBody;
  }

  interface IMetricsBody {
    env: string;
    chart: IChart;
    mode: string;
    name: string;
    projectId: string;
    id: string;
    tenantId: string;
    config: Obj;
  }

  interface IPastIncidents {
    createAt?: string;
    durationFormat?: string;
    requestId?: string;
    lastUpdate?: string;
  }
}
