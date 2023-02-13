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

import React from 'react';
import { Badge, Tooltip, message, Avatar } from 'antd';
import AppCenter from './app-center';
import GlobalNavigation from './globalNavigation';
import { usePerm } from 'user/common';
import i18n from 'i18n';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import userStore from 'user/stores';
import messageStore from 'layout/stores/message';
import layoutStore from 'layout/stores/layout';
import { theme } from 'app/themes';
import { goTo, ossImg, insertWhen, getAvatarChars } from 'common/utils';
import { DOC_HELP_HOME, UC_USER_SETTINGS, erdaEnv } from 'common/constants';
import Logo from 'app/images/Erda.svg';
import orgStore from 'app/org-home/stores/org';
import routeStore from 'core/stores/route';
import './sidebar.scss';

const AppCenterEl = () => {
  const permMap = usePerm((s) => s.org);
  const appList = layoutStore.useStore((s) => s.appList);
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const { switchToApp } = layoutStore.reducers;
  const [visible, setVisible] = React.useState(false);

  const iconMap = {
    dop: 'devops1',
    sysAdmin: 'guanli',
    cmp: 'duoyun',
    msp: 'weifuwu1',
    orgCenter: 'guanli',
    fdp: 'dataEngineer',
    ecp: 'bianyuanjisuan',
    apiManage: 'apijishi',
  };

  const openMap = {
    orgCenter: permMap.entryOrgCenter.pass,
    cmp: permMap.cmp.showApp.pass,
    dop: permMap.dop.read.pass,
    fdp: permMap.entryFastData.pass && currentOrg.openFdp,
    msp: permMap.entryMsp.pass,
    ecp: erdaEnv.ENABLE_EDGE === 'true' && permMap.ecp.view.pass && currentOrg.type === 'ENTERPRISE',
    // apiManage: permMap.entryApiManage.pass,
  };
  const dataSource = appList
    .filter((app) => openMap[app.key])
    .map((app: LAYOUT.IApp) => {
      return {
        key: app.href || app.key,
        app,
        name: app.name,
        title: (
          <>
            <Avatar>{app.name.slice(0, 1)}</Avatar>
            <span>{app.name}</span>
          </>
        ),
      };
    });
  const onVisibleChange = (vis: boolean) => {
    if (currentOrg.id) {
      setVisible(vis);
    }
  };

  return (
    <AppCenter
      visible={visible}
      className="app-list"
      titleProp="name"
      title={
        <div className="flex items-center">
          {i18n.t('App Center')}
          <span className="app-center-version">{`v${process.env.mainVersion}`}</span>
        </div>
      }
      node={
        <Tooltip
          title={
            currentOrg?.id
              ? ''
              : i18n.t('layout:there is no organization information, please select an organization first')
          }
          placement="right"
        >
          <ErdaIcon type="appstore" color="white" size="20px" />
        </Tooltip>
      }
      linkRender={({ app }: { app: LAYOUT.IApp }) => {
        return (
          <a
            className="app-list-item inline-block w-full px-4"
            onClick={() => {
              switchToApp(app.key);
              goTo(app.href);
              setVisible(false);
            }}
            key={app.key}
          >
            <CustomIcon type={iconMap[app.key]} />
            <span className="pl-2 font-medium">{app.name}</span>
          </a>
        );
      }}
      dataSource={dataSource}
      onVisible={onVisibleChange}
    />
  );
};

const SideBar = () => {
  const loginUser = userStore.useStore((s) => s.loginUser);
  const [currentOrg, orgs] = orgStore.useStore((s) => [s.currentOrg, s.orgs]);
  const { switchMessageCenter } = layoutStore.reducers;
  const unreadCount = messageStore.useStore((s) => s.unreadCount);
  // 清掉旧版本缓存
  window.localStorage.removeItem('dice-sider');
  const curOrgName = currentOrg.name;
  const customIconStyle = { fontSize: 20, marginRight: 'unset' };
  const current = window.localStorage.getItem('locale') || 'zh';
  const isIn = routeStore.getState((s) => s.isIn);
  const isAdminRoute = isIn('sysAdmin');
  const operations = [
    {
      show: true,
      icon: (
        <Tooltip title={i18n.t('layout:view doc')} placement="right">
          <ErdaIcon type="help" className="mr-0 mt-1" size="20" />
        </Tooltip>
      ),
      onClick: () => {
        window.open(DOC_HELP_HOME);
      },
    },
    {
      show: true,
      icon: (
        <Tooltip title={i18n.t('default:switch language')} placement="right">
          <ErdaIcon type={current === 'zh' ? 'chinese' : 'english'} style={customIconStyle} />
        </Tooltip>
      ),
      onClick: () => {
        const next = current === 'zh' ? 'en' : 'zh';
        window.localStorage.setItem('locale', next);
        window.location.reload();
      },
    },
    {
      show: true,
      icon: (
        <Badge
          size="small"
          count={unreadCount}
          offset={[-5, 2]}
          className="message-icon select-none"
          style={{ boxShadow: 'none' }}
        >
          <ErdaIcon type="remind" className="mr-0 mt-0.5" size="20px" style={customIconStyle} />
        </Badge>
      ),
      onClick: () => switchMessageCenter(null),
    },
  ].filter((a) => a.show);

  const useMenuOperations = [
    ...insertWhen(!!loginUser.isSysAdmin, [
      {
        icon: <ErdaIcon type="user-config" />,
        title: <span className="ml-1">{i18n.t('operation manage platform')}</span>,
        onClick: () => {
          window.localStorage.setItem('lastOrg', window.location.pathname.split('/')[1]);
          goTo(goTo.pages.sysAdmin, { orgName: '-' });
        },
      },
    ]),
    ...insertWhen(loginUser.isNewUser || !!erdaEnv.UC_PUBLIC_URL, [
      {
        icon: <ErdaIcon type="user-config" />,
        title: i18n.t('layout:personal settings'),
        onClick: () => {
          window.open(loginUser.isNewUser ? UC_USER_SETTINGS : erdaEnv.UC_PUBLIC_URL);
        },
      },
    ]),
    {
      icon: <ErdaIcon className="mr-1" type="logout" size="14" />,
      title: i18n.t('layout:logout'),
      onClick: userStore.effects.logout,
    },
  ];

  const userMenu = {
    name: loginUser.nick || loginUser.name,
    avatar: {
      src: loginUser.avatar ? ossImg(loginUser.avatar, { w: 48 }) : undefined,
      chars: getAvatarChars(loginUser.nick || loginUser.name),
    },
    operations: useMenuOperations,
  };

  return (
    <GlobalNavigation
      layout="vertical"
      verticalBrandIcon={
        <img
          className="mr-0 cursor-pointer"
          src={Logo}
          style={{
            width: '19px',
            height: '19px',
          }}
          onClick={() => {
            const isIncludeOrg = !!orgs.find((x: Obj) => x.name === curOrgName);
            if (isAdminRoute) {
              const lastOrg = window.localStorage.getItem('lastOrg');
              const isInLastOrg = !!orgs.find((x: Obj) => x.name === lastOrg);
              goTo(goTo.pages.orgRoot, { orgName: isInLastOrg ? lastOrg : '-' });
            } else if (isIncludeOrg) {
              goTo(goTo.pages.orgRoot);
            } else if (!orgs?.length) {
              // skipping warning when the user doesn't join any organization.
              goTo(goTo.pages.orgRoot, { orgName: '-' });
            } else {
              message.warning(i18n.t('default:org-jump-tip'), 2, () => goTo(goTo.pages.orgRoot, { orgName: '-' }));
            }
          }}
        />
      }
      operations={operations}
      userMenu={userMenu}
      slot={
        !isAdminRoute
          ? {
              title: ({ compact }: any) => (compact ? <AppCenterEl /> : null),
              element: <AppCenterEl />,
            }
          : undefined
      }
    />
  );
};

export default SideBar;
