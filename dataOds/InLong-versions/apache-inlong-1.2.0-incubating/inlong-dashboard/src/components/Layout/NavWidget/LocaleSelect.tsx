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

import React from 'react';
import { Dropdown, Menu } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { useSelector, useLocation } from '@/hooks';
import { State } from '@/models';
import { localesConfig } from '@/configs/locales';

const localeList = Object.keys(localesConfig).map(item => ({
  label: localesConfig[item].label,
  value: item,
}));

const SelectLang = () => {
  const location = useLocation();

  const locale = useSelector<State, State['locale']>(state => state.locale);

  const changeLang = ({ key }) => {
    window.location.href = window.location.href.replace(
      `/${locale}${location.pathname}`,
      `/${key}${location.pathname}`,
    );
  };

  const langMenu = (
    <Menu selectedKeys={[locale]} onClick={changeLang}>
      {localeList.map(item => (
        <Menu.Item key={item.value}>{item.label}</Menu.Item>
      ))}
    </Menu>
  );

  return (
    <Dropdown overlay={langMenu} placement="bottomRight">
      <GlobalOutlined />
    </Dropdown>
  );
};

export default SelectLang;
