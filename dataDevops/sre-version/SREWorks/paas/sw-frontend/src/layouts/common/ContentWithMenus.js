/**
 * Created by caoshuaibiao on 2018/10/27.
 * 内容区存在顶部菜单组件
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Menu, Divider } from 'antd';
import { Route, Redirect, Link } from 'dva/router';
import dynamic from 'dva/dynamic';
import OamContent from '../../core/framework/OamContent';
import * as util from "../../utils/utils";
import NodeContent from "../../core/framework/core/NodeContent";
import JSXRender from "../../components/JSXRender";
import qs from 'query-string'

let contentRoutes = null, meunItems = null;
let fromAppKeyParams = {};
class ContentWithMenus extends React.Component {

    constructor(props) {
        super(props);
    }

    componentWillMount() {
        let queryString = window.location.hash.split('?');
        let hashArr = window.location.hash.split('/');
        if (queryString.length && hashArr.includes('appdev')) {
            fromAppKeyParams = qs.parse(queryString[1])
        }
        const { routeData, urlSearchParams, isV2, ...otherProps } = this.props;
        contentRoutes = (
            <div>
                {
                    routeData.children.map(({ path, children, component, type, sync, nodeId, ...dynamics }, key) => {
                        let childrenNode = routeData.children[key];
                        if (path && component) {
                            return (
                                <Route
                                    key={"_" + path}
                                    exact
                                    path={path}
                                    component={sync ? component : dynamic({
                                        component,
                                        ...dynamics,
                                    })}
                                />
                            )

                        }
                        if (isV2) {
                            return <Route Route path={path} key={path} component={() => <NodeContent {...this.props} routeData={childrenNode} nodeData={childrenNode}
                                nodeId={nodeId} nodeConfig={childrenNode.config} />} />;
                        } else {
                            if (type === 'custom') {
                                return <Route Route path={path} key={path} component={() => <OamContent {...this.props} menuRouteData={routeData.children[key]} nodeId={nodeId} urlRequireNodeId={false} nodeConfig={routeData.children[key].config} />} />;
                            }
                        }
                    })
                }
                {
                    routeData.children && routeData.children[0] ? <Route exact path={routeData.path} render={() => (<Redirect to={routeData.children[0].path} />)} /> : null
                }
            </div>
        );
        meunItems = (
            routeData.children.map(({ path, children, name, icon, component, label, config = {} }) => {
                if (!label) return null;
                let { hidden, redirect } = config;
                if (config && (hidden === true || hidden === "true")) return null;
                if (path) {
                    return (
                        <Menu.Item key={path}>
                            {
                                redirect && redirect.path ?
                                    <RedirectLink redirect={redirect} label={label} icon={icon} />
                                    :
                                    <Link to={{
                                        pathname: path,
                                        search: util.objectToUrlParams(Object.assign(urlSearchParams || {}, fromAppKeyParams))
                                        // search:util.objectToUrlParams(urlSearchParams||{})
                                    }}
                                    >
                                        {icon ? <LegacyIcon type={icon} /> : null}<span>{label}</span>
                                    </Link>
                            }
                        </Menu.Item>
                    );
                }
            }).filter(r => r !== null)
        );
    }
    render() {
        let { routeData } = this.props, lp = this.props.history.location.pathname, selectKeys = [];
        routeData.children.forEach(c => {
            if (lp.indexOf(c.path) >= 0) {
                selectKeys.push(c.path);
            }
        });
        selectKeys.push(lp)
        let { menuBarHeader } = routeData;
        let disPlayMenuBarHeader = menuBarHeader || routeData.label;
        if (menuBarHeader && menuBarHeader.indexOf && menuBarHeader.indexOf("$") > -1) {
            disPlayMenuBarHeader = <JSXRender jsx={util.renderTemplateString(menuBarHeader, Object.assign({}, util.getUrlParams()))} />;
        }
        return (
            <div>
                {
                    meunItems.length ?
                        <div className="content-top-menu-bar globalBackground" style={{ display: 'flex', marginBottom: 8 }}>
                            {
                                menuBarHeader ?
                                    <div style={{ display: 'flex', marginLeft: 12, alignItems: 'center' }} key="__node_title">
                                        <b style={{ fontSize: 16 }}>{disPlayMenuBarHeader}</b>
                                    </div>
                                    : null
                            }
                            {
                                menuBarHeader ?
                                    <div style={{ display: 'flex', alignItems: 'center' }} key="__menu_divider">
                                        <Divider type="vertical" style={{ height: 48, width: 1 }} />
                                    </div>
                                    : null
                            }
                            <Menu
                                style={{ minWidth: 200 }}
                                selectedKeys={selectKeys}
                                mode="horizontal"
                            >
                                {meunItems}
                            </Menu>
                        </div>
                        : null

                }
                {contentRoutes}
            </div>

        )

    }
}

const RedirectLink = ({ redirect, icon, label }) => {

    let redirectPath = redirect.path;
    if (redirect.path.startsWith("http")) {
        return <span onClick={() => window.open(redirect.path)}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;
    }
    if (redirect.type === 'local') {
        let { origin } = window.location;
        redirectPath = origin + redirect.path;
        return <span onClick={() => window.open(redirectPath)}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;;
    }
    return (
        <Link to={{
            pathname: redirectPath,
            search: ''
        }}
        >
            {icon ? <LegacyIcon type={icon} /> : null}<span>{label}</span>
        </Link>
    );
};


export default ContentWithMenus;

