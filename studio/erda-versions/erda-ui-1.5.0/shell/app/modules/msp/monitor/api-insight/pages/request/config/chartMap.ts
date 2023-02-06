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
import { topPVPanel } from 'api-insight/common/components/topPVPanel';
import ConnectChart from 'api-insight/common/components/connectChart';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'APIRequest',
  groupId: 'apiRequest',
};

const chartMap = merge(
  {
    qps: {
      ...commonAttr,
      titleText: 'QPS',
      chartName: 'qps',
    },
    pv: {
      ...commonAttr,
      titleText: 'PV TOP10',
      chartName: 'pv',
      viewRender: topPVPanel,
    },
    connect: {
      ...commonAttr,
      titleText: i18n.t('msp:connection status'),
      chartName: 'connect',
      viewRender: ConnectChart,
    },
  },
  ApiMap,
);

export default {
  qps: chartRender(chartMap.qps) as any,
  pv: chartRender(chartMap.pv) as any,
  connect: chartRender(chartMap.connect) as any,
};
