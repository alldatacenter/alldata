/**
 * Created by caoshuaibiao on 2021/5/12.
 * 数据项的默认Toolbar实现,例如table行中的工具栏、描述列表项的工具栏等,水平铺开样式的toolbar
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { DownOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Button, Modal, Tooltip, Drawer, message, Divider } from 'antd';
import localeHelper from '../../../utils/localeHelper';
import * as util from '../../../utils/utils';
import JSXRender from './JSXRender';

const SubMenu = Menu.SubMenu;

export default class DefaultItemToolbar extends React.Component {

    constructor(props) {
        super(props);
        let { itemToolbar = {} } = props;
        let { actionList = [] } = itemToolbar, vActions = [], hActions = [], actions = [];
        for (let a = 0; a < actionList.length; a++) {
            let { icon, name, label, layout, block, type } = actionList[a];
            let adata = {
                ...actionList[a],
                name: name || block,
                btnType: type,
                icon,
                label
            };
            if (layout === "vertical") {
                vActions.push(adata);
            } else {
                hActions.push(adata);
            }
        }

        if (vActions.length > 0 && hActions.length > 0) {
            actions = [
                ...hActions,
                {
                    "btnType": "",
                    "label": "更多",
                    "children": [
                        ...vActions
                    ],
                    "icon": "more"
                },
            ]
        } else if (vActions.length > 0 && hActions.length === 0) {
            actions = [
                {
                    "btnType": "",
                    "label": "操作",
                    "children": [
                        ...vActions
                    ],
                    "icon": "down"
                },
            ]
        } else if (vActions.length === 0 && hActions.length > 0) {
            actions = [
                ...hActions
            ]
        }
        this.actions = actions;
        this.state = {
            dockVisible: false,
            currentAction: false
        };
    }

    showExecuteHistory = (action) => {
        this.setState({
            dockVisible: true,
            currentAction: action
        });
    };

    onClose = () => {
        this.setState({
            dockVisible: false
        });
    };

    handleActionClick = (action) => {
        let { openAction, callback, nodeParams, itemData, userParams } = this.props, { icon, name, label, btnType, params = {}, render, href, beforeOpen } = action, canOpen = true, beforOpenRes = {};
        openAction && openAction(action, Object.assign(params, itemData), callback);
    };

    renderActionMenuItem = (actions) => {
        let { size = 'small', itemData, openAction, callback, handleRefresh, nodeParams, userParams = {} } = this.props;
        return actions.map((action, index) => {
            let { label, icon, params } = action, divider = index !== actions.length - 1 ? <Menu.Divider key={index + "_Divider"} /> : null;
            if (action.children && action.children.length > 0) {
                return [<SubMenu key={index} title={<span style={{ fontSize: 12 }}>{icon ? <LegacyIcon type={icon} style={{ marginRight: 6 }} /> : null}<span style={{ marginRight: 12, fontSize: 12 }}>{label}</span></span>}>
                    {this.renderActionMenuItem(action.children)}
                </SubMenu>, divider];
            } else {
                return (
                    [<Menu.Item key={index}>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <div>
                                <RunnableAction key={index} action={action} type={'link'} size={size} paramsSet={Object.assign({}, userParams, params, nodeParams, itemData)} onClick={this.handleActionClick} onRefresh={handleRefresh} openAction={openAction} callback={callback} />
                            </div>
                            {
                                action.id ?
                                    <div className="oam-action-bar-item" style={{ marginRight: 0 }}>
                                        <Divider type="vertical" />
                                        <Tooltip title={localeHelper.get('manage.taskplatform.execute.history', '执行历史')}>
                                            <a onClick={() => this.showExecuteHistory(action)}>{localeHelper.get('manage.taskplatform.execute.history', '历史')}</a>
                                        </Tooltip>
                                    </div> : null
                            }
                        </div>
                    </Menu.Item>, divider]
                )

            }


        });
    };

    render() {
        let { type = 'link', size = 'small', itemData, openAction, callback, handleRefresh, nodeParams, userParams = {}, itemToolbar = {} } = this.props, actions = this.actions;
        type = itemToolbar.type || type;
        let { customRender } = itemToolbar, paramsSet = Object.assign({}, userParams, nodeParams, itemData);
        return (
            <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                {
                    customRender &&
                    <JSXRender jsx={util.renderTemplateString(customRender, { row: paramsSet, ...paramsSet })} />
                }
                {
                    actions.map((action, index) => {
                        let { params = {}, children, label, icon, docs } = action;
                        //存在分组的情况,子操作全部按照下拉方式展示
                        if (children && children.length) {
                            return (
                                <Dropdown key={index} overlay={<Menu>{this.renderActionMenuItem(children)}</Menu>}>
                                    {
                                        type === 'link' ?
                                            <a className="oam-action-bar-item" size={size}>
                                                {label || localeHelper.get("operation", '操作')} <DownOutlined />
                                            </a>
                                            :
                                            <Button className="oam-action-bar-item" size={size}>
                                                {icon ? <LegacyIcon type={icon} /> : null}{label || localeHelper.get("operation", '操作')} <DownOutlined />
                                            </Button>
                                    }
                                </Dropdown>
                            );
                        }
                        return <RunnableAction key={index} action={action} type={type} size={size} paramsSet={Object.assign({}, userParams, params, nodeParams, itemData)} onClick={this.handleActionClick} onRefresh={handleRefresh} openAction={openAction} callback={callback} />
                    })

                }
            </div>
        );
    }
}

const RunnableAction = ({ action, type, size = 'small', paramsSet, onClick, onRefresh, openAction, callback }) => {
    let { icon, name, label, btnType, render, href, hiddenExp } = action;
    if (hiddenExp) {
        let row = paramsSet;
        if (eval(hiddenExp, row)) {
            return null;
        }
    }
    //增加超链配置
    if (href) {
        href = util.renderTemplateString(href, { row: paramsSet, ...paramsSet });
        if (type === 'link') {
            return <a className="oam-action-bar-item" href={href} >{icon ? <LegacyIcon type={icon} style={{ marginRight: 6 }} /> : null}{label}</a>;
        }
        return <Button type={btnType} className="oam-action-bar-item" icon={icon ? <LegacyIcon type={icon} /> : null} size={size} onClick={() => window.location.href = href}>{label}</Button>;
    }
    if (render) {
        return <div className="oam-action-bar-item"><JSXRender jsx={util.renderTemplateString(render, { row: paramsSet, ...paramsSet })} /></div>
    }
    //内置的刷新action
    if (name === "__refresh__") {
        return <Button type={btnType} className="oam-action-bar-item" icon={icon ? <LegacyIcon type={icon} /> : null} size={size} onClick={() => onRefresh && onRefresh()}>{label ? label : localeHelper.get("manage.taskplatform.common.refresh", "刷新")}</Button>;
    }
    if (type === 'link') return <a className="oam-action-bar-item" onClick={() => openAction && openAction(action, paramsSet, callback)} >{icon ? <LegacyIcon type={icon} style={{ marginRight: 6 }} /> : null}{label}</a>;
    return <Button type={btnType} className="oam-action-bar-item" icon={icon ? <LegacyIcon type={icon} /> : null} size={size} onClick={() => onClick && onClick(action)}>{label}</Button>;
};