/**
 * Created by caoshuaibiao on 2018/10/27.
 * 左侧可收缩菜单,按约定只支持两级,第三级在ContentMenus中
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Menu } from 'antd';
import { Link } from 'dva/router';
import properties from 'appRoot/properties';
import * as util from "../../utils/utils";

const SubMenu = Menu.SubMenu;

const LeftCategoryMenus = ({ routes, location, currentModule, siderFold }) => {
    //根据routes列表中的category字段生成新的菜单树
    let selectKeys = [], menuTreeData = routes, categoryMapping = {}, noCategory = [];
    //菜单顺序调整,临时方案
    routes.forEach((r, index) => r.seq = r.seq || index);
    routes.sort(function (a, b) { return a.seq - b.seq });
    const { pathname } = location;
    if (pathname) {
        const getKeys = function (routeData) {
            routeData.forEach((r, i) => {
                let { path, children, category } = r;
                if (category) {
                    categoryMapping[category] = categoryMapping[category] || [];
                    categoryMapping[category].push(r);
                } else {
                    noCategory.push(r);
                }
                if (pathname.includes(path + "/") || pathname.endsWith(path)) {
                    selectKeys.push(path);
                    if (category) { selectKeys.push(category) }
                }
                if (children && children.length > 0) {
                    getKeys(children);
                }
            })
        };
        getKeys(routes);
    }
    //生成新的菜单树数据
    let ckeys = Object.keys(categoryMapping);
    if (ckeys.length > 0) {
        menuTreeData = [];
        ckeys.forEach(k => {
            menuTreeData.push({
                path: k,
                children: categoryMapping[k],
                label: k,
                name: k
            })
        });
        menuTreeData.push(...noCategory);
    }
    return (
        <Menu mode="inline" selectedKeys={selectKeys} defaultOpenKeys={siderFold ? [] : selectKeys}>
            {
                menuTreeData.map(({ path, children, name, icon, component, label, layout, config }, key) => {
                    //过滤掉带":"的路径,这些一般为子页面而不是菜单
                    if (!label) return null;
                    //TODO 3.8暂时隐藏掉通道服务
                    if ((properties.envFlag === properties.ENV.DXZ || properties.envFlag === properties.ENV.ApsaraStack || properties.envFlag === properties.ENV.RQY) && name === 'tunnel') {
                        return null;
                    }
                    if (config && (config.hidden === true || config.hidden === "true")) return null;
                    if ((!children || children.length === 0) || layout === 'top') {
                        return (
                            <Menu.Item key={path} >
                                <Link to={path}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
                            </Menu.Item>
                        );
                    }
                    if (children && children.length > 0) {
                        return (
                            <SubMenu key={path} title={<span><LegacyIcon type={icon || 'folder'} /><span>{label}</span></span>}>
                                {
                                    children.map(({ path, children, name, icon, component, label, config }, key) => {
                                        //过滤掉带":"的路径,这些一般为子页面而不是菜单
                                        if (!label) return null;
                                        if (config && (config.hidden === true || config.hidden === "true")) return null;
                                        if (path) {
                                            return (
                                                <Menu.Item key={path}>
                                                    <Link to={path}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
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
};

const LeftMenus = ({ routes, location, currentModule, siderFold }) => {
    //对内的管理模块启用服务分组菜单,暂时写死管理模块,后续如果需要可以实现可配置
    if (currentModule && currentModule.name === 'manage' && currentModule.type === 'buildIn' && properties.envFlag === properties.ENV.Internal) {
        return <LeftCategoryMenus routes={routes} location={location} currentModule={currentModule} siderFold={siderFold} />
    }
    let selectKeys = [];
    const { pathname } = location;
    if (pathname) {
        const getKeys = function (routeData) {
            routeData.forEach(({ path, children, config }) => {
                if (pathname.includes(path + "/") || pathname.endsWith(path)) {
                    selectKeys.push(path);
                    if (config && (config.hidden === true || config.hidden === "true")) {
                        if (config.selectPathInHidden) {
                            selectKeys.push(config.selectPathInHidden);
                        }
                    }
                }
                if (children && children.length > 0) {
                    getKeys(children);
                }
            })
        };
        getKeys(routes);
    }
    return (
        <Menu mode="inline" selectedKeys={selectKeys} defaultOpenKeys={siderFold ? [] : selectKeys}>
            {
                routes.map(({ path, children, name, icon, component, label, layout, config }, key) => {
                    //过滤掉带":"的路径,这些一般为子页面而不是菜单
                    if (!label) return null;
                    //TODO 3.8暂时隐藏掉通道服务
                    if ((properties.envFlag === properties.ENV.DXZ || properties.envFlag === properties.ENV.ApsaraStack || properties.envFlag === properties.ENV.RQY) && name === 'tunnel') {
                        return null;
                    }
                    if (config && (config.hidden === true || config.hidden === "true")) {
                        return null;
                    }
                    let { acceptUrlParams = [] } = config, acceptParams = false, urlParams = util.getUrlParams();
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
                    if ((!children || children.length === 0) || layout === 'top' || layout === 'custom') {
                        return (
                            <Menu.Item key={path} >
                                {
                                    acceptParams && Object.keys(acceptParams).length > 0 ?
                                        <Link to={{
                                            pathname: path,
                                            search: util.objectToUrlParams(acceptParams)
                                        }}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link> :
                                        <Link to={path}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
                                }
                            </Menu.Item>
                        );
                    }
                    if (children && children.length > 0) {
                        return (
                            <SubMenu key={path} title={<span><LegacyIcon type={icon || 'folder'} /><span>{label}</span></span>}>
                                {
                                    children.map(({ path, children, name, icon, component, label, config }, key) => {
                                        //过滤掉带":"的路径,这些一般为子页面而不是菜单
                                        if (!label) return null;
                                        if (config && (config.hidden === true || config.hidden === "true")) return null;
                                        if (path) {
                                            return (
                                                <Menu.Item key={path}>
                                                    {
                                                        acceptParams && Object.keys(acceptParams).length > 0 ?
                                                            <Link to={{
                                                                pathname: path,
                                                                search: util.objectToUrlParams(acceptParams)
                                                            }}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link> :
                                                            <Link to={path}><LegacyIcon type={icon || 'file'} /><span>{label}</span></Link>
                                                    }
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
};

LeftMenus.propTypes = {
    onMenuOpenChange: PropTypes.func,
    onSwitchTheme: PropTypes.func,
    theme: PropTypes.string,
    menuMode: PropTypes.string,
    routes: PropTypes.array,
    currentModule: PropTypes.object,
    siderOpenKeys: PropTypes.array
};

export default LeftMenus;

