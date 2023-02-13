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
import { AddStrategyPageName, EditStrategyPageName } from 'cmp/common/alarm-strategy/strategy-form';

function AlarmRouter() {
  return {
    path: 'alarm',
    routes: [
      {
        pageName: i18n.t('alarm strategy'),
        getComp: (cb) => cb(import('msp/alarm-manage/alarm-strategy/pages/alarm-index')),
      },
      {
        path: 'add-strategy',
        breadcrumbName: i18n.t('cmp:new alarm strategy'),
        pageNameInfo: AddStrategyPageName,
        getComp: (cb) => cb(import('msp/alarm-manage/alarm-strategy/pages/alarm-index/msp-strategy')),
      },
      {
        path: 'edit-strategy/:id',
        breadcrumbName: i18n.t('cmp:edit alarm strategy'),
        pageNameInfo: EditStrategyPageName,
        getComp: (cb) => cb(import('msp/alarm-manage/alarm-strategy/pages/alarm-index/msp-strategy')),
      },
    ],
  };
}

export default AlarmRouter;
