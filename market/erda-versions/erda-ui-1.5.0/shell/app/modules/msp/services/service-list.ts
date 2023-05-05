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

import { apiCreator } from 'core/service';

interface ServicesParams {
  tenantId: string;
  serviceName?: string;
  pageNo: number;
  pageSize: number;
  serviceStatus?: MSP_SERVICES.ServiceStatus;
}

interface AnalyzerOverviewParams {
  tenantId: string;
  serviceIds: string[];
  view?: string;
  startTime?: number;
  endTime?: number;
}

const apis = {
  getServices: {
    api: 'get@/api/msp/apm/services',
  },
  getAnalyzerOverview: {
    api: 'get@/api/msp/apm/service/analyzer-overview',
  },
  getServiceCount: {
    api: 'get@/api/msp/apm/service/count',
  },
};

export const getServices = apiCreator<(payload: ServicesParams) => MSP_SERVICES.SERVICE_LIST>(apis.getServices);
export const getServiceCount = apiCreator<(payload: { tenantId: string }) => MSP_SERVICES.ServiceCount>(
  apis.getServiceCount,
);
export const getAnalyzerOverview = apiCreator<
  (payload: AnalyzerOverviewParams) => { list: MSP_SERVICES.SERVICE_LIST_CHART[] }
>(apis.getAnalyzerOverview);
