import React, { Component } from 'react';
import OamAction from '../../../OamAction';
import { Spin, message } from 'antd';

class TabFilter extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            action: null
        }
    }
    componentWillMount() {
        const { __app_id__ } = this.props.nodeParams, { nodeId, parameters, mode, actions = [] } = this.props;
        let action = actions.filter(action => {
            return action.config.name === mode.config.action;
        })[0];
        if (action) {
            this.setState({
                action: action,
                loading: false
            });
        } else {
            message.warn("Action Not Found");
        }
    }
    render() {
        const { action } = this.state, { mode } = this.props;
        if (!action) {
            return <Spin />
        }
        return <OamAction {...this.props} filterConfig={mode.config} showSearch={mode.config.showSearch} advanced={mode.config.advanced} key={action.id || action.elementId} actionId={action.id || action.elementId} actionData={action} mode="custom" displayType={"tabFilter"} />
    }
}

export default TabFilter;