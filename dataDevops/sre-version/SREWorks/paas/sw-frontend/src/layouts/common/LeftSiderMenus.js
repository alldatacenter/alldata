/**
 * Created by caoshuaibiao on 2018/11/22.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Menu, Layout } from 'antd';
import LeftMenus from './LeftMenus';
import JSXRender from "../../components/JSXRender";

const SubMenu = Menu.SubMenu;
const { Content, Sider } = Layout;


const LeftSiderMenus = ({ routes, location, currentModule, siderFold, onSwitchSidebar }) => {
    if (!currentModule || (currentModule && currentModule.config && currentModule.config.hidden)) return <div />;
    let leftMenusRoutes = [], siderLabel = "", { layout } = currentModule;
    if (layout === 'custom') return <div />;
    if (layout === 'top-left' || layout === 'popup') {
        //获取当前顶部菜单模块下的子菜单传递给LeftMenus
        const { pathname } = location, childrenModules = currentModule.children || [];
        for (let t = 0; t < childrenModules.length; t++) {
            if (pathname.indexOf(childrenModules[t].path) > -1) {
                let cm = childrenModules[t];
                leftMenusRoutes = cm.children || [];
                if (cm.label.endsWith("运维")) {
                    siderLabel = cm.label.substring(0, cm.label.indexOf("运维"));
                }
                break;
            }
        }
    } else {
        //直接把模块下的子传递过去
        leftMenusRoutes = currentModule.children || [];
    }
    const menuProps = { routes: leftMenusRoutes, location, currentModule, siderFold, onSwitchSidebar };
    if (leftMenusRoutes.length > 0 && leftMenusRoutes.filter(lm => lm && lm.config && !lm.config.hidden).length > 0 && layout === 'left') {
        return (
            <Sider
                trigger={null}
                collapsible
                collapsed={siderFold}
                className="left-menus-panel"
            >
                <div style={{ height: 48 }} className="menu-tree-header">
                    <span style={{ display: siderFold ? 'none' : 'block' }}>
                        <h4><JSXRender jsx={siderLabel || currentModule.label} /></h4>
                    </span>
                    <span>
                        <a onClick={onSwitchSidebar}>
                            <LegacyIcon type={siderFold ? 'menu-unfold' : 'menu-fold'} />
                        </a>
                    </span>
                </div>
                <LeftMenus {...menuProps} />
            </Sider>
        );
    }
    return <div />;

};

LeftSiderMenus.propTypes = {
    onMenuOpenChange: PropTypes.func,
    onSwitchTheme: PropTypes.func,
    theme: PropTypes.string,
    menuMode: PropTypes.string,
    routes: PropTypes.array,
    currentModule: PropTypes.object,
    siderFold: PropTypes.bool,
    onSwitchSidebar: PropTypes.func,
    siderOpenKeys: PropTypes.array
};

export default LeftSiderMenus;