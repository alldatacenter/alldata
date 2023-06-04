/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Tooltip } from 'antd';
import { StarOutlined, BulbOutlined } from '@ant-design/icons';
import { config } from '@/configs/default';
import menusTree from '@/configs/menus';
import defaultSettings from './defaultSettings';
import { useLocation, useSelector } from '@/ui/hooks';
import { isDevelopEnv } from '@/core/utils';
import ProBasicLayout, {
  getMenuData,
  MenuDataItem,
  SettingDrawer,
  SettingDrawerProps,
} from '@ant-design/pro-layout';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { MenuProps } from 'antd/es/menu';
import { State } from '@/core/stores';
import NavWidget from './NavWidget';
import LocaleSelect from './NavWidget/LocaleSelect';

const BasicLayout: React.FC = props => {
  const location = useLocation();
  const [settings, setSetting] = useState(defaultSettings as SettingDrawerProps['settings']);
  const [openKeys, setOpenKeys] = useState<string[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<string[]>(['/']);
  const isDev = isDevelopEnv();
  const { pathname } = location;
  const roles = useSelector<State, State['roles']>(state => state.roles);
  const { breadcrumbMap, menuData } = useMemo(() => {
    const _menus = menusTree.filter(
      item => (item.isAdmin && roles?.includes('ADMIN')) || !item.isAdmin,
    );
    return getMenuData(_menus);
  }, [roles]);

  useEffect(() => {
    const firstPathname = `/${pathname.slice(1).split('/')?.[0]}`;
    const select = breadcrumbMap.get(firstPathname);
    if (select) {
      setOpenKeys((select as MenuDataItem)['pro_layout_parentKeys']);
      setSelectedKeys([(select as MenuDataItem)['key'] as string]);
    }
  }, [breadcrumbMap, pathname]);

  const menuItemRender = useCallback((menuItemProps, defaultDom) => {
    if (menuItemProps.isUrl || !menuItemProps.path) {
      return defaultDom;
    }
    return <Link to={menuItemProps.path}>{defaultDom}</Link>;
  }, []);

  const handleOnOpenChange = useCallback(keys => setOpenKeys(keys as string[]), []);

  const menuProps = useMemo<MenuProps>(() => {
    return {
      selectedKeys,
      openKeys,
      onOpenChange: handleOnOpenChange,
    };
  }, [handleOnOpenChange, openKeys, selectedKeys]);

  const [navTheme, setNavTheme] = useState(
    window.matchMedia('(prefers-color-scheme: dark)')?.matches ? 'realDark' : settings.navTheme,
  );

  useEffect(() => {
    window.matchMedia('(prefers-color-scheme: dark)').onchange = e => {
      setNavTheme(e.matches ? 'realDark' : settings.navTheme);
    };
  }, [settings.navTheme]);

  return (
    <>
      <ProBasicLayout
        {...settings}
        title={config.title}
        logo={<img src={config.logo} style={{ height: 50 }} alt="Logo" />}
        menuItemRender={menuItemRender}
        route={{
          path: '/',
          routes: menuData,
        }}
        navTheme={navTheme}
        menuProps={menuProps}
        actionsRender={() => [
          navTheme === 'realDark' ? (
            <Tooltip title="Dark">
              <StarOutlined onClick={() => setNavTheme(settings.navTheme)} />
            </Tooltip>
          ) : (
            <Tooltip title="Light">
              <BulbOutlined onClick={() => setNavTheme('realDark')} />
            </Tooltip>
          ),
          <LocaleSelect />,
          <NavWidget />,
        ]}
      >
        {props.children}
      </ProBasicLayout>

      {isDev && (
        <SettingDrawer
          getContainer={() => document.getElementById('root')}
          settings={settings}
          onSettingChange={setSetting}
          enableDarkTheme
        />
      )}
    </>
  );
};

export default BasicLayout;
