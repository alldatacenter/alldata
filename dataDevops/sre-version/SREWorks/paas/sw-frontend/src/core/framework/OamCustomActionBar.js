/**
 * Created by caoshuaibiao on 2019/3/14.
 * 用户自定义Action,负责Action唤起
 */

import React from 'react';
import { Dropdown, Menu, Card, Modal, Tooltip, List, Drawer } from 'antd';
import OamAction from './OamAction';
import OamWidgets from './OamWidgets';

export default class OamCustomActionBar extends React.Component {

    constructor(props) {
        super(props);
        let { actions = [], currentActionMix, inActionPanelTab = [] } = this.props;
        let action = false, actionTab = false;
        if (currentActionMix.actionName && actions.length > 0) {
            action = actions.filter(action => {
                return action.config.name === currentActionMix.actionName;
            })[0]
        }
        if (!action && inActionPanelTab.length) {
            actionTab = inActionPanelTab.filter(tab => tab.name === currentActionMix.actionName)[0];
        }
        this.state = {
            dockVisible: actionTab,
            action: action,
            actionTab: actionTab
        };
    }

    onClose = () => {
        let { onCloseAction } = this.props;
        this.setState({ dockVisible: false });
        onCloseAction && onCloseAction(this.state.action);
    };

    render() {
        let { nodeId, nodeParams, userParams, currentActionMix, ...contentProps } = this.props;
        let { action, actionTab, dockVisible } = this.state;
        if (actionTab) {
            let { config } = actionTab;
            return (
                <Drawer
                    placement="right"
                    title={actionTab.label}
                    width={config.width ? config.width : '60%'}
                    destroyOnClose={true}
                    onClose={this.onClose}
                    visible={dockVisible}
                >
                    <OamWidgets {...this.props} metaData={actionTab}
                        widgets={actionTab.components}
                        layout={actionTab.layout}
                        nodeId={actionTab.nodeId}
                        widgetParams={Object.assign({}, userParams, currentActionMix.actionParams)}
                    />
                </Drawer>
            )
        }
        if (action) {
            return <OamAction {...contentProps} key={action.id || action.elementId} actionId={action.id || action.elementId} nodeId={nodeId}
                actionData={action} nodeParams={nodeParams} userParams={userParams}
                mode="custom" execCallBack={currentActionMix.callBack}
                actionParams={currentActionMix.actionParams}
                onClose={this.onClose}
            />
        }
        return <div />;
    }
}