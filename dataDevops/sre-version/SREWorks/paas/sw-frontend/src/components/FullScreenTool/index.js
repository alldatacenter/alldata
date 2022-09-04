/**
 * Created by caoshuaibiao on 2019/5/14.
 * 全屏工具
 */
import React from 'react';
import { FullscreenOutlined } from '@ant-design/icons';
import { Tooltip, Button } from 'antd';
import localeHelper from '../../utils/localeHelper';

export default class FullScreenTool extends React.Component {

    requestFullScreen = () => {
        const { elementId } = this.props;
        let element = document.getElementById(elementId);
        let requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
        if (requestMethod) {
            requestMethod.call(element);
        }
    };



    render() {
        return (
            <Tooltip title={localeHelper.get('fullScreen', '全屏')}>
                <Button onClick={this.requestFullScreen}><FullscreenOutlined /></Button>
            </Tooltip>
        );
    }
}
