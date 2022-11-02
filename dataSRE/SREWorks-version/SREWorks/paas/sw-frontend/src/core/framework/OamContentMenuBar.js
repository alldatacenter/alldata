/**
 * Created by caoshuaibiao on 2019/2/21.
 * 运维功能页面导航菜单
 */

import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { BarsOutlined, DownOutlined, FilterOutlined } from '@ant-design/icons';
import {
    Menu,
    Divider,
    Cascader,
    Badge,
    Tooltip,
    Dropdown,
    TreeSelect,
    Drawer,
    Button,
    Collapse,
} from 'antd';
import { Link } from 'dva/router';
import FullScreenTool from "../../components/FullScreenTool"
import OamActionBar from './OamActionBar';
import { connect } from 'dva';
import properties from 'appRoot/properties';
import JSXRender from "../../components/JSXRender";
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";

import '../style.less';

const MenuItemGroup = Menu.ItemGroup;
const SubMenu = Menu.SubMenu;
const { Panel } = Collapse;

@connect(({ global }) => ({
    theme: global.theme
}))

class OamContentMenuBar extends React.Component {

    constructor(props) {
        super(props);
        let menusData = props.menusData;
        if (menusData) {
            const procMeuns = function (menuData) {
                if (Array.isArray(menuData)) {
                    menuData.forEach(md => {
                        procMeuns(md)
                    })
                } else {
                    menuData.value = menuData.nodeId;
                    menuData.key = menuData.nodeId;
                    menuData.disabled = !menuData.canClick;
                    if (menuData.children) {
                        menuData.children.forEach(cm => {
                            procMeuns(cm);
                        });
                    }
                }

            };
            procMeuns(menusData);
        }
        let { defaultFilterPath, nodeId } = this.props;
        /*if(filterMode==='multiple'){
            selected=defaultFilterPath[defaultFilterPath.length-1]&&defaultFilterPath[defaultFilterPath.length-1].split(",")||[];
        }else{
            selected=defaultFilterPath;
        }*/
        this.state = {
            selected: defaultFilterPath,
            menuNode: nodeId,
            menusData: menusData,
            showHistory: false
        }
    }

    handleFilterChanged = (value, selectedOptions, leafList) => {
        const { handleFilterChanged, filterMode } = this.props;
        this.setState({
            selected: value
        });
        handleFilterChanged && handleFilterChanged(filterMode === 'multiple' ? leafList : selectedOptions);
    };

    renderMenuItem = (menuData) => {
        return menuData.map(({ children, name, icon, label, nodeId, canClick }, key) => {
            if (canClick) {
                if (children && children.length > 0) {
                    return (
                        <SubMenu key={nodeId} title={<span><LegacyIcon type={icon || 'folder'} /><span>{label}</span></span>}>
                            {this.renderMenuItem(children)}
                        </SubMenu>
                    );
                } else {
                    return (
                        <Menu.Item key={nodeId}>
                            <LegacyIcon type={icon || 'deployment-unit'} /><span>{label}</span>
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
    //暂时保留菜单样式的
    getHeaderMenu() {
        let { menusData } = this.props;
        return (
            <Dropdown overlay={
                <Menu
                    onClick={this.onNodeChange}
                >
                    {
                        this.renderMenuItem(menusData)
                    }
                </Menu>
            }>
                <a>
                    select item <DownOutlined />
                </a>
            </Dropdown>
        );
    }

    onNodeChange = (nodeId) => {
        this.setState({
            menuNode: nodeId
        });
        let { handleNodeChanged } = this.props;
        handleNodeChanged && handleNodeChanged(nodeId)
    };

    handTimeChange = (timeRange) => {
        let { dispatch } = this.props;
        dispatch({
            type: 'node/updateParams', paramData: {
                ___refresh_timestamp: (new Date()).getTime(),
                startTime: timeRange[0], endTime: timeRange[1],
                stime_ms: timeRange[0], etime_ms: timeRange[1],
                stime: parseInt(timeRange[0] * 0.001), etime: parseInt(timeRange[1] * 0.001)
            }
        });
    };

    handleShowHistory = () => {
        this.setState({
            showHistory: true
        });
    };

    hadnleHistoryClose = () => {
        this.setState({
            showHistory: false
        });
    };

    render() {
        const { actions, nodeParams, filters = [], history, routeData, urlSearchParams, nodeId, userParams, menuBarHeader, menuLabel, filterMode, theme, hideActionHistory } = this.props, { menusData, showHistory } = this.state;
        let selectKeys = [];
        routeData.children.forEach(cp => {
            if (history.location.pathname.indexOf(cp.path + "/") > -1 || history.location.pathname.endsWith(cp.path)) {
                selectKeys.push(cp.path);
                let { config } = cp;
                if (config && (config.hidden === true || config.hidden === "true")) {
                    if (config.selectPathInHidden) {
                        selectKeys.push(config.selectPathInHidden);
                    }
                }
            }
        });
        let hiddenBar = true;
        _.forEach(routeData.children, function (route, index) {
            if (route.config && (route.config.hidden !== true && route.config.hidden !== "true")) {
                hiddenBar = false;
            }
        });
        //只有一个tab页时也隐藏
        if (hiddenBar === false && routeData.children.length === 1) {
            hiddenBar = true;
        }
        if (actions.length === 0 && filters.length === 0 && hiddenBar && !menusData) {
            return <div />
        }
        let disPlayMenuBarHeader = menuBarHeader;
        if (menuBarHeader && menuBarHeader.indexOf && menuBarHeader.indexOf("$") > -1) {
            disPlayMenuBarHeader = <JSXRender jsx={util.renderTemplateString(menuBarHeader, Object.assign({}, nodeParams, urlSearchParams))} />;
        }
        let menus = [];
        if (routeData.children && routeData.children.length) {
            menus = (routeData.children.map(({ path, icon, label, config = {} }) => {
                if (!label) return null;
                if (config && (config.hidden === true || config.hidden === "true")) return null;
                //增加 redirect配置
                //增加url自定义参数声明
                let { redirect, acceptUrlParams = [] } = config, acceptParams = {};
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
                            {
                                redirect ?
                                    <Redirect redirect={redirect} label={label} icon={icon} />
                                    :
                                    <Link to={{
                                        pathname: path,
                                        search: util.objectToUrlParams(acceptParams)
                                    }}
                                    >
                                        {icon ? <LegacyIcon type={icon} /> : null}<span>{label}</span>
                                    </Link>
                            }
                        </Menu.Item>
                    );
                }
            }).filter(r => r !== null)
            )
        }
        return (
            <div className="globalBackground" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: menus.length > 0 ? 8 : 0 }} key={nodeId}>
                <div style={{ display: 'flex' }}>
                    {
                        menusData ?
                            <div style={{ height: 48, display: 'flex', marginLeft: 8, alignItems: 'center' }} key="__top_menu">
                                <span />
                                <TreeSelect
                                    style={{ minWidth: 240 }}
                                    value={this.state.menuNode}
                                    dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
                                    treeData={menusData}
                                    placeholder="Please select"
                                    treeDefaultExpandAll
                                    onChange={this.onNodeChange}
                                />
                                <span style={{ marginLeft: 12 }}>
                                    <FullScreenTool elementId="__OAM_CONTENT__" />
                                </span>
                            </div>
                            : null
                    }
                    {
                        menuBarHeader ?
                            <div style={{ display: 'flex', marginLeft: 12, alignItems: 'center' }} key="__node_title">
                                <b style={{ fontSize: 16 }}>{menuBarHeader === true ? (this.props.hasTerminal ? (nodeParams.Host || nodeParams.__serviceItemName__) : nodeParams.__serviceItemName__) : disPlayMenuBarHeader}</b>
                                {this.props.hasTerminal ?
                                    <a style={{ marginLeft: 9 }}>
                                    </a>
                                    : null
                                }
                            </div>
                            : null
                    }
                    {
                        filters.length > 0 ?
                            <div style={{ display: 'flex', height: 48, alignItems: 'center' }} key="__node_filters">
                                <Divider type="vertical" style={{ height: 18 }} />
                                <div className="oam">
                                    <FilterOutlined style={{ marginRight: 8 }} />
                                    <Cascader value={this.state.selected}
                                        style={{ minWidth: 210 }}
                                        placeholder={localeHelper.get('oam.common.selectPrompt', "请选择")}
                                        showSearch
                                        options={filters}
                                        expandTrigger="hover"
                                        onChange={this.handleFilterChanged}
                                        displayRender={(label, selectOptions) => label[label.length - 1]}
                                        popupClassName="oam-cascader-pop"
                                    />
                                </div>
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
                    {menus.length > 0 ?
                        <div key="__node_menus" className="content-top-menu-bar" style={{ marginLeft: 0 }}>
                            <Menu
                                style={{minWidth:200}}
                                selectedKeys={selectKeys}
                                mode="horizontal"
                            >
                                {menus}
                            </Menu>
                        </div> : null
                    }
                </div>
                <div>
                    {

                        <div style={{ display: 'flex', height: 48, alignItems: 'center', marginRight: 6 }} key="__node_oper">
                            {
                                hideActionHistory ? null :
                                    <Button size="small" type="primary" icon={<BarsOutlined />} style={{ fontSize: 12, marginRight: 12 }} onClick={this.handleShowHistory}>
                                        {localeHelper.get("manage.taskplatform.execute.history", '操作历史')}
                                    </Button>
                            }

                            <div key={actions.length}>
                                {
                                    actions.length ? <OamActionBar {...this.props} actions={actions} nodeParams={nodeParams} userParams={userParams} /> : null
                                }
                            </div>
                            <Drawer
                                title={localeHelper.get('manage.taskplatform.execute.history', '操作历史')}
                                width={'70%'}
                                closable={true}
                                destroyOnClose={true}
                                onClose={this.hadnleHistoryClose}
                                visible={showHistory}
                            >
                            </Drawer>
                        </div>
                    }
                </div>
            </div>
        );

    }
}

const Redirect = ({ redirect, icon, label }) => {

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


export default OamContentMenuBar;
