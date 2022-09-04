import React, { Component } from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Card, Avatar, Tooltip, } from 'antd';
import JSXRender from "../../JSXRender";
import service from '../../../../services/appMenuTreeService';
import _ from 'lodash';

const { Meta } = Card;
export default class index extends Component {
    constructor(props) {
        super(props)
        this.state = {
            jsxDom: '',
            customList: []
        };
    }
    componentWillMount() {
        let { widgetData, widgetModel, widgetConfig } = this.props;
        service.getCustomList().then(customList => {
            customList && customList.forEach(element => {
                if (element.name === widgetModel.compName || element.name === widgetModel.name || element.name === widgetConfig.compName) {
                    this.setState({
                        jsxDom: element['configObject']['jsxDom'],
                        customList: customList
                    })
                }
            });
        })
    }
    componentWillReceiveProps(nextProps) {
        let { widgetConfig, widgetModel } = nextProps;
        let {customList } = this.state;
        customList && customList.forEach(element => {
            if (element.name === widgetModel.compName || element.name === widgetModel.name || element.name === widgetConfig.compName) {
                this.setState({
                    jsxDom: element['configObject']['jsxDom'],
                })
            }
        });
    }
    render() {
        let { widgetData, widgetConfig } = this.props;
        let { customProps, height = 240 } = widgetConfig;
        let { jsxDom } = this.state;
        return <div style={{ height: height }}>
            <JSXRender {...customProps} jsx={jsxDom}></JSXRender>
        </div>

    }
}