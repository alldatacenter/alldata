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

import serviceListRouter from 'msp/env-overview/service-list/router';
import i18n from 'i18n';
import getGatewayIngressMonitorRouter from 'gateway-ingress/router';
import getEIRouter from 'external-insight/router';

const tabs = [
  {
    key: 'service-list',
    name: i18n.t('msp:service list'),
  },
  {
    key: 'topology',
    name: i18n.t('msp:topology'),
  },
];
const getEnvOverViewRouter = (): RouteConfigItem => {
  return {
    path: ':terminusKey',
    tabs,
    pageName: i18n.t('msp:service overview'),
    breadcrumbName: i18n.t('msp:service overview'),
    alwaysShowTabKey: 'service-list',
    routes: [
      {
        path: 'topology',
        tabs,
        alwaysShowTabKey: 'topology',
        routes: [
          {
            layout: { fullHeight: true, noWrapper: true },
            getComp: (cb) => cb(import('msp/env-overview/topology/pages/topology')),
          },
          getGatewayIngressMonitorRouter(),
          getEIRouter(),
        ],
      },
      serviceListRouter(tabs),
      {
        getComp: (cb: RouterGetComp) => cb(import('msp/env-overview/service-list/pages')),
        layout: {
          fullHeight: true,
          noWrapper: true,
        },
      },
    ],
  };
};

export default getEnvOverViewRouter;
