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

function monitorStatusRouter(): RouteConfigItem {
  return {
    path: 'status',
    breadcrumbName: i18n.t('msp:active monitoring'),
    routes: [
      {
        getComp: (cb) => cb(import('status-insight/pages/status/status')),
        layout: {
          noWrapper: true,
        },
      },
      {
        path: ':metricId',
        breadcrumbName: ({ infoMap, params }) => {
          const { monitorStatusDetail } = infoMap;
          const { metricId } = params;
          let statusServiceName = i18n.t('msp:statistics details');
          if (monitorStatusDetail.metrics) {
            const target = monitorStatusDetail.metrics[metricId];
            if (target) {
              statusServiceName = target.name || statusServiceName;
            }
          }
          return statusServiceName;
        }, // '监控详情',
        alwaysShowTabKey: 'status',
        getComp: (cb) => cb(import('status-insight/pages/status/status-detail')),
      },
    ],
  };
}

export default monitorStatusRouter;
