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
import { Input, Button } from 'antd';
import { useDispatch, useHistory, useRequest } from '@/hooks';
import { useTranslation } from 'react-i18next';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import styles from './index.module.less';

const Comp: React.FC = () => {
  const { t } = useTranslation();
  const [form] = useForm();
  const dispatch = useDispatch();
  const history = useHistory();

  const config = [
    {
      type: <Input placeholder={t('pages.Login.PleaseEnterUserName')} />,
      name: 'username',
      rules: [{ required: true, message: t('pages.Login.UsernameCanNotBeEmpty') }],
    },
    {
      type: <Input.Password placeholder={t('pages.Login.PleaseEnterYourPassword')} />,
      name: 'password',
      rules: [
        { required: true, message: t('pages.Login.PasswordCanNotBeBlank') },
        { pattern: /^[a-z_\d]+$/, message: t('pages.Login.OnlyLowercaseWords') },
      ],
    },
  ];
  const [changedValues, setChangedValues] = useState<Record<string, unknown>>({});
  const { run: runLogin } = useRequest(
    {
      url: `/anno/login`,
      data: {
        ...changedValues,
      },
      method: 'post',
    },
    {
      manual: true,
      onSuccess: data => {
        dispatch({
          type: 'setUser',
          payload: {
            userName: data === 'success' ? changedValues.username : null,
          },
        });
        localStorage.setItem('userName', changedValues.username + '');
        history.push('/');
      },
    },
  );
  const login = async () => {
    await form.validateFields();
    runLogin();
  };
  const onEnter = e => {
    if (e.keyCode === 13) login();
  };

  return (
    <div className={styles.wrap} onKeyUp={onEnter}>
      <div className={styles['form-wrap']}>
        <img className={styles.logo} src={require('../../components/Icons/logo.svg')} alt="" />
        <div>
          <FormGenerator
            form={form}
            content={config}
            onValuesChange={(c, v) => setChangedValues(v)}
          />
        </div>
        <div className={styles['button-wrap']}>
          <span className={styles['button-desc']}>{t('pages.Login.Reset')}</span>
          <Button type="primary" onClick={login}>
            {t('pages.Login.LogIn')}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Comp;
