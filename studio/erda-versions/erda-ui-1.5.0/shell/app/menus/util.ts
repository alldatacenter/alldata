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

import { map, compact, get } from 'lodash';
import i18n from 'i18n';
import orgStore from 'app/org-home/stores/org';

interface IMenuItem {
  [pro: string]: any;
  key?: string;
}

export enum MENU_SCOPE {
  appCenter = 'appCenter',
  orgCenter = 'orgCenter',
  cmp = 'cmp',
  dop = 'dop',
}
const defaultFunc = (a: any) => a;

const menuFilterMap = {
  appCenter: {
    // 应用中心
    cmp: (item: IMenuItem) => {
      const name = i18n.t('Cloud management');
      return { ...item, name, breadcrumbName: name };
    },
  },
  orgCenter: {
    // 企业中心
    orgMarket: (item: IMenuItem) => {
      const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);
      return !publisherId ? null : item;
    },
    orgCertificate: (item: IMenuItem) => {
      const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);
      return !publisherId ? null : item;
    },
  },
  cmp: {
    // 云管平台
    cmpOverview: (item: IMenuItem) => {
      const text = i18n.t('cluster overview');
      return { ...item, text };
    },
  },
  dop: {
    dopPublisher: (item: IMenuItem) => {
      const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);
      return !publisherId ? null : item;
    },
  },
};

export const filterMenu = (menu: IMenuItem[], type: MENU_SCOPE) => {
  return compact(
    map(menu, (item) => {
      const func = get(menuFilterMap, `${type}.${item.key}`) || defaultFunc;
      const reItem = func(item);
      if (reItem && reItem.subMenu) {
        reItem.subMenu = compact(
          map(reItem.subMenu, (subItem) => {
            const subFunc = get(menuFilterMap, `${type}.${subItem.key}`) || defaultFunc;
            return subFunc(subItem);
          }),
        );
      }
      return reItem;
    }),
  );
};
