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

import getMonitorRouter from './monitor/monitor-overview/router';
import getAlarmManageRouter from 'msp/alarm-manage';
import getEnvOverViewRouter from 'msp/env-overview/router';
import i18n from 'i18n';
import wrapper from './pages/wait-wrapper';
import getQueryAnalysisRouter from 'msp/query-analysis';

const injectWrapper = (route: RouteConfigItem): RouteConfigItem => {
  route.wrapper = wrapper;
  if (route.routes) {
    route.routes.forEach(injectWrapper);
  }
  return route;
};

function getMspRouter(): RouteConfigItem[] {
  return [
    {
      path: 'msp',
      mark: 'msp',
      toMark: 'orgIndex',
      routes: [
        {
          path: 'overview',
          layout: {
            hideHeader: true,
            noWrapper: true,
            showSubSidebar: false,
          },
          breadcrumbName: i18n.t('overview'),
          getComp: (cb) => cb(import('msp/pages/micro-service/overview')),
        },
        injectWrapper({
          path: ':projectId/:env/:tenantGroup',
          breadcrumbName: '{mspProjectName}',
          mark: 'mspDetail',
          routes: [
            {
              // compatible with old data, si redirects to transaction
              path: 'topology/:terminusKey/:applicationId/:runtimeName/:serviceName/si/:type',
              getComp: (cb) => cb(import('msp/pages/transfer/si')),
            },
            {
              path: 'synopsis',
              routes: [getEnvOverViewRouter()],
            },
            {
              path: 'monitor',
              // breadcrumbName: i18n.t('msp:monitoring platform'),
              // disabled: true,
              routes: [getMonitorRouter()],
            },
            {
              path: 'analysis',
              routes: [getQueryAnalysisRouter()],
            },
            {
              path: 'alarm-management',
              routes: [getAlarmManageRouter()],
            },
            {
              path: 'environment',
              routes: [
                {
                  path: ':terminusKey/configuration',
                  breadcrumbName: i18n.t('msp:access configuration'),
                  getComp: (cb) => cb(import('msp/env-setting/configuration')),
                },
                {
                  path: ':terminusKey/member',
                  breadcrumbName: i18n.t('cmp:member management'),
                  getComp: (cb) => cb(import('msp/env-setting/member-manage')),
                },
                {
                  path: 'info/:tenantId?',
                  breadcrumbName: i18n.t('msp:component info'),
                  layout: { fullHeight: true },
                  getComp: (cb) => cb(import('msp/env-setting/info')),
                },
              ],
            },
            {
              path: 'perm',
              pageName: i18n.t('role permissions description'),
              layout: { showSubSidebar: false, fullHeight: true },
              getComp: (cb) => cb(import('user/common/perm-editor/perm-editor'), 'MspPermEditor'),
            },
          ],
        }),
      ],
    },
  ];
}

export default getMspRouter;
