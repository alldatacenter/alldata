/**
 * Created by caoshuaibiao on 2019/6/5.
 * 路由入口及控制
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Router, Switch, Route, Redirect } from 'dva/router';
import dynamic from 'dva/dynamic';
import { ConfigProvider, Spin } from 'antd';

import zhCN from 'antd/lib/locale-provider/zh_CN';
import enUS from 'antd/lib/locale-provider/en_US';
import zhTW from 'antd/lib/locale-provider/zh_TW';
import Application from './core/Application';
import NotFound from './components/NotFound';

dynamic.setDefaultLoadingComponent(() => {
  return <Spin size="large" className="globalSpin" />;
});

//  注册模块路由
let routes = [];
const Routers = function ({ history, app }) {
  const Notfound = dynamic({
    app,
    component: () => NotFound,
  });

  let lng = zhCN;
  switch (window.APPLICATION_LANGUAGE) {
    case 'zh_CN':
      lng = zhCN;
      break;
    case 'en_US':
      lng = enUS;
      break;
    case 'zh_MO':
      lng = zhTW;
      break;
    default:
      lng = zhCN;
  }

  return (
    <ConfigProvider locale={lng}>
      <Router history={history}>
        <Application routes={routes} app={app}>
          <Switch>
            <Route component={Notfound} />
          </Switch>
        </Application>
      </Router>
    </ConfigProvider>
  );
};

Routers.propTypes = {
  history: PropTypes.object,
  app: PropTypes.object,
};

export default Routers;
