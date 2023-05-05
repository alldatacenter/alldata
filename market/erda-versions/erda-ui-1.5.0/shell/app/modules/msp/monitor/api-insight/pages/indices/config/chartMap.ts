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
import ConnectChart from 'api-insight/common/components/connectChart';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'APIIndices',
  groupId: 'apiIndices',
};

const chartMap = merge(
  {
    memory: {
      ...commonAttr,
      titleText: i18n.t('memory'),
      chartName: 'memory',
      viewRender: ConnectChart,
    },
    cpu: {
      ...commonAttr,
      titleText: 'CPU',
      chartName: 'cpu',
      viewRender: ConnectChart,
    },
    disk: {
      ...commonAttr,
      titleText: i18n.t('disk'),
      chartName: 'disk',
      viewRender: ConnectChart,
    },
    network: {
      ...commonAttr,
      titleText: i18n.t('network'),
      chartName: 'network',
      viewRender: ConnectChart,
    },
  },
  ApiMap,
);

export default {
  memory: chartRender(chartMap.memory) as any,
  cpu: chartRender(chartMap.cpu) as any,
  disk: chartRender(chartMap.disk) as any,
  network: chartRender(chartMap.network) as any,
};
