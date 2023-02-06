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

const CustomDashBoard = () => ({
  path: 'custom-dashboard',
  breadcrumbName: i18n.t('dop:dashboard'),
  routes: [
    {
      path: 'add',
      breadcrumbName: i18n.t('dop:dashboard'),
      layout: { fullHeight: true },
      getComp: (cb: RouterGetComp) => cb(import('msp/query-analysis/custom-dashboard/pages/custom-dashboard')),
    },
    {
      path: ':dashboardId',
      breadcrumbName: '{dashboardName}',
      layout: { fullHeight: true },
      getComp: (cb: RouterGetComp) => cb(import('msp/query-analysis/custom-dashboard/pages/custom-dashboard')),
    },
    {
      getComp: (cb: RouterGetComp) => cb(import('msp/query-analysis/custom-dashboard/pages')),
    },
  ],
});

export default CustomDashBoard;
