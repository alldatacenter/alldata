/**
 * Created by caoshuaibiao on 2020/12/21.
 * 自定义渲染组件
 */
import React, { Component } from 'react';
import _ from 'lodash';
import JSXRender from '../../JSXRender';

export default class CustomRender extends Component {
    render() {
        const jsx = _.get(this.props, 'widgetConfig.jsxDom', '<span>请填写组件的jsx属性</span>');
        return (
            <JSXRender  {...this.props} jsx={jsx}
            />
        );
    }
}