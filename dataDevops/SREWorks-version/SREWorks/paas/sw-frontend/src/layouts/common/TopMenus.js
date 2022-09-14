/**
 * Created by caoshuaibiao on 2018/11/22.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Menu } from 'antd';
import { Link } from 'dva/router';

const TopMenus = ({ location, currentModule }) => {
    if (!currentModule) return <div />;
    if (currentModule.layout === 'top-left' && currentModule.children && currentModule.children.length > 0) {
        let selectKeys = [];
        const { pathname } = location;
        if (pathname) {
            const getKeys = function (routeData) {
                routeData.forEach(({ path, children, config }) => {
                    if (pathname.includes(path)) {
                        selectKeys.push(path);
                    }
                    if (config && (config.hidden === true || config.hidden === "true")) {
                        if (config.selectPathInHidden) {
                            selectKeys.push(config.selectPathInHidden);
                        }
                    }
                    if (children && children.length > 0) {
                        getKeys(children);
                    }
                })
            };
            getKeys(currentModule.children);
        }
        return (
            <div className="t-top-menus">
                <Menu
                    selectedKeys={selectKeys}
                    mode="horizontal"
                    className="t-menu"
                >
                    {
                        (currentModule.children || []).map(({ path, children, name, icon, component, label, config }, key) => {
                            //过滤掉带":"的路径,这些一般为子页面而不是菜单
                            if ((path && path.indexOf(":") > 0) || !label) return null;
                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                            if (path) {
                                return (
                                    <Menu.Item key={path}>
                                        <Link to={path}><span>{label}</span></Link>
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

TopMenus.propTypes = {
    onMenuOpenChange: PropTypes.func,
    onSwitchTheme: PropTypes.func,
    theme: PropTypes.string,
    menuMode: PropTypes.string,
    routes: PropTypes.array,
    currentModule: PropTypes.object,
    siderOpenKeys: PropTypes.array
};

export default TopMenus;