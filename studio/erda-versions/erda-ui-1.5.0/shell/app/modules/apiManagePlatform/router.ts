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
import { assetTabs } from 'apiManagePlatform/pages/api-market/list';

const getApiManagePlatformRouter = (): RouteConfigItem[] => [
  {
    path: 'apiManage',
    mark: 'apiManage',
    routes: [
      {
        path: 'api-market/:scope',
        breadcrumbName: i18n.t('API market'),
        tabs: assetTabs,
        routes: [
          {
            getComp: (cb) => cb(import('apiManagePlatform/pages/api-market/list')),
          },
          {
            path: ':assetID',
            routes: [
              {
                breadcrumbName: '{assetName}',
                getComp: (cb) => cb(import('apiManagePlatform/pages/api-market/version')),
                layout: {
                  noWrapper: true,
                },
              },
              {
                path: ':versionID',
                breadcrumbName: '{assetName}',
                getComp: (cb) => cb(import('apiManagePlatform/pages/api-market/detail')),
                layout: { noWrapper: true, fullHeight: true },
              },
            ],
          },
        ],
      },
      {
        path: 'api-design',
        breadcrumbName: i18n.t('dop:API design'),
        routes: [
          {
            layout: { fullHeight: true },
            getComp: (cb) => cb(import('apiManagePlatform/pages/api-market/design/index')),
          },
        ],
      },
      {
        path: 'access-manage',
        breadcrumbName: i18n.t('access management'),
        routes: [
          {
            getComp: (cb) => cb(import('apiManagePlatform/pages/access-manage/list')),
          },
          {
            path: 'access/:type',
            routes: [
              {
                getComp: (cb) => cb(import('apiManagePlatform/pages/access-manage/edit')),
              },
              {
                path: ':accessID',
                getComp: (cb) => cb(import('apiManagePlatform/pages/access-manage/edit')),
              },
            ],
          },
          {
            path: 'detail/:accessID',
            getComp: (cb) => cb(import('apiManagePlatform/pages/access-manage/detail')),
            layout: {
              noWrapper: true,
            },
          },
        ],
      },
      {
        path: 'client',
        breadcrumbName: i18n.t('my visit'),
        routes: [
          {
            getComp: (cb) => cb(import('apiManagePlatform/pages/client/list')),
          },
          {
            path: ':id',
            breadcrumbName: '{clientName}',
            getComp: (cb) => cb(import('apiManagePlatform/pages/client/detail')),
          },
        ],
      },
    ],
  },
];

export default getApiManagePlatformRouter;
