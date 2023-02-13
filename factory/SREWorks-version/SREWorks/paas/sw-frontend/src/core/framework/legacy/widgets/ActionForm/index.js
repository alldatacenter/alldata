/**
 * Created by caoshuaibiao on 2019/11/5.
 * 表单挂件
 */
import React, { Component } from 'react';
import OamAction from '../../../OamAction';
import { Spin, message } from 'antd';
import _ from 'lodash';

class ActionForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            action: null,
            buildId: null
        }
    }

    componentWillMount() {
        const { __app_id__ } = this.props.nodeParams, { nodeId, actionData, mode, actions = [] } = this.props;
        let action = actions.filter(action => {
            return action.config.name === mode.config.action;
        })[0] || actionData;
        if (action) {
            this.setState({
                action: action,
                loading: false
            });
        } else {
            message.warn("Action Not Found");
        }
    }

    componentDidUpdate(prevProps) {
        let { mode, nodeParams } = this.props;
        let { buildDepends } = mode.config;
        if (buildDepends && buildDepends.length) {
            let reBuildId = false;
            buildDepends.forEach(pName => {
                if (!_.isEqual(prevProps.nodeParams[pName], nodeParams[pName])) {
                    reBuildId = (new Date()).getTime();
                }
            });
            if (reBuildId) {
                this.setState({
                    buildId: reBuildId
                });
            }
        }

    }

    render() {
        const { action, buildId } = this.state, { mode } = this.props;
        if (!action) {
            return <Spin />
        }
        return (
            <OamAction {...this.props} showSearch={mode.config.showSearch} advanced={mode.config.advanced} showScenes={mode.config.showScenes} addScene={mode.config.addScene} key={buildId || action.id || action.elementId} actionId={action.id || action.elementId} actionData={action} mode="custom" displayType={this.props.displayType || "form"} />
        );
    }
}

export default ActionForm;