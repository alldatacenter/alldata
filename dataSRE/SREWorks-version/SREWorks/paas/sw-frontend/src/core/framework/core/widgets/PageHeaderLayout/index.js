/**
 * Created by caoshuaibiao on 2021/3/10.
 * 带PageHeader的布局组件,支持把组件所在的节点下的子节点生成至pageheader的footer内
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Layout, Avatar, Button, PageHeader, Tabs, Statistic, Descriptions, Menu, Row, Col } from 'antd';
import { Route, Redirect, Link, Switch, withRouter } from 'dva/router';
import NodeContent from "../../NodeContent";
import ToolBar from '../../toolbar';
import JSXRender from '../../JSXRender';
import * as util from "../../../../../utils/utils";

import './index.less';
import _ from "lodash";

let topGroupMapping = null, routeItem = [], pageParams = {};
const getSelectedKey = (routeData, location, selectKeys = []) => {
    const { pathname } = location;
    if (pathname) {
        routeData && routeData.forEach(({ path, children = [], config }) => {
            if (pathname.includes(path)) {
                selectKeys.push(path);
            }
            if (config && (config.hidden === true || config.hidden === "true")) {
                if (config.selectPathInHidden) {
                    selectKeys.push(config.selectPathInHidden);
                }
            }
            routeData.children && getSelectedKey(routeData.children, location, selectKeys);
        })
    }
};

const { Content, Sider } = Layout;
const SubMenu = Menu.SubMenu;

const TopMenus = ({ routeData, location, topMenuType }) => {
    if (!routeData) return <div />;
    if (routeData.children && routeData.children.length > 0) {
        let selectKeys = [];
        getSelectedKey(routeData.children, location, selectKeys);
        //超链形式的菜单
        if (topMenuType === 'link') {
            return (
                <div className="top-link-menu" style={{ marginTop: 8, paddingLeft: 3 }}>
                    {
                        routeData && routeData.children && routeData.children.map(({ path, name, icon, label, config }, key) => {
                            //过滤掉带":"的路径,这些一般为子页面而不是菜单
                            if ((path && path.indexOf(":") > 0) || !label) return null;
                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                            if (path) {
                                return (
                                    <div key={path} className="link-item">
                                        <a style={selectKeys.includes(path) ? { fontWeight: "700" } : {}}><Link to={{
                                            pathname: path,
                                            search: util.objectToUrlParams(pageParams)
                                        }}><span>{label}</span></Link></a>
                                    </div>
                                );
                            }
                        })
                    }
                </div>
            )
        }
        return (
            <div>
                <Menu
                    selectedKeys={selectKeys}
                    mode="horizontal"
                    style={{ borderBottom: "none", minWidth: 480 }}
                >
                    {
                        routeData && routeData.children && routeData.children.map(({ path, name, icon, label, config }, key) => {
                            //过滤掉带":"的路径,这些一般为子页面而不是菜单
                            if ((path && path.indexOf(":") > 0) || !label) return null;
                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                            if (path) {
                                return (
                                    <Menu.Item key={path}>
                                        <Link to={{
                                            pathname: path,
                                            search: util.objectToUrlParams(pageParams)
                                        }}><span>{label}</span></Link>
                                    </Menu.Item>
                                );
                            }
                        })
                    }
                </Menu>
            </div>
        )
    }
    return <div />;
};

const LeftMenus = ({ routeData, location }) => {
    if (!routeData) return <div />;
    if (routeData.children && routeData.children.length > 0) {
        let selectKeys = [];
        getSelectedKey(routeData.children, location, selectKeys);
        return (
            <Menu mode="inline" selectedKeys={selectKeys} defaultOpenKeys={selectKeys} style={{ borderRight: "none" }}>
                {
                    routeData && routeData.children && routeData.children.map(({ path, children = [], name, icon, component, label, layout, config }, key) => {
                        if (config && (config.hidden === true || config.hidden === "true")) {
                            return null;
                        }
                        if ((!children || children.length === 0) || layout === 'top') {
                            return (
                                <Menu.Item key={path} >
                                    <Link to={{
                                        pathname: path,
                                        search: util.objectToUrlParams(pageParams)
                                    }}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
                                </Menu.Item>
                            );
                        }

                        if (children && children.length > 0) {
                            return (
                                <SubMenu key={path} title={<span><LegacyIcon type={icon || 'folder'} /><span>{label}</span></span>}>
                                    {
                                        children && children.map(({ path, children = [], name, icon, component, label, config }, key) => {
                                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                                            if (path) {
                                                return (
                                                    <Menu.Item key={path}>
                                                        <Link to={{
                                                            pathname: path,
                                                            search: util.objectToUrlParams(pageParams)
                                                        }}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
                                                    </Menu.Item>
                                                );
                                            }
                                        })
                                    }
                                </SubMenu>
                            );
                        }


                    })
                }
            </Menu>
        );
    }
    return <div />;
};


class PageHeaderLayout extends React.Component {

    constructor(props) {
        super(props);
        pageParams = util.getUrlParams();
        const { routeData, widgetConfig, userParams } = props;
        let newUserParams = Object.assign({}, userParams, util.getUrlParams());
        topGroupMapping = {};
        routeItem = [];
        const genRoute = function (routeData) {
            if (!routeData) {
                return
            }
            let { path, exact, type, children = [], layout } = routeData;
            let displayChildren = children.filter(child => child.config && !child.config.hidden), defaultChildren;
            if (displayChildren && displayChildren.length) {
                defaultChildren = displayChildren[0];
            } else {
                defaultChildren = children[0] || {};
            }
            if (children && children.length > 0) {
                if (layout === 'custom') {
                    path && routeItem.push(<Route path={path} key={"_" + path} component={() => <NodeContent {...props}
                        userParams={newUserParams}
                        routeData={routeData}
                        nodeData={routeData}
                        nodeId={routeData.nodeId || ''} />} />);
                    return;
                } else {
                    path && defaultChildren.path && routeItem.push(<Route exact path={path} key={"_" + path} component={() => (<Redirect to={defaultChildren.path + '?' + util.objectToUrlParams(pageParams)} />)} />);
                }
            } else {
                path && routeItem.push(<Route path={path} key={path} component={() =>
                    <NodeContent {...props}
                        userParams={newUserParams}
                        routeData={routeData}
                        nodeData={routeData}
                        nodeId={routeData.nodeId || ''}
                    />} />);
            }
            if (children && children.length > 0) {
                topGroupMapping[path] = routeData;
                children.forEach(levelData => {
                    genRoute(levelData);
                });
            }

        };
        routeData && routeData.children && routeData.children.forEach(moduleData => {
            genRoute(moduleData);
        });
    }


    render() {
        const { routeData, location, widgetConfig, widgetData = {} } = this.props;
        const { title, logoUrl, description, descriptions = [], paddingInner = 0, statistics, height, contentPadding, width, topMenuType = 'menu' } = widgetConfig, { pathname } = location;
        let leftSiderMenu = null, toolbarItem = null;
        let leftKey = Object.keys(topGroupMapping).filter(topKey => pathname.startsWith(topKey))[0];
        if (leftKey) {
            leftSiderMenu = <LeftMenus {...this.props} routeData={topGroupMapping[leftKey]} />
        }
        if (widgetConfig.toolbar && Object.keys(widgetConfig.toolbar).length > 0) {
            toolbarItem = <ToolBar {...this.props} widgetConfig={widgetConfig} />
        }
        if (!routeData) {
            return <div></div>
        }
        let { pageLayoutType = 'FLUID' } = this.props;
        return (
            <div className="widget-layout-page-content" key={routeData.nodeId || ''}>
                <PageHeader
                    className={`common-border brief-page-content globalBackground ${title ? "" : "brief-header-no-title"}`}
                    style={{
                        paddingTop: title ? 32 : 0, zIndex: 1,
                        position: "relative", marginTop: pageLayoutType === 'FLUID' ? '-12px' : '-20px', marginLeft: '-15.4vw', marginRight: '-15.4vw', paddingLeft: '15.3vw', paddingRight: '15.3vw'
                    }}
                    title={
                        title &&
                        <div className="brief-header-title">
                            <div style={{ width: 48, minHeight: 48, alignSelf: "baseline" }}>
                                {
                                    logoUrl ? <Avatar src={logoUrl} style={{ fontSize: 26, width: 48, height: 48, lineHeight: "48px", backgroundColor: 'white', verticalAlign: 'middle' }} size="large" /> : <Avatar style={{ fontSize: 26, width: 48, height: 48, lineHeight: "48px", backgroundColor: 'rgb(35, 91, 157)', verticalAlign: 'middle' }} size="large">{title[0]}</Avatar>
                                }
                            </div>
                            <div style={{ flex: 1, display: "grid", marginLeft: 14 }}>
                                <h2 style={{ fontSize: 24 }}><JSXRender jsx={title} /></h2>
                                {description && <p className="ant-page-header-heading-sub-title" style={{ fontWeight: "normal" }}><JSXRender jsx={description} /></p>}
                            </div>
                        </div>
                    }
                    extra={<div>
                        <div>{toolbarItem}</div>
                        {
                            statistics && !!statistics.length &&
                            <Row gutter={16}>
                                {
                                    widgetData && statistics.map(item => {
                                        return <Col>
                                            <Statistic title={item.label} suffix={item.unit} value={_.get(item.dataIndex, widgetData)} />
                                        </Col>
                                    })
                                }
                            </Row>
                        }
                    </div>}
                    footer={routeData &&
                        <TopMenus {...this.props} routeData={routeData} topMenuType={topMenuType} />
                    }
                >
                    {
                        descriptions.length > 0 &&
                        <Descriptions size="small" column={3}>
                            {
                                descriptions.map(dItem => {
                                    return <Descriptions.Item label={dItem.label}>{dItem.value}</Descriptions.Item>
                                })
                            }
                        </Descriptions>
                    }
                </PageHeader>
                <Layout className="brief-page-content" style={{ overflow: 'auto', paddingLeft: paddingInner + 'vw', paddingRight: paddingInner + 'vw',background:'transparent' }}>
                    {
                        leftSiderMenu &&
                        <Sider style={{ marginRight: 8 }}>
                            {leftSiderMenu}
                        </Sider>
                    }
                    <Content style={{ margin: '-8px 0px 0' }}>
                        {routeItem}
                    </Content>
                </Layout>
            </div>
        );
    }
}

export default withRouter(PageHeaderLayout)