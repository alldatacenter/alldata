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

import alarmRouter from 'msp/alarm-manage/alarm-strategy';
import alarmRecordRouter from 'msp/alarm-manage/alarm-record';
import i18n from 'i18n';

const alarmManageRouters = [
  alarmRouter(),
  alarmRecordRouter(),
  {
    path: 'custom-alarm',
    breadcrumbName: i18n.t('msp:rule management'),
    routes: [
      {
        path: ':dashboardId',
        breadcrumbName: '{dashboardName}',
        layout: { fullHeight: true },
        getComp: (cb: RouterGetComp) => cb(import('msp/query-analysis/custom-dashboard/pages/custom-dashboard')),
      },
      {
        getComp: (cb: RouterGetComp) => cb(import('msp/alarm-manage/alarm-strategy/pages/custom-alarm')),
      },
    ],
  },
  {
    path: 'notify-group',
    breadcrumbName: i18n.t('msp:notification group management'),
    getComp: (cb: RouterGetComp) => cb(import('msp/alarm-manage/notify-group')),
  },
];

const getAlarmManageRouter = () => {
  return {
    path: ':terminusKey',
    routes: alarmManageRouters,
  };
};

export default getAlarmManageRouter;
