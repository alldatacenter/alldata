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

/**
 * No UI, No API
 */
import React, { Suspense, lazy, useEffect, useCallback } from 'react';
import { HashRouter as Router, Switch, Route, Redirect } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { UseRequestProvider } from 'ahooks';
import { useLocation, useHistory } from 'react-router';
import { Provider } from 'react-redux';
import routes, { RouteProps } from '@/configs/routes';
import { State } from '@/core/stores';
import request from '@/core/utils/request';
import { localesConfig } from '@/configs/locales';
import store from './stores';
import { config } from '@/configs/default';

const { AppProvider, AppLoading, AppLayout, useLogin, redirectRoutes } = config;

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
        exact
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

  return (
    <>
      <Switch>
        {useLogin && (
          <Route
            exact
            path="/login"
            render={() => {
              const LazyLogin = lazy(() => import('@/ui/pages/Login'));
              return (
                <Suspense fallback={<AppLoading />}>
                  <LazyLogin />
                </Suspense>
              );
            }}
          />
        )}

        <AppLayout>
          <Suspense fallback={<AppLoading />}>
            <Switch>
              {Object.keys(redirectRoutes).map(path => (
                <Route
                  exact
                  key={path}
                  path={path}
                  render={() => <Redirect to={redirectRoutes[path]} />}
                />
              ))}
              {renderRoutes(routes)}
            </Switch>
          </Suspense>
        </AppLayout>
      </Switch>
    </>
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
      <AppProvider>
        <App />
      </AppProvider>
    </UseRequestProvider>
  </Router>
);

// eslint-disable-next-line
export default () => (
  <Provider store={store}>
    <Content />
  </Provider>
);
