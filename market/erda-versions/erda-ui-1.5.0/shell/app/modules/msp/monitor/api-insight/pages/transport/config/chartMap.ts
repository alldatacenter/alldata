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
import { chartRender } from 'api-insight/common/components/apiRenderFactory';
import { apiSizePanel } from 'api-insight/common/components/apiSizePanel';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'APITransport',
  groupId: 'apiTransport',
};

const chartMap = merge(
  {
    requestSize: {
      ...commonAttr,
      titleText: i18n.t('msp:request size'),
      chartName: 'requestSize',
    },
    requestSizeTop: {
      ...commonAttr,
      titleText: i18n.t('msp:request size TOP 10'),
      chartName: 'requestSizeTop',
      viewRender: apiSizePanel,
    },
    responseSize: {
      ...commonAttr,
      titleText: i18n.t('msp:response size'),
      chartName: 'responseSize',
    },
    responseSizeTop: {
      ...commonAttr,
      titleText: i18n.t('msp:response size TOP 10'),
      chartName: 'responseSizeTop',
      viewRender: apiSizePanel,
    },
  },
  ApiMap,
);

export default {
  requestSize: chartRender(chartMap.requestSize) as any,
  requestSizeTop: chartRender(chartMap.requestSizeTop) as any,
  responseSize: chartRender(chartMap.responseSize) as any,
  responseSizeTop: chartRender(chartMap.responseSizeTop) as any,
};
