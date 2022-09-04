/**
 * Created by caoshuaibiao on 2020/12/2.
 * 页面内容设计器
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List } from 'antd';

import ContentLayout from './ContentLayout';
import ContentToolbar from './ContentToolbar';

export default class ContentDesigner extends React.Component {

    constructor(props) {
        super(props);
        this.state = {}
    }

    render() {
        return (
            // <Card size="small" type="inner" bordered={false} extra={<ContentToolbar {...this.props}/>} style={{ width: '100%' }} bodyStyle={{padding:0}}>
            <ContentLayout {...this.props} />
            // </Card>
        );
    }
}



