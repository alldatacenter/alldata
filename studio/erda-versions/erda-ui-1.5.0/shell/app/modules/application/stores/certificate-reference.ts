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
import { getCertRefList, addCertRef, deleteCertRef, pushToConfig } from 'application/services/app-setting';
import i18n from 'i18n';

const certificateReference = createCRUDStore<Certificate.Detail>({
  name: 'appCertificateImport',
  services: {
    get: getCertRefList,
    add: addCertRef,
    delete: deleteCertRef,
  },
  effects: {
    pushToConfig({ call }, payload: APP_SETTING.PushToConfigBody) {
      return call(pushToConfig, payload, { successMsg: i18n.t('operated successfully') });
    },
  },
});

export default certificateReference;
