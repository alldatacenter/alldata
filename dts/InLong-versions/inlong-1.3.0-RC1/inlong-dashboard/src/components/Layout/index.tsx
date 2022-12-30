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

import { config } from '@/configs/default';
import menus from '@/configs/menus';
import defaultSettings from '@/defaultSettings';
import { useLocation, useSelector } from '@/hooks';
import { isDevelopEnv } from '@/utils';
import ProBasicLayout, {
  getMenuData,
  MenuDataItem,
  SettingDrawer,
  SettingDrawerProps,
} from '@ant-design/pro-layout';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { MenuProps } from 'antd/es/menu';
import { State } from '@/models';
import NavWidget from './NavWidget';

const renderMenuItem = (menus: MenuDataItem[]): MenuDataItem[] =>
  menus.map(({ icon, children, ...item }) => ({
    ...item,
    children: children && renderMenuItem(children),
  }));

const BasicLayout: React.FC = props => {
  const location = useLocation();
  const [settings, setSetting] = useState(defaultSettings as SettingDrawerProps['settings']);
  const [openKeys, setOpenKeys] = useState<string[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<string[]>(['/']);
  const isDev = isDevelopEnv();
  const { pathname } = location;
  const roles = useSelector<State, State['roles']>(state => state.roles);
  const { breadcrumbMap, menuData } = useMemo(() => {
    const _menus = menus.filter(
      item => (item.isAdmin && roles?.includes('ADMIN')) || !item.isAdmin,
    );
    return getMenuData(_menus);
  }, [roles]);

  useEffect(() => {
    const firstPathname = `/${pathname.slice(1).split('/')?.[0]}`;
    const select = breadcrumbMap.get(firstPathname);
    if (select) {
      // setOpenKeys((select as MenuDataItem)['pro_layout_parentKeys']);
      setSelectedKeys([(select as MenuDataItem)['key'] as string]);
    }
  }, [breadcrumbMap, pathname]);

  const menuDataRender = useCallback(() => renderMenuItem(menuData), [menuData]);

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

  return (
    <>
      <ProBasicLayout
        {...settings}
        title={config.title}
        logo={config.logo}
        menuDataRender={menuDataRender}
        menuItemRender={menuItemRender}
        menuHeaderRender={(logo, title) => (
          <a href="#/">
            {logo}
            {title}
          </a>
        )}
        menuProps={menuProps}
        rightContentRender={() => <NavWidget />}
        headerHeight={64}
      >
        {props.children}
      </ProBasicLayout>

      {isDev && (
        <SettingDrawer
          getContainer={() => document.getElementById('root')}
          settings={settings}
          onSettingChange={setSetting}
        />
      )}
    </>
  );
};

export default BasicLayout;
