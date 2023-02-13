/**
 * Created by caoshuaibiao
 * 程序启动入口
 */
import React from 'react';
import ReactDOM from 'react-dom';
import dva from 'dva';
import createLoading from 'dva-loading';
import createHistory from 'history/createHashHistory';
import router from './router';
import AppService from './core/services/appService';
import MenuTreeService from './core/services/appMenuTreeService'
//不能去掉用于引入less.js来换肤使用
import less from 'less';
import 'antd/dist/antd.less';
import 'antd/dist/antd.css';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import * as util from './utils/utils';
import { dispatch } from 'dva'

import './index.less';
import { httpClient } from './core';

const app = dva({
    history: createHistory(),
    onError(error) {
        console.error(error.message)
    },
});

app.use(createLoading());

const modelsContext = require.context('./', true, /^\.\/models+\/[\s\S]*\.js$/);
const models = modelsContext.keys().map(key => modelsContext(key), []);

models.forEach(m => app.model(m));

//读取样式,根据不同的应用场景需要动态的适配
let themeType = localStorage.getItem('sreworks-theme');
if (!themeType) {
    localStorage.setItem('sreworks-theme', 'light');
}
if (themeType === 'dark') {
    themeType = 'navyblue';
    localStorage.setItem('sreworks-theme', 'navyblue');
}
{/* global THEMES */ }
if (themeType === 'navyblue') window.less.modifyVars(THEMES[themeType]);
app.router(router);
// 入口前初始化桌面数据 util.getNewBizApp().split(",")[1]
(async function () {
    let params = {
        namespaceId: (util.getNewBizApp() && util.getNewBizApp().split(",")[1]) || '',
        stageId: (util.getNewBizApp() && util.getNewBizApp().split(",")[2]) || '',
        visit: true,
    };
    try {
        const isLogined = await AppService.isLogined()
        if (isLogined && islogined['name']) {
            const res = await AppService.getAllProfile(params)
            let { collectList = [], customQuickList = [], workspaces } = res.profile;
            app.start();
            app._store.dispatch({ type: "home/setDesktopData", workspaces, collectList, customQuickList, isEqual: res.isEqual })
            app.start('#root');
        } else {
            return false
        }
    } catch (error) {
        app.start('#root');
    }
})();



