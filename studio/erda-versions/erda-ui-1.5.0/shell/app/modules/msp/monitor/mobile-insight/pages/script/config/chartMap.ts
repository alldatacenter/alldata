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
import { sortRender, chartRender } from 'mobile-insight/common/components/miRenderFactory';
import ScriptDetail from '../script-detail';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

const commonAttr = {
  moduleName: 'MIScript',
  groupId: 'MIScript',
};

const chartMap = merge(
  {
    sortTab: {
      moduleName: 'MIScript',
      type: 'sortTab',
      tabList: [{ name: i18n.t('msp:error message'), key: 'error' }],
    },
    sortList: {
      moduleName: 'MIScript',
      type: 'sortList',
      chartName: 'MIScriptSort',
      viewProps: {
        onClickItem: null,
      },
    },
    errorTopN: {
      titleText: `${i18n.t('msp:number of errors')} TOP5`,
      ...commonAttr,
      chartName: 'errorTopN',
    },
    osTopN: {
      titleText: i18n.t('msp:operating system'),
      ...commonAttr,
      chartName: 'osTopN',
    },
    scriptDetail: {
      titleText: i18n.t('msp:error details'),
      ...commonAttr,
      chartName: 'scriptDetail',
      viewRender: ScriptDetail,
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  errorTopN: chartRender(chartMap.errorTopN) as any,
  osTopN: chartRender(chartMap.osTopN) as any,
  scriptDetail: chartRender(chartMap.scriptDetail) as any,
};
