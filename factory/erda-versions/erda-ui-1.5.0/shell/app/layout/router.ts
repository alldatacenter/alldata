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

import i18n from 'core/i18n';

function getLayoutRouter(): RouteConfigItem[] {
  return [
    {
      path: 'noAuth',
      toMark: 'orgIndex',
      breadcrumbName: i18n.t('layout:error page'),
      getComp: (cb) => cb(import('layout/common/error-page'), 'NoAuth'),
      layout: {
        use: 'error',
        noWrapper: true,
      },
    },
    {
      path: 'freshMan',
      toMark: 'orgIndex',
      breadcrumbName: i18n.t('layout:error page'),
      getComp: (cb) => cb(import('layout/common/error-page'), 'NotJoinOrg'),
      layout: {
        use: 'error',
        noWrapper: true,
      },
    },
    {
      path: 'notFound',
      toMark: 'orgIndex',
      breadcrumbName: i18n.t('layout:error page'),
      getComp: (cb) => cb(import('layout/common/error-page'), 'NotFound'),
      layout: {
        use: 'error',
        noWrapper: true,
      },
    },
    {
      path: 'inviteToOrg',
      toMark: 'orgIndex',
      breadcrumbName: i18n.t('layout:join organization'),
      getComp: (cb) => cb(import('layout/common/invite-to-org')),
      layout: {
        use: 'error',
        noWrapper: true,
      },
    },
  ];
}

export default getLayoutRouter;
