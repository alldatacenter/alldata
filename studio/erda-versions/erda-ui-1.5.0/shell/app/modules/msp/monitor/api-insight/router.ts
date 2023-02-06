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

import i18n from 'i18n';

const tabs = [
  { key: 'api-monitor', name: i18n.t('msp:request analysis') },
  { key: 'api-monitor/delay', name: i18n.t('msp:delay analysis') },
  { key: 'api-monitor/transport', name: i18n.t('msp:transmission analysis') },
  // { key: 'api-monitor/indices', name: i18n.t('msp:gateway load') },
];

// API 分析页
const getApiInsightRouter = (): RouteConfigItem => ({
  path: 'api-monitor',
  mark: 'api-monitor',
  pageName: i18n.t('msp:API analysis'),
  tabs,
  routes: [
    {
      getComp: (cb) => cb(import('msp/monitor/api-insight/pages/request')),
    },
    {
      path: 'delay',
      alwaysShowTabKey: 'api-monitor/delay',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/api-insight/pages/delay')),
    },
    {
      path: 'transport',
      alwaysShowTabKey: 'api-monitor/transport',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/api-insight/pages/transport')),
    },
    {
      path: 'indices',
      alwaysShowTabKey: 'api-monitor/indices',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/api-insight/pages/indices')),
    },
  ],
});

export default getApiInsightRouter;
