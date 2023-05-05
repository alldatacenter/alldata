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
import { sortRender, chartRender } from 'browser-insight/common/components/biRenderFactory';
import ScriptDetail from '../script-detail';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

const commonAttr = {
  moduleName: 'BIScript',
  groupId: 'BIScript',
};

const chartMap = merge(
  {
    sortTab: {
      moduleName: 'BIScript',
      type: 'sortTab',
      tabList: [
        { name: i18n.t('msp:error message'), key: 'error' },
        { name: i18n.t('msp:error page'), key: 'url' },
      ],
    },
    sortList: {
      moduleName: 'BIScript',
      type: 'sortList',
      chartName: 'BIScriptSort',
      viewProps: {
        onClickItem: null,
      },
    },
    errorTopN: {
      titleText: i18n.t('msp:JavaScript error times'),
      ...commonAttr,
      chartName: 'errorTopN',
    },
    browsersTopN: {
      titleText: i18n.t('msp:browser error times'),
      ...commonAttr,
      chartName: 'browsersTopN',
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
  browsersTopN: chartRender(chartMap.browsersTopN) as any,
  scriptDetail: chartRender(chartMap.scriptDetail) as any,
};
