/**
 * Created by caoshuaibiao on 2018/8/15.
 * 顶部多模块头
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'dva/router';
import properties from '../properties';
import {
    AppstoreOutlined,
    DownOutlined,
    LogoutOutlined,
    QuestionCircleOutlined,
    UserOutlined,
} from '@ant-design/icons';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import {
    Layout,
    Menu,
    Avatar,
    Switch,
    Dropdown,
    List,
    Card,
    Tooltip,
    Divider,
    Badge,
    Drawer,
    Radio,
} from 'antd';
import localeHelper from '../utils/localeHelper';
import cacheRepository from '../utils/cacheRepository';
import SiderNavToggleBar from "../components/SiderNavToggleBar";
import * as util from "./../utils/utils"
import DropDownUser from "./common/DropDownUser";
import SearchBar from './SearchBar';
const { Header } = Layout;
const SubMenu = Menu.SubMenu;
const Item = Menu.Item;
const MenuItemGroup = Menu.ItemGroup;

const DefaultHeader = ({ siderFold, siderRespons, theme, siderOpenKeys, menuResponsVisible, moduleData, onSwitchSidebar,
    onSwitchTheme, onSwitchMenuMode, onRoleChange, onLanguageChange, onModuleMenuChang, currentModule, language,
    currentUser, moduleGroups, onEnvChang, onLogout, settings, currentProduct, envs }) => {
    const roles = currentUser.roles, cacheRole = cacheRepository.getRole(currentProduct.productId);
    const currentRole = roles.filter(role => role.roleId === cacheRole)[0];

    const TopBar = () => (
        <div style={{ display: "flex", marginTop: 3 }}>
            {
                currentProduct.isHasSearch ?
                    <div style={{ position: 'relative', top: -2, marginRight: 6 }}>
                        <SearchBar global={{ currentUser: currentUser }}></SearchBar>
                    </div> : null
            }
            {
                currentProduct.docsUrl ?
                    <div>
                        <a href={currentProduct.docsUrl} target="_blank"><Tooltip title={localeHelper.get('MainMenuDocumentation', '帮助文档')}><QuestionCircleOutlined style={{ fontSize: 18 }} /> </Tooltip></a>
                    </div> : null
            }
            {
                currentProduct.isHasNotify ?
                    <div className="rightMenu">

                    </div> : null
            }
        </div>
    );
    /**
     * 大数据管家用户图标下拉菜单
     * @constructor
     */
    const UserMenu = () => (
        <div style={{ display: "flex", alignItems: "center" }}>
            <Dropdown placement="bottomRight" overlay={
                <Menu mode="horizontal" style={{ width: 240 }}>
                    <MenuItemGroup title={localeHelper.get("common.theme.setting", "主题设置")}>
                        <Menu.Item key="theme">
                            <Switch onChange={onSwitchTheme}
                                defaultChecked={localStorage.getItem("sreworks-theme") === "light"}
                                size="small" checkedChildren="亮"
                                unCheckedChildren="暗" />
                        </Menu.Item>
                    </MenuItemGroup>
                    <MenuItemGroup title={localeHelper.get("common.language.setting", "语言设置")}>
                        <Menu.Item key="language">
                            <Switch onChange={onLanguageChange} checked={localStorage.getItem("t_lang_locale") === 'zh_CN'} size="small" checkedChildren={localeHelper.get("common.language.zh_CN", "中文")} unCheckedChildren={localeHelper.get("common.language.en_US", "英文")} />
                        </Menu.Item>
                    </MenuItemGroup>
                    <MenuItemGroup title={localeHelper.get("common.switchEnv", "环境切换")}>
                        <Menu.Item key="switchEnv" style={{ marginBottom: 12 }}>
                            <div onClick={(e) => e.stopPropagation()}>
                                <Radio.Group defaultValue={util.getNewBizApp() && util.getNewBizApp().split(",")[2]}
                                    buttonStyle="solid" onChange={(e) => onEnvChang(e.target.value)}>
                                    {
                                        envs.map(env => <Radio.Button value={env.value}
                                            key={env.value}>{env.label}</Radio.Button>)
                                    }
                                </Radio.Group>
                            </div>
                        </Menu.Item>
                    </MenuItemGroup>
                    {currentUser && currentUser.roles && !!currentUser.roles.length && <MenuItemGroup title={localeHelper.get("common.switchRole", "角色切换")}>
                        <Menu.Item key="switchRole" style={{ marginBottom: 12 }}>
                            <div onClick={(e) => e.stopPropagation()}>
                                <Radio.Group defaultValue={cacheRole} buttonStyle="solid"
                                    onChange={(e) => onRoleChange(e.target.value)}>
                                    {
                                        currentUser.roles.map(role => <Radio.Button value={role.roleId}
                                            key={role.roleId}>{role.roleName}</Radio.Button>)
                                    }
                                </Radio.Group>

                            </div>
                        </Menu.Item>
                    </MenuItemGroup>}
                    <MenuItemGroup>
                        <Menu.Item key='logout'>
                            <a
                                onClick={onLogout}><LogoutOutlined /><span>{localeHelper.get("MainMenulogout", "登出")}</span></a>
                        </Menu.Item>
                    </MenuItemGroup>
                </Menu>
            }>
                {/* <a>
                    <UserTitle />
                </a> */}
            </Dropdown>
        </div>
    );


    const SiteLogo = () => {
        let { platformName, platformLogo } = properties
        if(process.env.NODE_ENV === 'local'){
            platformLogo = properties.baseUrl + platformLogo
        }
        const logo = (
            <div className="left-logo">
                <div>
                    <SiderNavToggleBar theme="dark" top={48} />
                </div>
                <div>
                    <span className="logo-text">
                        <span className="sre-work-link" onClick={() => window.open("#/desktop/")}>
                            <img style={{ width: 24, marginLeft: 10, marginRight: 10, marginTop: -3 }} onClick={() => { window.open("/#", "_blank") }} src={platformLogo} />
                            <span onClick={() => { window.open("/#", "_blank") }}>{platformName}</span>
                        </span>
                        <Divider type="vertical" style={{ background: '#fff', width: 2, height: 20, marginLeft: 12, marginRight: 12 }} />
                        <span>
                            {currentProduct.productName}
                        </span>
                    </span>
                </div>
            </div>
        );

        return logo;
    };


    const ModuleMenu = () => {
        let keys = [currentModule.name];
        // let keys=[];
        //顶部模块级别的菜单隐藏选中方式
        let { selectPathInHidden, hidden } = currentModule.config || {};
        if (hidden && selectPathInHidden) {
            if (window.location.hash.indexOf(currentModule.path + "/") > -1 || window.location.hash.endsWith(currentModule.path)) {
                keys.push(selectPathInHidden);
            }
        } else {
            keys.push(currentModule.path);
        }
        //需要对弹出菜单做选中样式适配
        if (currentModule.layout === 'popup' && currentModule.children && currentModule.children.length) {
            keys.push(currentModule.path);
            let cPath = window.location.hash;
            currentModule.children.forEach(c => {
                if (cPath.indexOf(c.path) >= 0) {
                    keys.push(c.path);
                }
            })
        }
        return (
            <Menu mode="horizontal"
                id="abm-menu"
                defaultSelectedKeys={keys}
                selectedKeys={keys}
            >
                {
                    moduleData.filter(md => (md.group === currentModule.group || !md.group) && (md.config.hidden !== true && md.config.hidden !== "true")).map(m => {
                        if (m.layout === 'popup' && m.children && m.children.length) {
                            return (
                                <SubMenu
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)' }}>{m.icon ? <LegacyIcon type={m.icon} /> : null}{m.label}<DownOutlined style={{ fontSize: 12, marginRight: 0, transform: 'scale(0.8)' }} /></span>}
                                    key={m.path}
                                >
                                    {
                                        m.children.map((child, index) => {
                                            let { config } = child, cItems = [];
                                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                                            let { redirect } = config || {};
                                            if (redirect && redirect.path) {
                                                cItems.push(
                                                    <Item key={child.path}>
                                                        <RedirectLink redirect={redirect} label={child.label} icon={child.icon} />
                                                    </Item>
                                                );
                                            } else {
                                                cItems.push(<Item key={child.path}><Link to={child.path} /><span>{child.icon ? <LegacyIcon type={child.icon} /> : null}{child.label}</span></Item>);
                                            }
                                            if ((m.children.length - 1) > index) {
                                                cItems.push(<Menu.Divider key={child.name + "_divider"} />);
                                            }
                                            return cItems;
                                        })
                                    }
                                </SubMenu>
                            );
                        }
                        let { redirect } = m.config || {};
                        if (redirect && redirect.path) {
                            return (
                                <Item key={m.name}>
                                    <RedirectLink redirect={redirect} label={m.label} icon={m.icon} />
                                </Item>
                            );
                        }
                        return <Item key={m.name}><Link to={m.path} /><span style={{ color: 'rgba(255,255,255,0.8)' }}>{m.icon ? <LegacyIcon type={m.icon} /> : null}{m.label}</span></Item>;
                    }
                    )
                }
            </Menu>
        );
    };



    return (
        <div className="leftHeader">
            <div className="left">
                <SiteLogo />
            </div>
            <div className="center">
                <div style={{ minWidth: 200 }}>
                    <ModuleMenu />
                </div>
            </div>
            <div className="right">
                <TopBar />
                <DropDownUser />
            </div>

        </div>
    )
};

const RedirectLink = ({ redirect, icon, label }) => {

    let redirectPath = redirect.path;
    if (redirect.path.startsWith("http")) {
        return <span onClick={() => window.open(redirect.path)} style={{ color: 'rgba(255,255,255,0.8)' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;
    }
    if (redirect.type === 'local') {
        let { origin } = window.location;
        redirectPath = origin + redirect.path;
        return <span onClick={() => window.open(redirectPath)} style={{ color: 'rgba(255,255,255,0.8)' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;;
    }
    return (
        <Link to={{
            pathname: redirectPath,
            search: ''
        }}
        >
            <span style={{ color: 'rgba(255,255,255,0.8)' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>
        </Link>
    );
};

DefaultHeader.prototype = {
    siderFold: PropTypes.bool,
    siderRespons: PropTypes.bool,
    theme: PropTypes.string,
    siderOpenKeys: PropTypes.array,
    menuResponsVisible: PropTypes.bool,
    currentModule: PropTypes.object,
    onSwitchSidebar: PropTypes.func,
    onSwitchTheme: PropTypes.func,
    onSwitchMenuMode: PropTypes.func,
    onRoleChange: PropTypes.func,
    onLanguageChange: PropTypes.func,
    onModuleMenuChang: PropTypes.func,
    language: PropTypes.string,
    currentUser: PropTypes.object,
    moduleGroups: PropTypes.array,
    onLogout: PropTypes.func,
    settings: PropTypes.object,
    currentProduct: PropTypes.object,
};

export default DefaultHeader;
