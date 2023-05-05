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

import { createStore } from 'core/cube';
import layoutStore from 'layout/stores/layout';
import { some } from 'lodash';
import { goTo } from 'common/utils';

const orgCenterStore = createStore({
  name: 'orgCenter',
  state: {},
  subscriptions: async ({ listenRoute }: IStoreSubs) => {
    listenRoute(({ isEntering, isMatch }) => {
      if (isEntering('orgCenter')) {
        const subSiderInfoMap = layoutStore.getState((s) => s.subSiderInfoMap);
        const orgMenus = subSiderInfoMap.orgCenter.menu;
        const hasAuth = some(orgMenus, (menu) => location.pathname.includes(menu.href));
        if (!hasAuth && orgMenus.length) {
          goTo(orgMenus[0].href);
        }
      }
    });
  },
});

export default orgCenterStore;
