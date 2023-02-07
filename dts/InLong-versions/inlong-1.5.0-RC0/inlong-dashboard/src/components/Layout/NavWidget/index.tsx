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

import React, { useState } from 'react';
import { Dropdown, Menu } from 'antd';
import { useSelector } from '@/hooks';
import { State } from '@/models';
import { useTranslation } from 'react-i18next';
import request from '@/utils/request';
import LocaleSelect from './LocaleSelect';
import styles from './index.module.less';
import PasswordModal from './PasswordModal';
import KeyModal from './KeyModal';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const userName = useSelector<State, State['userName']>(state => state.userName);

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const [keyModal, setKeyModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const runLogout = async () => {
    await request('/anno/logout');
    window.location.href = '/';
  };

  const menuItems = [
    {
      label: t('components.Layout.NavWidget.PersonalKey'),
      key: 'mykey',
      onClick: () => setKeyModal({ visible: true }),
    },
    {
      label: t('components.Layout.NavWidget.EditPassword'),
      key: 'password',
      onClick: () => setCreateModal({ visible: true }),
    },
    {
      label: t('components.Layout.NavWidget.Logout'),
      key: 'logout',
      onClick: runLogout,
    },
  ];

  return (
    <div style={{ marginRight: '20px' }}>
      <span className={styles.iconToolBar}>
        <LocaleSelect />
      </span>
      <Dropdown overlay={<Menu items={menuItems} />} placement="bottomLeft">
        <span>{userName}</span>
      </Dropdown>
      <PasswordModal
        {...createModal}
        visible={createModal.visible as boolean}
        onCancel={() => setCreateModal({ visible: false })}
        onOk={async () => {
          runLogout();
          setCreateModal({ visible: false });
        }}
      />
      <KeyModal
        {...keyModal}
        visible={keyModal.visible as boolean}
        onCancel={() => setKeyModal({ visible: false })}
        onOk={async () => {
          setKeyModal({ visible: false });
        }}
      />
    </div>
  );
};

export default Comp;
