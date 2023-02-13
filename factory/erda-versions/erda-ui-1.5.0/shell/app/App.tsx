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

// external modules
import React from 'react';
import ReactDOM from 'react-dom';
import { get } from 'lodash';
import moment from 'moment';
import 'moment/locale/zh-cn';
import { Pagination, message, ConfigProvider } from 'antd';
import antd_zhCN from 'antd/es/locale-provider/zh_CN';
import antd_enUS from 'antd/es/locale-provider/en_US';
import { IconProvider, DEFAULT_ICON_CONFIGS } from '@icon-park/react/es/runtime';
// core modules
import { isZh } from 'core/i18n';
import { startApp, registerModule } from 'core/index';
import { setConfig, getConfig } from 'core/config';
// common modules
import { registChartControl } from 'charts/utils/regist';
import { setGlobal } from 'app/global-space';
import { PAGINATION } from 'app/constants';
import { erdaEnv } from 'common/constants';
import { EmptyListHolder } from 'common';
import { setLS, notify, insertWhen } from 'common/utils';
import { initAxios } from 'common/utils/axios-config';
import { getResourcePermissions } from 'user/services/user';
import userStore from './user/stores';
import permStore from 'user/stores/permission';
import { getJoinedOrgs } from 'app/org-home/services/org';
import orgStore, { isAdminRoute } from 'app/org-home/stores/org';
import modules from './mf-modules'; // ambiguous modules may conflict with modules folder, then rename to mf-modules
import './styles/antd-extension.scss';
import './styles/app.scss';
import '@erda-ui/dashboard-configurator/dist/index.css';
import 'tailwindcss/tailwind.css';

setConfig('onAPISuccess', message.success);
setConfig('onAPIFail', notify);

const history = getConfig('history');

const momentLangMap = {
  en: 'en',
  zh: 'zh-cn',
};

Pagination.defaultProps = {
  showSizeChanger: false,
  ...Pagination.defaultProps,
  pageSize: PAGINATION.pageSize,
  pageSizeOptions: PAGINATION.pageSizeOptions,
  showTotal: (total) => (isZh() ? `共计 ${total} 条` : `total ${total} items`),
};

const start = (userData: ILoginUser, orgs: ORG.IOrg[]) => {
  setLS('diceLoginState', true);

  const locale = window.localStorage.getItem('locale') || 'zh';
  moment.locale(momentLangMap[locale]);
  orgStore.reducers.updateJoinedOrg(orgs);
  initAxios();
  startApp().then(async (App) => {
    // get the organization info first, or will get org is undefined when need org info (like issueStore)
    const orgName = get(location.pathname.split('/'), '[1]');
    await orgStore.effects.getOrgByDomain({ orgName });
    [
      import('layout/entry'),
      import('org/entry'),
      import('app/org-home/entry'),
      import('dop/entry'),
      import('runtime/entry'),
      import('publisher/entry'),
      import('project/entry'),
      import('apiManagePlatform/entry'),
      import('msp/entry'),
      ...insertWhen(erdaEnv.ENABLE_EDGE === 'true', [import('app/modules/ecp/entry')]),
      import('application/entry'),
      import('cmp/entry'),
      import('user/entry'),
      import('dcos/entry'),
      import('addonPlatform/entry'),
      import('./modules/extra/entry'),
      ...Object.values(modules),
    ].forEach((p) => p.then((m) => m.default(registerModule)));
    userStore.reducers.setLoginUser(userData); // 需要在app start之前初始化用户信息
    const Wrap = () => {
      return (
        <ConfigProvider renderEmpty={EmptyListHolder} locale={isZh() ? antd_zhCN : antd_enUS}>
          <App />
        </ConfigProvider>
      );
    };

    ReactDOM.render(<Wrap />, document.getElementById('erda-content'));
    delete window._userData;
    registChartControl();
  });
};

const init = (userData: ILoginUser) => {
  // step1: get user last path
  window.localStorage.removeItem(`lastPath`); // clear old lastPath
  const lastPath = window.localStorage.getItem(`${userData.id}-lastPath`);
  if (lastPath) {
    window.localStorage.removeItem(`${userData.id}-lastPath`);
    history.replace(lastPath);
  }

  // step2: get user isSysAdmin
  getResourcePermissions({ scope: 'sys', scopeID: '0' })
    .then((result) => {
      if (result.success) {
        let data: ILoginUser = { ...userData };
        if (result.data?.access) {
          permStore.reducers.updatePerm('sys', result.data);
          setGlobal('erdaInfo.isSysAdmin', true);
          const { roles } = result.data;
          data = { ...data, isSysAdmin: true, adminRoles: roles };
        } else if (isAdminRoute()) {
          history.replace('/');
        }
        return data;
      } else {
        return Promise.reject(Error('fetch sys permission failed'));
      }
    })
    .then((perRes: ILoginUser) => {
      // step3: get user joined orgs
      // TODO check if admin has org permissions
      getJoinedOrgs().then((orgResult) => {
        const orgs = orgResult.data?.list || [];
        if (location.pathname === '/') {
          // replace to default org
          const defaultOrgPath = `/${orgs?.[0]?.name || '-'}`;
          history.replace(defaultOrgPath);
        }
        start(perRes, orgs);
      });
    });
};

if (window._userData) {
  init(window._userData);
} else {
  window.userCb = init;
}
