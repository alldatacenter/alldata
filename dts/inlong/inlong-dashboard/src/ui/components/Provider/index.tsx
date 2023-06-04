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

import React, { useEffect, useCallback, useState } from 'react';
import { ConfigProvider, Spin } from 'antd';
import dayjs from 'dayjs';
import { useDispatch, useSelector, useRequest } from '@/ui/hooks';
import { State } from '@/core/stores';
import { localesConfig } from '@/configs/locales';
import i18n from '@/i18n';
import './antd.cover.less';

const Provider = ({ children }) => {
  const dispatch = useDispatch();

  const locale = useSelector<State, State['locale']>(state => state.locale);

  const [antdMessages, setAntdMessages] = useState();

  useRequest(
    {
      url: '/user/currentUser',
      method: 'POST',
    },
    {
      onSuccess: result => {
        dispatch({
          type: 'setUserInfo',
          payload: {
            userName: result.name,
            userId: result.userId,
            roles: result.roles,
          },
        });
      },
    },
  );

  const importLocale = useCallback(async locale => {
    if (!localesConfig[locale]) return;

    const { uiComponentPath, dayjsPath } = localesConfig[locale];
    const [messagesDefault, messagesExtends, antdMessages] = await Promise.all([
      import(
        /* webpackChunkName: 'default-locales-[request]' */
        `@/ui/locales/${locale}.json`
      ),
      import(
        /* webpackChunkName: 'extends-locales-[request]' */
        `@/ui/locales/extends/${locale}.json`
      ),
      import(
        /* webpackInclude: /(zh_CN|en_US)\.js$/ */
        /* webpackChunkName: 'antd-locales-[request]' */
        `antd/es/locale/${uiComponentPath}.js`
      ),
      import(
        /* webpackInclude: /(zh-cn|en)\.js$/ */
        /* webpackChunkName: 'dayjs-locales-[request]' */
        `dayjs/esm/locale/${dayjsPath}.js`
      ),
    ]);
    i18n.changeLanguage(locale);
    i18n.addResourceBundle(locale, 'translation', {
      ...messagesDefault.default,
      ...messagesExtends.default,
    });
    dayjs.locale(dayjsPath);
    setAntdMessages(antdMessages.default);
  }, []);

  useEffect(() => {
    importLocale(locale);
  }, [locale, importLocale]);

  return antdMessages ? (
    <ConfigProvider locale={antdMessages} autoInsertSpaceInButton={false}>
      {children}
    </ConfigProvider>
  ) : (
    <Spin />
  );
};

export default Provider;
