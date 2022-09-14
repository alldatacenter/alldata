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

import React, { Suspense, lazy, useEffect, useCallback, useState } from 'react';
import { ConfigProvider, Spin } from 'antd';
import dayjs from 'dayjs';
import { HashRouter as Router, Switch, Route, Redirect } from 'react-router-dom';
import {
  useLocation,
  useHistory,
  useDispatch,
  UseRequestProvider,
  useSelector,
  useRequest,
} from '@/hooks';
import { PageLoading } from '@ant-design/pro-layout';
import { Provider } from 'react-redux';
import Layout from '@/components/Layout';
import routes, { RouteProps } from '@/configs/routes';
import { State } from '@/models';
import request from '@/utils/request';
import { localesConfig } from '@/configs/locales';
import store from './models';
import i18n from './i18n';
import '@/themes/index.less';
import Login from '@/pages/Login';

const lazyComponentCache: Record<string, ReturnType<typeof lazy>> = {};

const renderRoutes = function (routes: RouteProps[], parentPath = ''): any[] {
  return routes.reduce((accumulator, route: RouteProps) => {
    const compiledPath = `${parentPath}${route.path}`;
    const childRouteComponents = route.childRoutes
      ? renderRoutes(route.childRoutes, compiledPath)
      : [];

    if (!route.component) {
      return accumulator.concat(childRouteComponents);
    }

    return accumulator.concat(
      <Route
        key={compiledPath}
        path={compiledPath}
        exact={route.exact}
        strict={route.strict}
        render={props => {
          const LazyComponent = lazyComponentCache[compiledPath] || lazy(route.component);
          if (!lazyComponentCache[compiledPath]) {
            // No need to remount the component when writing querystring
            lazyComponentCache[compiledPath] = LazyComponent;
          }
          return <LazyComponent {...props} />;
        }}
      />,
      childRouteComponents,
    );
  }, []);
};
const App = () => {
  const location = useLocation();
  const history = useHistory();
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

    const { antdPath, dayjsPath } = localesConfig[locale];
    const [messagesDefault, messagesExtends, antdMessages] = await Promise.all([
      import(
        /* webpackChunkName: 'default-locales-[request]' */
        `@/locales/${locale}.json`
      ),
      import(
        /* webpackChunkName: 'extends-locales-[request]' */
        `@/locales/extends/${locale}.json`
      ),
      import(
        /* webpackInclude: /(zh_CN|en_US)\.js$/ */
        /* webpackChunkName: 'antd-locales-[request]' */
        `antd/es/locale/${antdPath}.js`
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

  const setCurrentMenu = useCallback(
    pathname => {
      dispatch({
        type: 'setCurrentMenu',
        payload: { pathname },
      });
    },
    [dispatch],
  );

  useEffect(() => {
    return history.listen(location => {
      const newLocale = location.pathname.split('/')[1];
      if (newLocale !== locale && localesConfig[newLocale]) {
        window.location.reload();
      }
    });
  }, [history, locale]);

  useEffect(() => {
    setCurrentMenu(location.pathname);
  }, [history, location, setCurrentMenu]);

  return antdMessages ? (
    <ConfigProvider locale={antdMessages} autoInsertSpaceInButton={false}>
      <Switch>
        <Route exact path="/login" render={() => <Login />} />
        <Layout>
          <Suspense fallback={<PageLoading />}>
            <Switch>
              <Route exact path="/" render={() => <Redirect to="/group" />} />
              {renderRoutes(routes)}
            </Switch>
          </Suspense>
        </Layout>
      </Switch>
    </ConfigProvider>
  ) : (
    <Spin />
  );
};

const Content = () => (
  <Router basename={`/${useSelector<State, State['locale']>(state => state.locale)}`}>
    <UseRequestProvider
      value={{
        pollingWhenHidden: false,
        loadingDelay: 200,
        throttleInterval: 1000,
        requestMethod: request,
      }}
    >
      <App />
    </UseRequestProvider>
  </Router>
);

// eslint-disable-next-line
export default () => (
  <Provider store={store}>
    <Content />
  </Provider>
);
