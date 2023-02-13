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

import { getOrgCenterMenu } from './orgCenter';
import { getCmpMenu } from './cmp';
import { getDopMenu } from './dop';
import { getMspMenu } from './msp';
import { getProjectMenu } from './project';
import { getAppMenu } from './application';
import { getEcpMenu } from './ecp';
import i18n from 'app/i18n';
import { produce } from 'immer';
import { filter, map } from 'lodash';
import { appList } from './appCenter';

export { getProjectMenu, getAppMenu, getOrgCenterMenu, getCmpMenu, getDopMenu, getMspMenu, getEcpMenu };

export const getAppCenterAppList = appList;

export const getSubSiderInfoMap = () => {
  const menus = {
    orgCenter: {
      menu: getOrgCenterMenu(),
      detail: {
        displayName: i18n.t('orgCenter'),
      },
    },
    cmp: {
      menu: getCmpMenu(),
      detail: {
        displayName: i18n.t('Cloud management'),
      },
    },
    dop: {
      menu: getDopMenu(),
      detail: {
        displayName: i18n.t('dop'),
      },
    },
    msp: {
      menu: getMspMenu(),
      detail: {
        displayName: i18n.t('msp'),
      },
    },
    ecp: {
      menu: getEcpMenu(),
      detail: {
        displayName: i18n.t('ecp:Edge computing'),
      },
    },
    // sysAdmin: {
    //   menu: getSysAdminMenu(),
    //   detail: {
    //     displayName: i18n.t('admin'),
    //   },
    // },
  };

  const filtered = produce(menus, (draft) => {
    map(draft, (obj) => {
      // eslint-disable-next-line no-param-reassign
      obj.menu = filter(obj.menu, (m: any) => {
        if (m.subMenu) {
          // eslint-disable-next-line no-param-reassign
          m.subMenu = m.subMenu.filter((s: any) => s.show !== false);
        }
        return m.show !== false;
      });
    });
  });
  return filtered;
};
