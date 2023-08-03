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
import { Dropdown } from 'antd';
import { useSelector } from '@/ui/hooks';
import { State } from '@/core/stores';
import { useTranslation } from 'react-i18next';
import request from '@/core/utils/request';
import PasswordModal from './PasswordModal';
import KeyModal from './KeyModal';
import { useLocalStorage } from '@/core/utils/localStorage';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const userName = useSelector<State, State['userName']>(state => state.userName);
  const [getLocalStorage, setLocalStorage, removeLocalStorage] = useLocalStorage('tenant');

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const [keyModal, setKeyModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const runLogout = async () => {
    await request('/anno/logout');
    removeLocalStorage('tenant');
    window.location.href = '/';
  };

  const menuItems = [
    {
      label: t('components.Layout.NavWidget.PersonalKey'),
      key: 'mykey',
      onClick: () => setKeyModal({ open: true }),
    },
    {
      label: t('components.Layout.NavWidget.EditPassword'),
      key: 'password',
      onClick: () => setCreateModal({ open: true }),
    },
    {
      label: t('components.Layout.NavWidget.Logout'),
      key: 'logout',
      onClick: runLogout,
    },
  ];

  return (
    <>
      <Dropdown menu={{ items: menuItems }} placement="bottomLeft">
        <span style={{ fontSize: 14 }}>{userName}</span>
      </Dropdown>
      <PasswordModal
        {...createModal}
        open={createModal.open as boolean}
        onCancel={() => setCreateModal({ open: false })}
        onOk={async () => {
          runLogout();
          setCreateModal({ open: false });
        }}
      />
      <KeyModal
        {...keyModal}
        open={keyModal.open as boolean}
        onCancel={() => setKeyModal({ open: false })}
        onOk={async () => {
          setKeyModal({ open: false });
        }}
      />
    </>
  );
};

export default Comp;
