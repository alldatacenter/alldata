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
import React from 'react';
import { getAddons, approves } from '../services/index';
import layoutStore from 'layout/stores/layout';
import addonStore from 'common/stores/addon';
import permStore from 'user/stores/permission';
import i18n from 'i18n';
import { getTranslateAddonList } from 'app/locales/utils';
import { reduce } from 'lodash';
import { CATEGORY_NAME } from 'addonPlatform/pages/common/configs';
import { getSideMenu } from '../pages/addons/sidebar-menu';
import { ErdaIcon } from 'common';

interface IState {
  addonCategory: {
    [k: string]: ADDON.Instance[];
  };
  addonList: ADDON.Instance[];
  projectAddonCategory: {
    [k: string]: ADDON.Instance[];
  };
}

const initState: IState = {
  addonCategory: {},
  addonList: [],
  projectAddonCategory: {},
};

const dopStore = createFlatStore({
  name: 'dop',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, params }) => {
      if (isEntering('addonsManage')) {
        const { projectId, insId, orgName } = params;
        permStore.effects.checkRouteAuth({
          type: 'project',
          id: projectId,
          routeMark: 'addonsManage',
        });
        addonStore.getAddonDetail(insId).then((ret) => {
          const { logoUrl, name, addonName } = ret;
          const prefixPath = `/${orgName}/dop/addonsManage/${projectId}`;
          const rootPath = `${prefixPath}/${insId}`;
          const menu = getSideMenu({ rootPath }) as any[];
          if (['log-analytics'].includes(addonName)) {
            menu.splice(1, 0, {
              key: 'console',
              href: `${rootPath}/${addonName}`,
              icon: 'rqrz',
              text: i18n.t('console'),
            });
          } else if (addonName === 'jvm-profiler') {
            menu.splice(1, 0, {
              key: 'profile',
              href: `${rootPath}/${addonName}`,
              icon: 'rqrz',
              text: i18n.t('JVM analysis'),
            });
          } else if (addonName === 'mysql') {
            menu.splice(1, 0, {
              href: `${rootPath}/mysql-settings`,
              icon: <ErdaIcon type="permissions" />,
              text: i18n.t('account management'),
              subMenu: [
                {
                  href: `${rootPath}/mysql-settings/account`,
                  text: i18n.t('cmp:database account'),
                },
                {
                  href: `${rootPath}/mysql-settings/consumer`,
                  text: i18n.t('dop:consumer manager'),
                },
              ],
            });
          }
          layoutStore.reducers.setSubSiderInfoMap({
            key: 'addonsManage',
            menu,
            loading: false,
            detail: {
              name,
              logo: logoUrl,
            },
          });
        });
      }
    });
  },
  effects: {
    async getDopAddons({ call, update }) {
      let addonList = await call(getAddons, { type: 'workbench', value: 'workbench' });
      addonList = getTranslateAddonList(addonList, 'name');
      update({ addonList });
      dopStore.getAddonsSuccess({ addonList, type: 'addonCategory' });
    },
    async getProjectAddons({ call, getParams, update }, projectId?: string) {
      const { projectId: paramProjectId } = getParams();
      const _projectId = paramProjectId || projectId;
      let addonList = await call(getAddons, { type: 'project', value: _projectId });
      update({ addonList });
      addonList = getTranslateAddonList(addonList, 'name');
      dopStore.getAddonsSuccess({ addonList, type: 'projectAddonCategory' });
      return addonList;
    },
    async getDataSourceAddons({ call, getParams, update }, { displayName }: Omit<ADDON.DataSourceAddon, 'projectId'>) {
      const { projectId } = getParams();
      let addonList = await call(getAddons, {
        type: 'database_addon',
        value: projectId,
        projectId,
        displayName,
        workspace: ['TEST', 'DEV'],
      });
      update({ addonList });
      addonList = getTranslateAddonList(addonList, 'name');
      return addonList;
    },
    async applyUnblock({ call }, payload: PROJECT.Approves) {
      const res = await call(approves, payload, { successMsg: i18n.t('default:applied successfully') });
      return res;
    },
  },
  reducers: {
    getAddonsSuccess(state, payload) {
      const { addonList, type } = payload;
      const addonCategory = reduce(
        addonList,
        (result, value) => {
          // eslint-disable-next-line no-param-reassign
          (result[CATEGORY_NAME[value.category]] || (result[CATEGORY_NAME[value.category]] = [])).push(value);
          return result;
        },
        {},
      );

      return { ...state, [type]: addonCategory };
    },
    clearDopAddons(state) {
      return { ...state, addonCategory: {}, addonList: [] };
    },
    clearProjectAddons(state) {
      return { ...state, projectAddonCategory: {}, addonList: [] };
    },
  },
});

export default dopStore;
