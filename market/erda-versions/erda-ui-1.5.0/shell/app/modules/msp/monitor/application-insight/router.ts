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
  { key: 'ai', name: i18n.t('overview') },
  { key: 'ai/web', name: i18n.t('msp:Web transaction') },
  { key: 'ai/rpc', name: i18n.t('msp:RPC transaction') },
  { key: 'ai/db', name: i18n.t('database') },
  { key: 'ai/cache', name: i18n.t('cache') },
  { key: 'ai/jvm', name: 'JVMs' },
  { key: 'ai/nodes', name: 'NodeJs' },
];

const getAIRouter = (): RouteConfigItem => ({
  path: 'ai',
  // breadcrumbName: '应用性能',
  tabs,
  pageName: i18n.t('application performance'),
  routes: [
    {
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/overview/overview')),
    },
    {
      path: 'web',
      // breadcrumbName: 'web事务',
      alwaysShowTabKey: 'ai/web',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/web/web')),
    },
    {
      path: 'rpc',
      // breadcrumbName: 'web事务',
      alwaysShowTabKey: 'ai/rpc',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/rpc/rpc')),
    },
    {
      path: 'db',
      // breadcrumbName: '数据库',
      alwaysShowTabKey: 'ai/db',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/database/database')),
    },
    {
      path: 'cache',
      // breadcrumbName: '缓存',
      alwaysShowTabKey: 'ai/cache',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/cache/cache')),
    },
    {
      path: 'jvm',
      // breadcrumbName: 'JVMs',
      alwaysShowTabKey: 'ai/jvm',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/jvms/jvms')),
    },
    {
      path: 'nodes',
      // breadcrumbName: 'Nodes',
      alwaysShowTabKey: 'ai/nodes',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/application-insight/pages/nodes/nodes')),
    },
  ],
});

export default getAIRouter;
