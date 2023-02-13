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

declare namespace MSP_SERVICES {
  type SERVICE_LIST_CHART_TYPE = 'RPS' | 'AvgDuration' | 'ErrorRate';

  type ServiceStatus = 'hasError' | 'withoutRequest';

  interface SERVICE_LIST_CHART {
    serviceId: string;
    views: {
      type: SERVICE_LIST_CHART_TYPE;
      maxValue: number;
      view: {
        timestamp: number;
        value: number;
      }[];
    }[];
  }

  interface SERVICE_LIST_ITEM {
    id: string;
    language: string;
    lastHeartbeat: string;
    name: string;
    aggregateMetric: {
      avgRps: number;
      maxRps: number;
      avgDuration: number;
      maxDuration: number;
      errorRate: number;
    };
  }

  type SERVICE_LIST = IPagingResp<SERVICE_LIST_ITEM> & IPaging;


  interface ServiceCount {
    totalCount: number;
    hasErrorCount: number;
    withoutRequestCount: number
  }
}
