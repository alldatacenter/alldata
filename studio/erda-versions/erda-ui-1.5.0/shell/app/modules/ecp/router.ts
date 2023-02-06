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

const getEcpRouter = () => [
  {
    path: 'ecp',
    mark: 'ecp',
    toMark: 'orgIndex',
    routes: [
      {
        path: 'application',
        breadcrumbName: i18n.t('ecp:application'),
        routes: [
          {
            layout: { fullHeight: true },
            getComp: (cb: RouterGetComp) => cb(import('./pages/application')),
          },
          {
            path: ':id',
            breadcrumbName: ({ query }) => {
              return `${query?.appName}`;
            },
            routes: [
              {
                layout: { fullHeight: true },
                getComp: (cb: RouterGetComp) => cb(import('./pages/application/site-manage')),
              },
              {
                path: ':siteName',
                layout: { fullHeight: true },
                getComp: (cb: RouterGetComp) => cb(import('./pages/application/site-ip-manage')),
              },
            ],
          },
        ],
      },
      {
        path: 'resource',
        breadcrumbName: i18n.t('resource management'),
        routes: [
          {
            breadcrumbName: i18n.t('ecp:site management'),
            layout: { fullHeight: true },
            getComp: (cb: RouterGetComp) => cb(import('./pages/resource')),
          },
          {
            path: ':id',
            breadcrumbName: i18n.t('ecp:site management'),
            layout: { fullHeight: true },
            getComp: (cb: RouterGetComp) => cb(import('./pages/resource/machine-manage')),
          },
        ],
      },
      {
        path: 'setting',
        breadcrumbName: i18n.t('ecp:configuration'),
        routes: [
          {
            layout: { fullHeight: true },
            getComp: (cb: RouterGetComp) => cb(import('./pages/setting')),
          },
          {
            path: ':id',
            breadcrumbName: ({ query }) => {
              return `${query?.configSetName}`;
            },
            layout: { fullHeight: true },
            getComp: (cb: RouterGetComp) => cb(import('./pages/setting/detail')),
          },
        ],
      },
    ],
  },
];

export default getEcpRouter;
