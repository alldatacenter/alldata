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

import { createCRUDStore } from 'common/stores/_crud_module';
import { getCloudECSList, stopCloudECS, startCloudECS, restartCloudECS, renewalCloudECS } from '../services/computing';
import i18n from 'i18n';

const computing = createCRUDStore<COMPUTING.ECS>({
  name: 'cloudECS',
  services: {
    get: getCloudECSList,
  },
  effects: {
    async stopCloudECS({ call }, payload: COMPUTING.ECSActionReq) {
      await call(stopCloudECS, { ...payload }, { successMsg: i18n.t('set successfully') });
    },
    async startCloudECS({ call }, payload: COMPUTING.ECSActionReq) {
      await call(startCloudECS, { ...payload }, { successMsg: i18n.t('set successfully') });
    },
    async restartCloudECS({ call }, payload: COMPUTING.ECSActionReq) {
      await call(restartCloudECS, { ...payload }, { successMsg: i18n.t('set successfully') });
    },
    async renewalCloudECS({ call }, payload: COMPUTING.ECSActionReq) {
      await call(renewalCloudECS, { ...payload }, { successMsg: i18n.t('set successfully') });
    },
  },
});

export default computing;
