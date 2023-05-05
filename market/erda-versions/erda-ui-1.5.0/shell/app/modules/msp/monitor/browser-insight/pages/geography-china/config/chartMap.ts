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

import { merge, floor } from 'lodash';
import ChinaMap from 'app/files/china.json';
import { sortCreator } from 'browser-insight/common/utils';
import { sortRender, chartRender } from 'browser-insight/common/components/biRenderFactory';
import SlowTrack from '../slow-track';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

const commonAttr = {
  moduleName: 'BIGeography',
  groupId: 'BIGeography',
};
const chartMap = merge(
  {
    sortTab: sortCreator(commonAttr.moduleName, 'sortTab'),
    sortList: {
      ...commonAttr,
      type: 'sortList',
      chartName: 'sortList',
    },
    performanceInterval: {
      ...commonAttr,
      titleText: i18n.t('performance interval'),
      chartName: 'performanceInterval',
    },
    pagePerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:performance trend'),
      chartName: 'performanceTrends',
    },
    regionalLoadingTime: {
      titleText: i18n.t('msp:regional average load time'),
      ...commonAttr,
      chartName: 'geography-china',
      viewType: 'map',
      viewProps: {
        mapData: ChinaMap,
        formatter(params: any) {
          const data = params.data || {};
          if (!data) return `${data.name}`;
          if (!data.name) return null;
          const tps = data.tps ? `${floor(data.tps, 3)} cpm` : i18n.t('msp:no data');
          const time = data.value ? `${floor(data.value, 3)} s` : i18n.t('msp:no data');
          return `${data.name} <br /> 吞吐量: ${tps} <br /> ${i18n.t('msp:average load time')}: ${time}`;
        },
      },
    },
    slowTrack: {
      titleText: i18n.t('msp:slow loading tracking'),
      ...commonAttr,
      chartName: 'slow',
      viewRender: SlowTrack,
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  regionalLoadingTime: chartRender(chartMap.regionalLoadingTime) as any,
  performanceInterval: chartRender(chartMap.performanceInterval) as any,
  pagePerformanceTrends: chartRender(chartMap.pagePerformanceTrends) as any,
  slowTrack: chartRender(chartMap.slowTrack) as any,
};
