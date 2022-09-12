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
import { useHistory, useSelector, useDispatch, useRequest } from '@/hooks';
import { State } from '@/models';
import { useTranslation } from 'react-i18next';
// import { FileTextOutlined } from '@/components/Icons';
import LocaleSelect from './LocaleSelect';
import styles from './index.module.less';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const userName = useSelector<State, State['userName']>(state => state.userName);
  const history = useHistory();
  const dispatch = useDispatch();

  const { run: runLogout } = useRequest('/anno/logout', {
    manual: true,
    onSuccess: () => {
      localStorage.removeItem('userName');
      history.push('/login');
      dispatch({
        type: 'setUser',
        payload: {
          userName: null,
        },
      });
    },
  });

  const menu = (
    <Menu>
      <Menu.Item onClick={runLogout}>{t('components.Layout.NavWidget.Logout')}</Menu.Item>
    </Menu>
  );

  return (
    <div style={{ marginRight: '20px' }}>
      <span className={styles.iconToolBar}>
        {/* <Tooltip placement="bottom">
          <a href="http://localhost" target="_blank" rel="noopener noreferrer">
            <FileTextOutlined />
          </a>
        </Tooltip> */}
        <LocaleSelect />
      </span>
      <Dropdown overlay={menu} placement="bottomLeft">
        <span>{userName}</span>
      </Dropdown>
    </div>
  );
};

export default Comp;
