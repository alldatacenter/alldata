/**
 * Created by caoshuaibiao on 2018/8/15.
 * 顶部按照模块分类布局
 */

import React from 'react';
import { Layout, Alert, Button } from 'antd';
import DefaultHeader from './DefaultHeader';
import TopMenus from './common/TopMenus';
import LeftSiderMenus from './common/LeftSiderMenus';
import { Switch, Route, Redirect } from 'dva/router';
import ContentWithMenus from "./common/ContentWithMenus";
import NProgress from 'nprogress';
import properties from 'appRoot/properties';
import OamContent from '../core/framework/OamContent';
import cacheRepository from '../utils/cacheRepository';
import NoticeBar from './common/NoticeBar';
import ErrorBoundary from "../components/ErrorBoundary";
import NodeContent from "../core/framework/core/NodeContent";
import * as util from "../utils/utils";
import httpClient from '../utils/httpClient';
//其他布局引入
import BriefLayout from './BriefLayout';
import BriefHeader from "./BriefLayout/Header";
import SiderNavToggleBar from '../components/SiderNavToggleBar';
import Bus from '../utils/eventBus';

const { Content } = Layout;
let envs = [{ label: "日常", value: "daily" }, { label: "预发", value: "pre" }, { label: "生产", value: "prod" }];
NProgress.configure({ parent: '#top_progress' });
let routeItem = [];

class DefaultLayout extends React.Component {

    constructor(props) {
        super(props);
        const { global } = this.props;
        global.provider.progressBar = NProgress;
        global.provider.httpClient = httpClient;
        global.provider.properties = properties;
        httpClient.addRequestInterceptor(function (req) {
            NProgress.start();
            return req;
        });
        httpClient.addResponseInterceptor(function (res) {
            NProgress.done();
            return res;
        });
        if (properties.deployEnv === 'daily') {
            envs = [{ label: "日常", value: "daily" }]
        } else if (properties.deployEnv === 'prepub') {
            envs = [{ label: "预发", value: "pre" }]
        }
        if (properties.deployEnv !== 'local' && !global.currentProduct.isNeedDailyEnv) {
            envs = envs.filter(env => env.value !== "daily")
        }
        this.state = {
            themeFlag: true
        }
    }

    componentWillMount() {
        const { global, app } = this.props, props = this.props;
        const { accessRoutes, currentProduct } = global;
        const isV2 = currentProduct.version === 'v2' || currentProduct.appType === properties.ENV.PaaS;
        const genRoute = function (routeData, recursionDepth) {
            let { path, exact, component, type, children, layout, label, config, ...dynamics } = routeData;
            // analysis.registerPath(path,label);
            recursionDepth++;
            if (layout === 'top' && children && children.length > 0) {
                routeItem.push(<Route path={path} key={"_" + path} component={() => <ContentWithMenus {...props} routeData={routeData} isV2={isV2} />} />);
                return;
            } else {
                let displayChildren = children.filter(child => child.config && !child.config.hidden), defaultChildren;
                if (displayChildren && displayChildren.length) {
                    defaultChildren = displayChildren[0];
                } else {
                    defaultChildren = children[0];
                }
                if (type === 'custom') {
                    if (children && children.length > 0) {
                        if (isV2 && layout === 'custom') {
                            routeItem.push(<Route path={path} key={"_" + path} component={() => <NodeContent {...props} routeData={routeData} nodeData={routeData} nodeId={routeData.nodeId} />} />);
                            return;
                        } else {
                            routeItem.push(<Route exact path={path} key={"_" + path} component={() => (<Redirect to={defaultChildren.path} />)} />);
                        }
                    } else {
                        if (isV2) {
                            routeItem.push(<Route path={path} key={"_" + path} component={() => <NodeContent {...props} nodeData={routeData} nodeId={routeData.nodeId} />} />);
                        } else {
                            routeItem.push(<Route path={path} key={"_" + path} component={() => <OamContent {...props} menuRouteData={routeData} nodeId={routeData.nodeId} urlRequireNodeId={false} nodeConfig={routeData.config} />} />);
                        }
                    }
                } else {
                    //不是叶子节点时需要默认显示第一个叶子节点的内容
                    if (children && children.length > 0) {
                        routeItem.push(<Route key={"_" + path} exact={exact !== false} path={path} component={() => <Redirect to={defaultChildren.path} />} />);
                    }
                }
            }
            if (children && children.length > 0) {
                children.forEach(levelData => {
                    let rcDepth = recursionDepth;
                    genRoute(levelData, rcDepth);
                });
            }

        };
        accessRoutes.forEach(moduleData => {
            let recursionDepth = 0;
            //模块级别
            genRoute(moduleData, recursionDepth);
        });
    }

    componentDidMount() {
        //const { global,history} = this.props;
        //开启埋点统计
        if(localStorage.getItem("sreworks-theme") === "navyblue"){
            this.setState({
                themeFlag: localStorage.getItem("sreworks-theme") === "navyblue"? false : true
            })
        }
        Bus.on('themeChange', (themeType) => {
            let flag = false;
            if(themeType === 'navyBlue') {
                flag = false
            } else if(themeType === 'light') {
                flag = true
            } else {
                flag = false
            }
            this.setState({
                themeFlag: flag
            },()=> {
                console.log(this.state.themeFlag,'themeType-')
            })
        })

    };
    // componentWillUnmount(){
    //     Bus.off('themeChange')
    // }
    onSwitchSidebar = () => {
        const { dispatch } = this.props;
        dispatch({ type: 'global/switchSidebar' });
    };

    onSwitchTheme = () => {
        let themeType = (localStorage.getItem('sreworks-theme') === 'navyblue' ? 'light' : "navyblue");
        const { dispatch } = this.props;
        dispatch({ type: 'global/switchTheme', theme: themeType });
        {/* global THEMES */ }
        window.less.modifyVars(THEMES[themeType]);
        //window.location.reload();
    };

    onRoleChange = (roleId) => {
        /*let app=this.props.global.currentProduct.productId;
        cacheRepository.setRole(app,roleId);
        window.location.href=window.location.href.split("#")[0]+"#/"+app;
        window.location.reload();*/
        const { dispatch } = this.props;
        dispatch({ type: 'global/switchRole', roleId: roleId });
    };

    onSwitchMenuMode = (e) => {
        const { dispatch, global } = this.props;
        const key = e && e.key;
        if (key === 'left' || key === 'top') {
            dispatch({ type: 'global/switchFakeGlobal', payload: true });
            setTimeout(function () {
                dispatch({ type: 'global/switchFakeGlobal', payload: false });
                dispatch({ type: 'global/switchMenuMode', payload: key });
            }, 600);
        }
    };

    onSwitchMenuPopover = () => {
        const { dispatch } = this.props;
        dispatch({ type: 'global/switchMenuPopver' });
    };

    onModuleMenuChang = (moduleMenu) => {
        const { dispatch } = this.props;
        dispatch({ type: 'global/onModuleMenuChange', payload: { moduleName: moduleMenu.key } });
    };

    onEnvChang = (envItem) => {
        let app = this.props.global.currentProduct.productId;
        cacheRepository.setAppBizId(app, util.getNewBizApp().split(",")[1], envItem);
        window.location.reload();
    };

    onLanguageChange = (language) => {
        //console.log("language--------->",language);
        //暂时只支持两种语言
        let lng = localStorage.getItem("t_lang_locale") === "zh_CN" ? 'en_US' : 'zh_CN';
        localStorage.setItem('t_lang_locale', lng);
        //localeHelper.changeLocale(lng);
        const { dispatch } = this.props;
        dispatch({ type: 'global/switchLanguage', language: lng });
        //window.location.reload();
    };

    onLogout = () => {
        const { dispatch } = this.props;
        dispatch({ type: 'global/logout' });
    };

    render() {

        const { global, children, location, app } = this.props;
        const { siderFold, siderRespons, theme, menuMode, siderOpenKeys, menuResponsVisible, moduleName, currentUser, moduleGroups, accessRoutes, settings, currentProduct } = global;
        //if(accessRoutes.length===0) return null;
        let {themeFlag} = this.state;
        let routes = accessRoutes;
        // console.log(this.props,'this.props')
        // console.log(accessRoutes, 'accessRoutes-defaultLayout')
        //从routes中解析出当前加载的模块
        let moduleData = [], currentRoutes = [], currentModule = routes[0] || {}, exitChildren = false;
        routes.forEach(module => {
            moduleData.push({
                path: module.path,
                name: module.name,
                label: module.label,
                icon: module.icon,
                seq: module.seq,
                group: module.group,
                layout: module.layout,
                config: module.config,
                children: module.children
            });
            //module要求必须全局唯一
            if (location.pathname.indexOf(module.path) > -1) {
                currentModule = module;
                currentRoutes = module.children;
                //对没有子功能的模块做处理
                if (module.children && module.children.length > 0) {
                    exitChildren = true;
                }
            }
        });
        const { pathname } = location, { productId, nameCn, layout = {}, initAccessUrl } = currentProduct;
        //模块未设置默认显示时重定向至模块第一个显示模块,若第一个模块也是树,将会显示404
        if (pathname === currentModule.path && !currentModule.component && currentModule.children && currentModule.children.length > 0) {
            let displayChildren = currentModule.children.filter(child => child.config && !child.config.hidden), defaultPath = "";
            if (displayChildren && displayChildren.length) {
                defaultPath = displayChildren[0].path;
            } else {
                defaultPath = currentModule.path;
            }
            return <Redirect to={defaultPath} />;
        }
        if (pathname === `/${productId}` || pathname === '/' || pathname === `/${productId}/`) {
            if (initAccessUrl) return <Redirect to={initAccessUrl} />;
            currentModule = routes.filter(module => module.config && !module.config.hidden)[0] || currentModule;
            let displayChildren = currentModule.children.filter(child => child.config && !child.config.hidden), defaultPath = "";
            if (displayChildren && displayChildren.length) {
                defaultPath = displayChildren[0].path;
            } else {
                defaultPath = currentModule.path;
            }
            if (defaultPath) {
                return <Redirect to={defaultPath} />;
            }
            return <Redirect to={`/${productId}/portal/`} />;

        }

        const menuProps = {
            location,
            menuMode,
            theme,
            siderFold,
            routes: currentRoutes,
            currentModule,
            siderOpenKeys,
            onSwitchTheme: this.onSwitchTheme,
            onSwitchSidebar: this.onSwitchSidebar,
            onMenuOpenChange: this.onMenuOpenChange,
            onSwitchMenuMode: this.onSwitchMenuMode
        };

        const headerProps = {
            siderFold,
            siderRespons,
            theme,
            siderOpenKeys,
            menuResponsVisible,
            moduleData,
            currentModule,
            currentUser,
            moduleGroups,
            settings,
            currentProduct,
            onSwitchTheme: this.onSwitchTheme,
            onSwitchSidebar: this.onSwitchSidebar,
            onSwitchMenuMode: this.onSwitchMenuMode,
            onRoleChange: this.onRoleChange,
            onSwitchMenuPopover: this.onSwitchMenuPopover,
            onModuleMenuChang: this.onModuleMenuChang,
            onLanguageChange: this.onLanguageChange,
            onEnvChang: this.onEnvChang,
            onLogout: this.onLogout,
            envs: envs,
            global: this.props.global
        };

        let hasTop = currentModule.layout === 'top-left' && currentModule.children && currentModule.children.length > 0, currentRoleId = cacheRepository.getRole(productId), currEnvkey = cacheRepository.getBizEnv(productId);
        let currentRole = currentUser.roles.filter(role => role.roleId === currentRoleId)[0], currentEnv = envs.filter(env => env.value === currEnvkey)[0];
        if (properties.envFlag === properties.ENV.Internal) {
            if (!currentRole) {
                console.error("服务返回角色与当前角色不匹配:", currentUser.roles, currentRoleId);
            }
            if (!currentEnv) {
                console.error("环境列表与当前环境不匹配:", currentEnv, currEnvkey);
            }
        }
        let { type, header = {}, contentPadding } = layout;
        const commonContent = (
            <div>
                {/*<Beard {...beardProps} />*/}
                <div style={{ minHeight: 'calc(100vh - 135px)' }}>
                    <ErrorBoundary>
                        <Switch>
                            {routeItem}
                            {children}
                        </Switch>
                    </ErrorBoundary>
                </div>
                {
                    properties.envFlag === properties.ENV.Internal ?
                        <div className="common-footer" key="_foot_key">
                            {`Copyright© 2014-${(new Date()).getFullYear()} 阿里巴巴-${nameCn} Powered by SREWorks`}
                        </div> : null
                }
            </div>
        );
        if (type === 'brief') {
            return <BriefLayout menuProps={menuProps} headerProps={headerProps}
                layout={layout}
                currentProduct={currentProduct} currentUser={currentUser}
                content={commonContent}
            />
        }
        if (type === "empty") {
            return <div>
                <div id="top_progress" style={{ height: 1, marginTop: -1, zIndex: 10 }} />
                <div id="__MAIN_CONTENT__" className="home-page-content">
                    <ErrorBoundary>
                        <Switch>
                            {/*<Route path={`/${productId}/noPermission`} component={NoPermissionPage}/>*/}
                            {routeItem}
                            {/*{children}*/}
                        </Switch>
                    </ErrorBoundary>
                </div>
            </div>;
        }
        return (
            <div className="layout-left">
                <Layout>
                    {
                        header.type === 'brief' ?
                            <div className="abm-default-brief-header-container ">
                                <div style={{ paddingTop: 0 }} className="brief-sider-nav-toggleBar">
                                    <SiderNavToggleBar theme={localStorage.getItem('sreworks-theme')} />
                                </div>
                                <div>
                                    <BriefHeader {...headerProps} />
                                </div>
                            </div>
                            : <DefaultHeader {...headerProps} />
                    }
                    <div id="top_progress" style={{ height: 1, marginTop: -1, zIndex: 10 }} />
                    <TopMenus {...menuProps} />
                    <Layout style={{ height: `calc(100vh - ${hasTop ? 90 : 60}px)`, overflowY: 'scroll', overflowX: 'hidden' }} id="__MAIN_CONTENT__">
                        <NoticeBar />
                        <Content>
                            <Layout className={themeFlag ? "mixin-background" : "mixin-background-black"} >
                                <LeftSiderMenus key={currentModule.name} {...menuProps} />
                                <Content
                                    style={{ padding: header.type === "brief" ? "12px 15vw 0 15vw" : (contentPadding ? contentPadding : "8px 8px 0px 8px") }}>
                                    {commonContent}
                                </Content>
                            </Layout>
                        </Content>
                    </Layout>
                </Layout>
            </div>
        );
    }
}

export default DefaultLayout;

