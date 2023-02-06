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
import { apiDelayPanel } from 'api-insight/common/components/apiDelayPanel';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'APIDelay',
  groupId: 'apiDelay',
};

const chartMap = merge(
  {
    requestDelay: {
      ...commonAttr,
      titleText: i18n.t('msp:request delay'),
      chartName: 'requestDelay',
      viewProps: {
        unitType: 'TIME',
      },
    },
    requestDelayTop: {
      ...commonAttr,
      titleText: i18n.t('msp:request delay TOP 10'),
      chartName: 'requestDelayTop',
      viewRender: apiDelayPanel,
    },
    backendDelay: {
      ...commonAttr,
      titleText: i18n.t('msp:backend delay'),
      chartName: 'backendDelay',
    },
    backendDelayTop: {
      ...commonAttr,
      titleText: i18n.t('msp:backend delay TOP 10'),
      chartName: 'backendDelayTop',
      viewRender: apiDelayPanel,
    },
  },
  ApiMap,
);

export default {
  requestDelay: chartRender(chartMap.requestDelay) as any,
  requestDelayTop: chartRender(chartMap.requestDelayTop) as any,
  backendDelay: chartRender(chartMap.backendDelay) as any,
  backendDelayTop: chartRender(chartMap.backendDelayTop) as any,
};
