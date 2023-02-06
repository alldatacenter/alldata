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

import i18n, { isZh } from 'i18n';

const tabs = [
  { key: 'mi', name: i18n.t('overview') },
  { key: 'mi/appversion', name: i18n.t('msp:application version') },
  { key: 'mi/page', name: i18n.t('msp:access page') },
  { key: 'mi/position', name: i18n.t('msp:location analysis') },
  { key: 'mi/request', name: i18n.t('msp:request') },
  { key: 'mi/script', name: i18n.t('error') },
  { key: 'mi/device', name: i18n.t('msp:device') },
];

if (isZh()) {
  tabs.push({ key: 'mi/geography', name: i18n.t('msp:geography') });
}

const getMIRouter = (): RouteConfigItem => ({
  path: 'mi',
  tabs,
  pageName: i18n.t('msp:app performance'),
  routes: [
    {
      getComp: (cb) => cb(import('mobile-insight/pages/overview/overview')),
    },
    {
      path: 'page',
      alwaysShowTabKey: 'mi/page',
      tabs,
      // breadcrumbName: 'Mobile Insight (访问页面)',
      getComp: (cb) => cb(import('mobile-insight/pages/page/page')),
    },
    {
      path: 'position',
      tabs,
      alwaysShowTabKey: 'mi/position',
      // breadcrumbName: 'Mobile Insight (定位分析)',
      routes: [
        {
          getComp: (cb) => cb(import('mobile-insight/pages/position/position')),
        },
        {
          path: 'comparative',
          breadcrumbName: i18n.t('msp:comparative analysis'),
          getComp: (cb) => cb(import('mobile-insight/pages/comparative/comparative')),
        },
      ],
    },
    {
      path: 'request',
      alwaysShowTabKey: 'mi/request',
      tabs,
      // breadcrumbName: 'Mobile Insight (请求)',
      getComp: (cb) => cb(import('mobile-insight/pages/ajax/ajax')),
    },
    {
      path: 'script',
      alwaysShowTabKey: 'mi/script',
      tabs,
      // breadcrumbName: 'Mobile Insight (页面错误)',
      getComp: (cb) => cb(import('mobile-insight/pages/script/script')),
    },
    {
      path: 'appversion',
      alwaysShowTabKey: 'mi/appversion',
      tabs,
      // breadcrumbName: 'Mobile Insight (应用版本)',
      getComp: (cb) => cb(import('mobile-insight/pages/app/app')),
    },
    ...(isZh()
      ? [
          {
            path: 'geography',
            alwaysShowTabKey: 'mi/geography',
            tabs,
            // breadcrumbName: 'Mobile Insight (地理)',
            getComp: (cb) => cb(import('mobile-insight/pages/geography/geography-china')),
          } as RouteConfigItem,
        ]
      : []),
    {
      path: 'device',
      alwaysShowTabKey: 'mi/device',
      tabs,
      // breadcrumbName: 'Mobile Insight (设备)',
      getComp: (cb) => cb(import('mobile-insight/pages/browser/browser')),
    },
  ],
});

export default getMIRouter;
