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

import { TimeSelectWithStore } from 'msp/components/time-select';
import i18n, { isZh } from 'i18n';

const tabs = [
  { key: 'bi', name: i18n.t('overview') },
  { key: 'bi/domain', name: i18n.t('msp:access domain') },
  { key: 'bi/page', name: i18n.t('msp:access page') },
  { key: 'bi/position', name: i18n.t('msp:location analysis') },
  // { key: 'exception', name: i18n.t('msp:access error') },
  { key: 'bi/ajax', name: i18n.t('msp:ajax interface') },
  { key: 'bi/script', name: i18n.t('msp:script error') },
  { key: 'bi/browser', name: i18n.t('msp:browser performance') },
  { key: 'bi/summary', name: i18n.t('msp:summary') },
];

if (isZh()) {
  tabs.push({ key: 'bi/geography', name: i18n.t('msp:geography') });
}

const getBIRouter = (): RouteConfigItem => ({
  path: 'bi',
  breadcrumbName: i18n.t('msp:front-end monitor'),
  pageName: i18n.t('msp:front-end monitor'),
  tabs,
  routes: [
    {
      // breadcrumbName: i18n.t('msp:browse performance'),
      getComp: (cb) => cb(import('browser-insight/pages/overview/overview')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'page',
      // breadcrumbName: '访问页面',
      alwaysShowTabKey: 'bi/page',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/page/page')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'domain',
      // breadcrumbName: '访问域名',
      alwaysShowTabKey: 'bi/domain',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/domain/domain')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'ajax',
      // breadcrumbName: 'Ajax接口',
      alwaysShowTabKey: 'bi/ajax',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/ajax/ajax')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'script',
      // breadcrumbName: '脚本错误',
      alwaysShowTabKey: 'bi/script',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/script/script')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'browser',
      // breadcrumbName: '浏览器性能',
      alwaysShowTabKey: 'bi/browser',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/browser/browser')),
      layout: {
        noWrapper: true,
      },
    },
    {
      path: 'exception',
      breadcrumbName: i18n.t('msp:access error'),
      alwaysShowTabKey: 'bi/exception',
      tabs,
      TabRightComp: TimeSelectWithStore,
      getComp: (cb) => cb(import('browser-insight/pages/exception/exception')),
    },
    {
      path: 'position',
      // breadcrumbName: '定位分析',
      tabs,
      alwaysShowTabKey: 'bi/position',
      routes: [
        {
          getComp: (cb) => cb(import('browser-insight/pages/position/position')),
          layout: {
            noWrapper: true,
          },
        },
        {
          path: 'comparative',
          breadcrumbName: i18n.t('msp:comparative analysis'),
          getComp: (cb) => cb(import('browser-insight/pages/comparative/comparative')),
          layout: {
            noWrapper: true,
          },
        },
      ],
    },
    {
      path: 'summary',
      // breadcrumbName: '摘要',
      alwaysShowTabKey: 'bi/summary',
      tabs,
      getComp: (cb) => cb(import('browser-insight/pages/summary/summary')),
      layout: {
        noWrapper: true,
      },
    },
    ...(isZh()
      ? [
          {
            path: 'geography',
            // breadcrumbName: '地理',
            alwaysShowTabKey: 'bi/geography',
            tabs,
            getComp: (cb) => cb(import('browser-insight/pages/geography-china/geography-china')),
            layout: {
              noWrapper: true,
            },
          } as RouteConfigItem,
        ]
      : []),
  ],
});

export default getBIRouter;
