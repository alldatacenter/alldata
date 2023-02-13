/**
 * Created by caoshuaibiao on 2021/2/25.
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { connect } from 'dva';
import { DownOutlined, LogoutOutlined, UserOutlined, QuestionCircleOutlined } from '@ant-design/icons';
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
import { Link } from 'dva/router';
import localeHelper from '../../utils/localeHelper';
import properties from 'appRoot/properties';
import './index.less';
import DropDownUser from "../common/DropDownUser";
import SearchBar from '../SearchBar';
const SubMenu = Menu.SubMenu;
const Item = Menu.Item;
const MenuItemGroup = Menu.ItemGroup;

const ModuleMenu = ({ currentModule, moduleData }) => {
    // let keys=[currentModule.name];
    // //需要对弹出菜单做选中样式适配
    // if(currentModule.layout==='popup'&&currentModule.children&&currentModule.children.length){
    //     keys.push(currentModule.path);
    //     let cPath=window.location.hash;
    //     currentModule.children.forEach(c=>{
    //         if(cPath.indexOf(c.path)>=0){
    //             keys.push(c.path);
    //         }
    //     })
    // }
    let keys = [currentModule.name];
    //顶部模块级别的菜单隐藏选中方式
    let { selectPathInHidden, hidden } = currentModule.config || {};
    if (hidden && selectPathInHidden) {
        if (window.location.hash.indexOf(currentModule.path + "/") > -1 || window.location.hash.endsWith(currentModule.path)) {
            keys.push(selectPathInHidden);
        }
    } else {
        keys.push(currentModule.path);
    }
    return (
        <div>
            <Menu
                mode="horizontal"
                defaultSelectedKeys={keys}
                selectedKeys={keys}
                style={{ borderBottom: "none", lineHeight: "32px" }}
            >
                {
                    moduleData.filter(md => (md.group === currentModule.group || !md.group) && (md.config.hidden !== true && md.config.hidden !== "true")).map(m => {
                        if (m.layout === 'popup' && m.children && m.children.length) {
                            return (
                                <SubMenu
                                    title={<span>{m.icon ? <LegacyIcon type={m.icon} /> : null}{m.label}<DownOutlined style={{ fontSize: 12, marginRight: 0, transform: 'scale(0.8)' }} /></span>}
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
                        return <Item key={m.name}><Link to={m.path} /><span>{m.icon ? <LegacyIcon type={m.icon} /> : null}{m.label}</span></Item>;
                    }
                    )
                }
            </Menu>
        </div>
    );
};

const RedirectLink = ({ redirect, icon, label }) => {
    let redirectPath = redirect.path;
    if (redirect.path.startsWith("http")) {
        return <span onClick={() => window.open(redirect.path)} style={{ color: '#1890fe' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;
    }
    if (redirect.type === 'local') {
        let { origin } = window.location;
        redirectPath = origin + redirect.path;
        return <span onClick={() => window.open(redirectPath)} style={{ color: '#1890fe' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>;;
    }
    return (
        <Link to={{
            pathname: redirectPath,
            search: ''
        }}
        >
            <span style={{ color: '#1890fe' }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</span>
        </Link>
    );
};

@connect(({ global }) => ({ global }))
export default class Header extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showMenu: true
        }
    }
    changeMenuSate = () => {
        const { showMenu } = this.state;
        this.setState({
            showMenu: !showMenu
        })
    }
    render() {
        const { siderFold, siderRespons, theme, siderOpenKeys, menuResponsVisible, moduleData, onSwitchSidebar,
            onSwitchTheme, onSwitchMenuMode, onRoleChange, onLanguageChange, onModuleMenuChang, currentModule, language,
            currentUser, moduleGroups, onEnvChang, onLogout, settings, currentProduct, envs, global } = this.props;
        const { platformLogo, platformName } = properties
        let { showMenu } = this.state;
        return (
            <div className="brief-layout-header globalBackground">
                <div className="left-logo">
                    <div>
                        <span className="logo-link"><img style={{ width: 24, marginRight: 10, marginTop: -3 }} onClick={() => { window.open("/#", "_blank") }} src={platformLogo} />
                            <span style={{fontSize: 16}} onClick={() => { window.open("/#", "_blank") }}>{platformName}</span>
                        </span>
                        <Divider type="vertical" style={{ width: 2, height: 20, marginLeft: 14, marginRight: 14 }} />
                        <span style={{ fontSize: 14, fontWeight: "bold" }}>{currentProduct.productName}</span>
                    </div>
                    {
                        showMenu && <div className="menu-container" style={{ minWidth: 200 }}>
                            <ModuleMenu {...this.props} />
                        </div>
                    }
                </div>
                <div className="right-user" style={{ position: "relative", display: 'flex' }}>
                    {
                        currentProduct.isHasSearch ? <SearchBar changeMenuSate={this.changeMenuSate} global={global}></SearchBar> : null
                    }
                    <div style={{ display: "flex", alignItems: "center" }}>
                        {
                            currentProduct.docsUrl ?
                                <div className='help-docs-title'>
                                    <a href={currentProduct.docsUrl} target="_blank"><Tooltip title={localeHelper.get('MainMenuDocumentation', '帮助文档')}><QuestionCircleOutlined style={{ fontSize: 18, position: 'relative', top: 2 }} /> </Tooltip></a>
                                </div> : null
                        }
                        <DropDownUser />
                    </div>
                </div>
            </div>
        );
    }
}



