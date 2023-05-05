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
  moduleName: 'gatewayIngressTraffic',
  groupId: 'gatewayIngressTraffic',
};
const chartMap = merge(
  {
    request: {
      titleText: i18n.t('msp:request traffic'),
      ...commonAttr,
      chartName: 'request',
    },
    response: {
      titleText: i18n.t('msp:response traffic'),
      ...commonAttr,
      chartName: 'response',
    },
  },
  ApiMap,
);

export default {
  request: chartRender(chartMap.request) as any,
  response: chartRender(chartMap.response) as any,
};
