/**
 * Created by caoshuaibiao on 2019/3/29.
 * Action集合 Render,可用于任何组件中需要展示Action唤起的场景,如table内，组件title上等,支持水平、垂直摆放及 link/button方式显示
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { DownOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Button, Modal, Tooltip, Drawer, message, Divider } from 'antd';
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import JSXRender from "../../components/JSXRender";
import safeEval from '../../utils/SafeEval';
import { connect } from 'dva';
import UserGuideRender from './UserGuideRender';

const SubMenu = Menu.SubMenu;

@connect(({ node, global }) => ({
    userParams: Object.assign({}, { __currentUser__: global.currentUser }, node.userParams),
    nodeParams: node.nodeParams
}))
export default class ActionsRender extends React.Component {

    constructor(props) {
        super(props);
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
        let { openAction, callback, nodeParams, record, userParams } = this.props, { icon, name, label, btnType, params = {}, render, href, beforeOpen } = action, canOpen = true, beforOpenRes = {};
        if (beforeOpen) {
            beforOpenRes = safeEval("(" + beforeOpen + ")(nodeParams)", { nodeParams: Object.assign({}, nodeParams, userParams, record, params) });
            canOpen = beforOpenRes.pass;
        }
        if (!canOpen) {
            message.warn(beforOpenRes.message);
        } else {
            openAction && openAction(name, Object.assign(params, record), callback);
        }
    };

    renderActionMenuItem = (actions) => {
        let { size = 'small', record, openAction, callback, handleRefresh, nodeParams, userParams = {} } = this.props;
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
                                <RunnableAction key={index} action={action} type={'link'} size={size} paramsSet={Object.assign({}, userParams, params, nodeParams, record)} onClick={this.handleActionClick} onRefresh={handleRefresh} openAction={openAction} callback={callback} />
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
        let { actions = [], layout, type = 'link', size = 'small', record, openAction, callback, handleRefresh, nodeParams, userParams = {} } = this.props, { dockVisible, currentAction } = this.state;
        if (layout === 'vertical') {
            //把垂直布局变为通用布局的一种情况
            actions = [
                {
                    label: this.props.label || '',
                    type: type,
                    children: actions,
                    icon: this.props.icon || ''
                }
            ]
        }
        return (
            <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                {
                    actions.map((action, index) => {
                        let { params = {}, children, label, icon, docs } = action;
                        if (docs) {
                            return <UserGuideRender key={index} {...action} />
                        }
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
                        return <RunnableAction key={index} action={action} type={type} size={size} paramsSet={Object.assign({}, userParams, params, nodeParams, record)} onClick={this.handleActionClick} onRefresh={handleRefresh} openAction={openAction} callback={callback} />
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
    if (type === 'link') return <a className="oam-action-bar-item" onClick={() => openAction && openAction(name, paramsSet, callback)} >{icon ? <LegacyIcon type={icon} style={{ marginRight: 6 }} /> : null}{label}</a>;
    return <Button type={btnType} className="oam-action-bar-item" icon={icon ? <LegacyIcon type={icon} /> : null} size={size} onClick={() => onClick && onClick(action)}>{label}</Button>;
};