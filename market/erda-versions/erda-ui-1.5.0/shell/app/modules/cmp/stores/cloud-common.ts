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
import { getCloudRegion, setCloudResourceTags } from '../services/cloud-common';
import { getCloudAccounts } from '../services/cloud-accounts';
import i18n from 'i18n';

interface IState {
  regions: CLOUD.Region[];
  cloudAccountExist: boolean;
}

const initState: IState = {
  regions: [],
  cloudAccountExist: false,
};

const cloudCommon = createFlatStore({
  name: 'cloudCommon',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering }) => {
      if (isEntering('cmp')) {
        cloudCommon.checkCloudAccount();
      }
    });
  },
  effects: {
    async getCloudRegion({ call, update }) {
      // 目前只有阿里云，暂时写死
      const regions = await call(getCloudRegion, { vendor: 'aliyun' });
      update({ regions });
    },
    async checkCloudAccount({ call, update }) {
      const { list } = await call(getCloudAccounts, { pageNo: 1, pageSize: 10 });
      update({ cloudAccountExist: list.length > 0 });
    },
    async setCloudResourceTags({ call }, data: CLOUD.SetTagBody) {
      return call(setCloudResourceTags, data, { successMsg: i18n.t('set successfully'), fullResult: true });
    },
  },
});

export default cloudCommon;
