/**
 * Created by caoshuaibiao on 2020/2/25.
 * 可通过路径路由进行重入,左侧为menu菜单,右侧为通用的tab
 * 因为是树需要把全部节点的tab页拉取下来用来生成路由,而tab页拉取可能会比较慢,因此把数据缓存一份,每次访问是用的上次的
 */

import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Row, Col, Card, Cascader, Menu, Select, Layout } from 'antd';
import { Route, withRouter, Switch, Redirect, Link } from 'react-router-dom';
import OamContent from './OamContent';
import oamTreeService from '../services/oamTreeService';
import JSXRender from "../../components/JSXRender";
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import httpClient from '../../utils/httpClient';


import '../style.less';

const MenuItemGroup = Menu.ItemGroup;
const SubMenu = Menu.SubMenu;
const { Sider, Content } = Layout;

let panelLabel = "", routeItems = null;
class ContentWithPathPanel extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            menusData: null,
            siderFold: false,
            selectParam: {},
            selectKey: ""
        }
        routeItems = null;
    }

    componentWillMount() {
        let { defaultNodeId } = this.props;
        oamTreeService.getTreeMeta(defaultNodeId).then(mate => {
            //this.initData(defaultNodeId,defaultNodeId,mate);
            this.loadMenus(mate.current.nodeId);
        });

    }


    loadMenus = (nodeId) => {
        let { match } = this.props;
        return oamTreeService.getTreeArchives(nodeId).then(menuData => {
            const procMeuns = function (children, path) {
                children.forEach(c => {
                    c.path = path + `/${c.name}`;
                    if (c.children && c.children.length) {
                        procMeuns(c.children, c.path);
                    }
                });

            };
            let { extensionConfig } = menuData;
            panelLabel = extensionConfig.menuBarHeader || menuData.label;
            //因存在根节点路径与name不对应的问题,因此暂时从子算起不要跟节点,后续统一
            procMeuns(menuData.children, match.path);
            this.genRoute(menuData.children);
            this.setState({
                menusData: menuData.children,
                menuConfig: extensionConfig
            });
        });
    };

    genRoute = (menusData) => {
        //let menusData=menuData.children;
        let { match } = this.props, props = this.props, routes = [];
        let children = (menusData) || [], urlSearchParams = util.getUrlParams(), defaultTab, nowPath = this.props.location.pathname;
        if (children.length > 0) {
            defaultTab = menusData[0].path;
            menusData.forEach(cp => {
                if (nowPath === cp.path || nowPath.indexOf(cp.path) >= 0) defaultTab = cp.path;
            });
        }

        const genRoute = function (child) {
            let { path, children, label } = child;
            if (children && children.length > 0) {
                routes.push(<Route exact path={path} key={"_" + path} component={() => (<Redirect to={children[0].path} />)} />);
            } else {
                routes.push(<Route path={path} key={"_" + path} component={() =>
                    <OamContent {...props}
                        urlRequireNodeId={false}
                        nodeId={child.nodeId} key={child.nodeId + "_"}
                        nodeConfig={child.extensionConfig}
                        menuBarHeader={label}
                    />
                } />);
            }
            if (children && children.length > 0) {
                children.forEach(levelData => {
                    genRoute(levelData);
                });
            }

        };
        children.forEach(moduleData => {
            genRoute(moduleData);
        });

        routeItems = (
            <Switch>
                {routes}
                {
                    defaultTab && <Route path={match.path} render={() => <Redirect to={
                        {
                            pathname: defaultTab,
                            search: util.objectToUrlParams(urlSearchParams)
                        }
                    }
                    />} />
                }
            </Switch>
        );
    }


    filter = (inputValue, path) => {
        return (path.some(option => (option.label).toLowerCase().indexOf(inputValue.toLowerCase()) > -1));
    };

    renderMenuItem = (menuData) => {
        let { menuConfig } = this.state, { acceptUrlParams = [] } = menuConfig, acceptParams = false, urlParams = util.getUrlParams();
        if (acceptUrlParams.length > 0) {
            if (typeof acceptUrlParams === 'string') {
                acceptUrlParams = acceptUrlParams.split(",");
            }
            acceptParams = {};
            acceptUrlParams.forEach(key => {
                if (urlParams.hasOwnProperty(key)) {
                    acceptParams[key] = urlParams[key];
                }
            });
        }
        return menuData.map(({ children, name, icon, label, nodeId, canClick, path, extensionConfig }, key) => {
            if (extensionConfig && extensionConfig.hidden) {
                return null;
            }
            if (canClick) {
                if (children && children.length > 0) {
                    return (
                        <SubMenu key={path} title={<span><LegacyIcon type={icon || 'folder'} /><span>{label}</span></span>}>
                            {this.renderMenuItem(children)}
                        </SubMenu>
                    );
                } else {
                    return (
                        <Menu.Item key={path}>
                            <Link to={{
                                pathname: path,
                                search: acceptParams ? util.objectToUrlParams(acceptParams) : ''
                            }}>
                                <LegacyIcon type={icon || 'deployment-unit'} /><span>{label}</span>
                            </Link>
                        </Menu.Item>
                    );
                }
            } else {
                return (
                    <MenuItemGroup key={nodeId} title={label}>
                        {this.renderMenuItem(children)}
                    </MenuItemGroup>
                )
            }

        });
    };


    onSwitchSidebar = () => {
        let { siderFold } = this.state;
        this.setState({
            siderFold: !siderFold,
        });
    };

    onSelectChange = (value, selectedOptions) => {
        let { selectChanged } = this.props;
        this.genRoute(this.state.menusData);
        this.setState({
            selectParam: [...selectedOptions].pop().selectParam,
            selectKey: [...value].pop()
        });
        selectChanged && selectChanged(selectedOptions);
    };


    render() {
        let { menusData, siderFold } = this.state, { selectDataSource, defaultSelected, match, location } = this.props;
        if (!menusData) return null;
        let panelTitle = <h4><JSXRender jsx={panelLabel} /></h4>, nodeRoute = null;
        if (selectDataSource && selectDataSource.length > 0) {
            panelTitle = (
                <Cascader defaultValue={[defaultSelected || selectDataSource[0].value]}
                    options={selectDataSource} style={{ width: '100%' }}
                    onChange={this.onSelectChange}
                    showSearch={this.filter}
                    allowClear={false}
                    popupClassName="oam-cascader-pop"
                />
            )
        }



        let selectKeys = [];
        const { pathname } = location;

        if (pathname) {
            const getKeys = function (routeData) {
                routeData.forEach(({ path, children }) => {
                    if (pathname.includes(path)) {
                        selectKeys.push(path);
                    }
                    if (children && children.length > 0) {
                        getKeys(children);
                    }
                })
            };
            getKeys(menusData);
        }
        return (
            <Layout className="oam">
                <Sider
                    trigger={null}
                    collapsible
                    collapsed={siderFold}
                    style={{ minHeight: '100vh' }}
                    className="sider-menu"
                >
                    <div style={{ height: 48 }} className="menu-tree-header">
                        <span style={{ display: siderFold ? 'none' : 'block' }}>
                            {panelTitle}
                        </span>
                        <span style={{ marginTop: 4 }}>
                            <a onClick={this.onSwitchSidebar}>
                                <LegacyIcon type={siderFold ? 'menu-unfold' : 'menu-fold'} />
                            </a>
                        </span>
                    </div>
                    <div>
                        <Menu
                            selectedKeys={selectKeys}
                            defaultOpenKeys={siderFold ? [] : selectKeys}
                            mode="inline"
                        >
                            {
                                this.renderMenuItem(menusData)
                            }
                        </Menu>
                    </div>
                </Sider>
                <Layout>
                    <Content style={{ marginLeft: 8 }}>
                        {routeItems}
                    </Content>
                </Layout>
            </Layout>
        );
    }

}

export default withRouter(ContentWithPathPanel)
