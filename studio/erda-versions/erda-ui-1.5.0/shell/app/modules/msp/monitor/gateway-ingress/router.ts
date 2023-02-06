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
  { key: 'gateway-ingress', name: i18n.t('msp:visit times') },
  { key: 'gateway-ingress/connection', name: i18n.t('msp:number of connection') },
  { key: 'gateway-ingress/traffic', name: i18n.t('msp:transmission traffic') },
  { key: 'gateway-ingress/latency', name: i18n.t('msp:latency') },
];

const getGatewayIngressMonitorRouter = (): RouteConfigItem => ({
  path: 'gateway-ingress',
  tabs,
  pageName: i18n.t('msp:ingress traffic monitoring'),
  routes: [
    {
      getComp: (cb) => cb(import('msp/monitor/gateway-ingress/pages/qps/qps')),
    },
    {
      path: 'connection',
      alwaysShowTabKey: 'gateway-ingress/connection',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/gateway-ingress/pages/connection/connection')),
    },
    {
      path: 'traffic',
      alwaysShowTabKey: 'gateway-ingress/traffic',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/gateway-ingress/pages/traffic/traffic')),
    },
    {
      path: 'latency',
      alwaysShowTabKey: 'gateway-ingress/latency',
      tabs,
      getComp: (cb) => cb(import('msp/monitor/gateway-ingress/pages/latency/latency')),
    },
  ],
});

export default getGatewayIngressMonitorRouter;
