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

declare namespace METRICS {
  interface IState {
    listMetric: Record<string, any>;
    metricItem: Record<string, any>;
  }

  interface chartQuery {
    fetchMetricKey: string;
    start: number;
    end: number;
    sum: string;
    points: number;
    filter_dpid: string;
    filter_denv: string;
    group: string;
    filter_csmr: string;
    align: boolean;
    projectId: string;
    sumCps: string[];
    filter_cluster_name: string;
  }

  interface GetGateway {
    type: string;
    resourceType: string;
    resourceId: string;
    chartQuery: chartQuery;
  }

  interface GatewayData {
    id: string;
    data: {
      results: Array<{
        name: string;
        data: any[];
      }>;
      time: string;
      title: string;
      total: number;
    };
  }

  interface LoadMetricItemQuery {
    filter_dpid: string;
    filter_denv: string;
    start: string;
    end: string;
    filter_papi: string;
    fetchMetricKey: string;
    points: string;
    avg: string[];
    customAPIPrefix: string;
    filter_pmapi: string;
    group?: string;
  }

  interface LoadMetricItemQuerys {
    resourceType: string;
    resourceId: string;
    type: string;
    chartQuery: LoadMetricItemQuery;
  }

  interface MetricItem {
    total: number;
    title: string;
    time: number[];
    resuult: Array<{
      data: Array<{
        [k: string]: any;
      }>;
      name: string;
    }>;
  }

  interface ChartMetaQuerys {
    resourceType: string;
    query?: {
      [k: string]: any;
    };
  }

  interface Metric {
    name: string;
    data: Array<{
      [k: string]: any;
    }>;
  }

  interface ChartMeta {
    resourceType: string;
    metrics: {
      data: Metric[];
    };
  }

  interface SetMetricItem {
    query: chartQuery | LoadMetricItemQuery;
    resourceType: string;
    data: GatewayData | { id: string; data: MetricItem };
  }
}
