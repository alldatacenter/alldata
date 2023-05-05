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

import { createFlatStore } from 'core/cube';
import { getLS } from 'common/utils';
import i18n from 'i18n';
import { isEmpty } from 'lodash';
import { getDomains, updateDomains, updateK8SDomain } from '../services/domain';
import runtimeStore from './runtime';

interface State {
  domainMap: RUNTIME_DOMAIN.DomainMap;
}

const initState: State = {
  domainMap: {},
};

const domain = createFlatStore({
  name: 'runtimeDomain',
  state: initState,
  effects: {
    async getDomains({ call, update, getParams }, payload: { domainType: string }) {
      const { runtimeId } = getParams();
      // 可能runtimeDetail还没返回就已经跳转页面了，此时runtimeId可能为空
      if (runtimeId) {
        const domainMap = await call(getDomains, { runtimeId, ...payload });
        update({ domainMap });
      }
    },
    async updateDomains({ call, getParams }) {
      const { runtimeId } = getParams();
      const domainMap = getLS(`${runtimeId}_domain`);
      if (isEmpty(domainMap)) return;
      await call(updateDomains, { data: domainMap, runtimeId });
      runtimeStore.setHasChange(true);
      await domain.getDomains({ domainType: 'domains' });
    },
    async updateK8SDomain({ call }, payload: RUNTIME_DOMAIN.UpdateK8SDomainBody) {
      await call(updateK8SDomain, payload, { successMsg: i18n.t('runtime:domain name takes effect immediately') });
    },
  },
});

export default domain;
