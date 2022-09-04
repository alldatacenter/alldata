import React, { Component } from 'react';
import { connect } from 'dva'
import { Result, Button } from 'antd'
import JSXRender from "../../../../../components/JSXRender"
import _ from 'lodash'
@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
class ResultStatus extends Component {
    constructor(props) {
        super(props);
        this.state = {
        }
    }
    render() {
        const { widgetConfig = {}, widgetData } = this.props;
        let {status,statusTitle,subStatusTitle,customIcon,extra, childrenContent} = widgetConfig;

        return (
            <Result
                status={widgetData || status || "success"}
                icon={customIcon||''}
                title={statusTitle || 'Successfully Purchased Cloud Server ECS!'}
                subTitle={subStatusTitle || 'Order number: 2017182818828182881 Cloud server configuration takes 1-5 minutes, please wait.'}
                extra={<JSXRender jsx={extra}/> || [
                    <Button type="primary" key="console">
                      Go Console
                    </Button>,
                    <Button key="buy">Buy Again</Button>
                  ]}
            >
                <JSXRender jsx={childrenContent}/>
            </Result>
        );
    }
}

export default ResultStatus;