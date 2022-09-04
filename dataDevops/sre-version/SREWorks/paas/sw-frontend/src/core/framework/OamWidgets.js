/**
 * Created by caoshuaibiao on 2019/2/21.
 * 运维内容区
 */

import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { List, Card, Menu, message } from 'antd';
import { Route, withRouter, Link, Switch, Redirect } from 'dva/router';
import OamWidget from './OamWidget';
import { connect } from 'dva';
import _ from 'lodash';
import JSXRender from "../../components/JSXRender";
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import httpClient from '../../utils/httpClient';
import '../style.less';


@connect(({ global, node }) => ({
    nodeParams: node.nodeParams,
    theme: global.theme,
    currentUser: global.currentUser,
    language: global.language
}))
export default class OamWidgets extends React.Component {

    constructor(props) {
        super(props);
        let { metaData, nodeId, match, modulePath, dispatch, defaultNodeParams, nodeParams, currentUser } = props, urlSearchParams = util.getUrlParams();
        let children = metaData.children || [], routeData = {}, initNodeParams = Object.assign({ __currentUser__: currentUser }, defaultNodeParams, urlSearchParams);
        if (metaData.config && metaData.config.resetNodeParams) {
            let newNodeParams = {};
            Object.keys(nodeParams).forEach(key => {
                //只保留内部变量
                if (key && key.startsWith && key.startsWith("__")) {
                    newNodeParams[key] = nodeParams[key]
                }
            });
            initNodeParams = Object.assign(newNodeParams, initNodeParams);
            dispatch({ type: 'node/resetParamContext' });
            dispatch({ type: 'node/initParams', initParams: { nodeParams: initNodeParams } });
        } else {
            initNodeParams = Object.assign(nodeParams, initNodeParams);
        }
        if (children.length > 0) {
            let pathPerfix = match.path;
            if (modulePath) {
                pathPerfix = modulePath;
            }
            routeData = {
                children: [],
                icon: "",
                label: "",
                name: "",
                path: pathPerfix,
            };
            routeData.children = children.map(child => {
                return {
                    icon: child.icon || "",
                    label: child.label,
                    name: child.name,
                    path: routeData.path + "/" + child.name,
                    config: Object.assign({}, child.config),
                    component: () => <OamWidgets {...props} metaData={child}
                        widgets={child.components || child.elements}
                        layout={child.layout}
                        nodeId={nodeId}
                        modulePath={routeData.path + "/" + child.name}
                        defaultNodeParams={initNodeParams}

                    />
                }
            });
        }
        this.state = {
            routeData: routeData,
            preCheckPass: false,
            awaitPageParams: true
        };
        this.pageParams = false;
        dispatch({ type: 'node/initParams', initParams: { nodeParams: initNodeParams } });
    }

    handleParamsChanged = (paramData, outputs) => {
        let { dispatch } = this.props;
        dispatch({ type: 'node/updateParams', paramData: paramData, outputs: outputs });
    };

    componentWillMount() {
        let { metaData } = this.props;
        let { preCheckApi, pageDataSource } = _.get(metaData, "config");
        if (pageDataSource) {
            this.loadPageContextParams();
        } else {
            this.setState({
                awaitPageParams: false
            });
        }
        //主要用于页面重入时前置检查
        if (preCheckApi) {
            let urlSearchParams = util.getUrlParams();
            httpClient.get(preCheckApi, { params: urlSearchParams }).then(result => {
                let { defaultParams, msg } = result;
                if (msg) {
                    message.warning(msg, 5);
                }
                if (defaultParams) {
                    let { history, location } = this.props;
                    history.push({
                        pathname: location.pathname,
                        search: Object.assign(urlSearchParams, defaultParams)
                    });
                }
                this.setState({
                    preCheckPass: true
                });
            })
        } else {
            this.setState({
                preCheckPass: true
            });
        }
    }

    loadPageContextParams = () => {
        let { metaData, dispatch, defaultNodeParams, nodeParams } = this.props;
        let { pageDataSource } = _.get(metaData, "config");
        let urlParams = util.getUrlParams();
        pageDataSource = JSON.parse(util.renderTemplateString(JSON.stringify(pageDataSource), { ...nodeParams, ...defaultNodeParams, ...urlParams }));
        let { url, params = {}, method = 'get', mode = 'async' } = pageDataSource, req = null;
        let paramsSet = Object.assign({}, params, urlParams);
        if (mode === 'sync') {
            this.setState({
                awaitPageParams: true
            });
        } else {
            this.setState({
                awaitPageParams: false
            });
        }
        if (method === 'get' || method === 'GET') {
            req = httpClient.get(url, { params: paramsSet });
        } else if (method === 'post' || method === 'POST') {
            req = httpClient.post(url, paramsSet);
        }
        if (req) {
            req.then(data => {
                this.pageParams = data;
                dispatch({ type: 'node/updateParams', paramData: data });
                this.setState({
                    awaitPageParams: false
                });
            })
        }
    };

    render() {
        let { widgets, layout, nodeId, metaData, urlRequireNodeId, ...contentProps } = this.props, { routeData, preCheckPass, awaitPageParams } = this.state;
        if (!widgets || !preCheckPass || awaitPageParams) return null;
        let largeCount = 1, bgFill = false;
        if (layout) {
            let { column, fill } = layout;
            largeCount = parseInt(column || largeCount);
            bgFill = (fill === true || fill === 'true');
        }
        if (largeCount > widgets.length) largeCount = widgets.length;
        let children = (metaData && metaData.children) || [], urlSearchParams = util.getUrlParams(), defaultTab, nowPath = this.props.history.location.pathname;
        if (children.length > 0) {
            let displayTabs = routeData.children.filter(child => child.config && !child.config.hidden);
            if (displayTabs && displayTabs.length) {
                defaultTab = displayTabs[0].path;
            } else {
                defaultTab = routeData.children[0].path;
            }
            routeData.children.forEach(cp => {
                if (nowPath === cp.path || nowPath.indexOf(cp.path) >= 0) defaultTab = cp.path;
            });
        }
        if (urlRequireNodeId === false) {
            delete urlSearchParams.nodeId;
        } else {
            urlSearchParams.nodeId = nodeId;
        }
        let menus = [];
        if (children.length) {
            menus = routeData.children.map(({ path, icon, label, config }) => {
                if (!label) return null;
                if (config && (config.hidden === true || config.hidden === "true")) return null;
                //增加url自定义参数声明
                let { acceptUrlParams = [] } = config, acceptParams = {};
                if (acceptUrlParams.length) {
                    acceptParams = {};
                    acceptUrlParams.forEach(key => {
                        if (urlSearchParams[key]) {
                            acceptParams[key] = urlSearchParams[key];
                        }
                    });
                } else {
                    acceptParams = urlSearchParams;
                }
                if (urlSearchParams.nodeId) {
                    acceptParams.nodeId = urlSearchParams.nodeId;
                }
                if (path) {
                    return (
                        <Menu.Item key={path}>
                            <Link to={{
                                pathname: path,
                                search: util.objectToUrlParams(acceptParams)
                            }}
                            >
                                {icon ? <LegacyIcon type={icon} /> : null}<span>{label}</span>
                            </Link>
                        </Menu.Item>
                    );
                }
            }).filter(r => r !== null)
        }
        return (
            <div className={bgFill ? 'globalBackground' : ''} style={{ padding: bgFill ? 8 : 0 }}>
                {
                    widgets.length > 0 ?
                        <List
                            grid={{ gutter: 8, xs: 1, sm: 1, md: 1, lg: largeCount, xl: largeCount, xxl: largeCount }}
                            dataSource={widgets}
                            key="__components_area"
                            renderItem={item => {
                                return (
                                    <List.Item>
                                        <OamWidget {...contentProps} widget={item} nodeId={nodeId} tabMate={metaData}
                                            handleParamsChanged={this.handleParamsChanged}
                                        />
                                    </List.Item>
                                )
                            }
                            }
                        />
                        : null
                }
                {
                    children.length > 0 ?
                        <div key={"__node_menus_" + nodeId}>
                            {
                                menus.length > 0 ?
                                    <Menu
                                        selectedKeys={[defaultTab]}
                                        mode="horizontal"
                                        className="t-menu"
                                    >
                                        {menus}
                                    </Menu> : null
                            }
                            <div style={{ marginTop: 8 }}>
                                <Switch>
                                    {
                                        routeData.children.map(child => {
                                            return <Route key={child.path} path={child.path} component={child.component} />
                                        })
                                    }
                                    {
                                        defaultTab && <Route path={routeData.path} render={() => <Redirect to={
                                            {
                                                pathname: defaultTab,
                                                search: util.objectToUrlParams(urlSearchParams)
                                            }
                                        }
                                        />} />
                                    }
                                    {
                                        routeData.path.length > nowPath.length ?
                                            <Route path={nowPath} render={() => <Redirect to={
                                                {
                                                    pathname: defaultTab,
                                                    search: util.objectToUrlParams(urlSearchParams)
                                                }
                                            }
                                            />} /> : null
                                    }
                                </Switch>
                            </div>
                        </div>
                        : null
                }
            </div>

        )
    }

    componentWillUnmount() {
        let pageParams = this.pageParams, { dispatch } = this.props;
        if (pageParams) {
            let { nodeParams } = this.props, newNodeParams = {};
            Object.keys(nodeParams).forEach(key => {
                if (!pageParams.hasOwnProperty(key)) {
                    newNodeParams[key] = nodeParams[key]
                }
            });
            dispatch({ type: 'node/resetParamContext' });
            dispatch({ type: 'node/initParams', initParams: { nodeParams: newNodeParams } });
        }
    }
}