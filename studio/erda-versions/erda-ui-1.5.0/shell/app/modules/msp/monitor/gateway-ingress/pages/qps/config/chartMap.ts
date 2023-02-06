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

import { merge } from 'lodash';
import { chartRender } from 'gateway-ingress/common/components/giRenderFactory';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'gatewayIngressQPS',
  groupId: 'gatewayIngressQPS',
};
const chartMap = merge(
  {
    overallQPS: {
      titleText: i18n.t('msp:overall QPS'),
      ...commonAttr,
      chartName: 'overallQPS',
    },
    successQPS: {
      titleText: i18n.t('msp:successful QPS'),
      ...commonAttr,
      chartName: 'successQPS',
    },
    QPS4xx: {
      titleText: '4xx QPS',
      ...commonAttr,
      chartName: 'QPS4xx',
    },
    QPS5xx: {
      titleText: '5xx QPS',
      ...commonAttr,
      chartName: 'QPS5xx',
    },
  },
  ApiMap,
);

export default {
  overallQPS: chartRender(chartMap.overallQPS) as any,
  successQPS: chartRender(chartMap.successQPS) as any,
  QPS4xx: chartRender(chartMap.QPS4xx) as any,
  QPS5xx: chartRender(chartMap.QPS5xx) as any,
};
