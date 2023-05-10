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

declare namespace COMMON_CUSTOM_ALARM {
  interface CustomAlarms {
    id: number;
    name: string;
    metric: string;
    window: number; // 周期，单位：min
    notifyTargets: string;
    dashboardId?: string;
    enable: boolean;
  }
  interface Metrics {
    name: CommonKey;
    fields: Array<{
      field: CommonKey;
      alias: string;
      dataType: string;
    }>;
    tags: Array<{
      tag: CommonKey;
      dataType: string;
    }>;
    fieldMap: any;
    tagMap: any;
  }
  interface Operators {
    key: string;
    display: string;
    type: string;
  }
  interface CommonKey {
    key: string;
    display: string;
  }
  type AlarmTarget = CommonKey;
  interface CustomMetricMap {
    metricMap: { [name: string]: Metrics };
    functionOperatorMap: { [name: string]: Operators };
    filterOperatorMap: { [name: string]: Operators };
    aggregator: CommonKey[];
    notifySample: string;
  }
  interface CustomMetrics {
    metrics: Metrics[];
    functionOperators: Operators[];
    filterOperators: Operators[];
    aggregator: CommonKey[];
    notifySample: string;
  }
  interface CustomAlarmNotify {
    targets: string[];
    title: string;
    content: string;
  }
  interface CustomAlarmRule {
    window: number;
    metric: string;
    group?: string[];
    filters?: Filter[];
    functions: Field[];
  }
  interface CustomAlarmDetail {
    name: string;
    rules: CustomAlarmRule[];
    notifies: CustomAlarmNotify[];
  }
  interface CustomAlarmQuery extends CustomAlarmDetail {
    scope: string;
    id?: number;
    tenantGroup?: string;
  }
  interface Filter {
    tag: string;
    operator: string;
    value: any;
    uniKey?: string;
  }
  interface Field {
    field: string;
    aggregator: string;
    operator: string;
    value: any;
    uniKey?: string;
    aggregations?: any[];
    aggregatorType?: string;
  }
  interface IPageParam {
    pageNo?: number;
    pageSize?: number;
    tenantGroup?: string;
  }
}
