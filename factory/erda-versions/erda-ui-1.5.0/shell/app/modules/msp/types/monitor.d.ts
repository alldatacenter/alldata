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

declare namespace MS_MONITOR {
  interface ICreateChartProps {
    moduleName: string;
    chartName: string;
    dataHandler?: Function;
  }

  interface ITraceCountQuery {
    cardinality: string;
    align: boolean;
    start?: number;
    end?: number;
    'filter_fields.terminus_keys': string;
    'filter_fields.applications_ids'?: number;
    'filter_fields.services_distinct'?: string;
    field_gt_errors_sum?: number;
    field_eq_errors_sum?: number;
    points?: number;
  }

  interface ITraceSummaryQuery {
    tenantId: string;
    status: string;
    startTime: number;
    endTime: number;
    limit: string;
    traceId?: string;
    sort: string;
    serviceName?: string;
    durationMin?: number;
    durationMax?: number;
    httpPath?: string;
  }

  interface ITraceSummary {
    id: string;
    duration: number;
    elapsed: number;
    services: string[];
    startTime: number;
  }
}
