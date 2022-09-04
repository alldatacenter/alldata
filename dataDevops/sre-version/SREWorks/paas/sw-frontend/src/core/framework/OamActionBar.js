/**
 * Created by caoshuaibiao on 2019/1/7.
 * 运维动作工具栏,其中的action是已经具备完整定义数据,而不是配置数据,这是和ActionRender中action最大的区别
 */
import React from 'react';
import { Dropdown, Menu, Card, Modal, Tooltip, List, Divider, Drawer, Button } from 'antd';
import ActionsRender from './ActionsRender';

export default class OamActionBar extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            dockVisible: false,
            currentAction: false
        };
    }

    render() {
        let { actions = [], nodeId, openAction, nodeParams, userParams, nodeConfig = {}, ...contentProps } = this.props, { dockVisible, currentAction } = this.state;
        let { actionBar = {} } = nodeConfig;
        let customActions = actionBar.actions;
        //自定义actions的排布
        if (customActions) {
            return <ActionsRender key="__actions_" {...this.props} {...actionBar} nodeParams={nodeParams} openAction={openAction} />;
        } else {
            //把bar统一转为 actionRender渲染
            let tranBar = {
                label: '',
                type: 'button',
                actions: [
                    {
                        label: '',
                        type: 'button',
                        children: actions.map(action => {
                            return {
                                ...action.config,
                                elementId: action.elementId,
                                id: action.id
                            }
                        }),
                        icon: 'setting'
                    }
                ],
                icon: 'setting'
            };
            return <ActionsRender key="__actions_" {...this.props} {...tranBar} nodeParams={nodeParams} openAction={openAction} />;
        }
    }
}