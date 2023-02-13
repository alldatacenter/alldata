/**
 * Created by caoshuaibiao on 2021/3/3.
 * 过滤器
 */
import React, { Component } from 'react';
import OamAction from '../../OamAction';
import { Spin, message } from 'antd';
import * as Utils from "../../../../utils/utils";
import _ from 'lodash';
import safeEval from ".././../../../utils/SafeEval";
//过滤器类型进行显示类型映射

const displayTypeMapping = {
    "FILTER_BAR": "filterBar",
    "FILTER_FORM": "form",
    "FILTER_MIX": "mixFilterBar",
    "FILTER_SINGLE": "filter",
    "FILTER_TAB": 'tabFilter'
};

class Filter extends Component {

    constructor(props) {
        super(props);
        this.state = {
            actionData: null,
            visible:true
        }
    }

    componentWillMount() {
        //widget 生成原 Action所需的过滤器数据
        const { widgetModel, widgetConfig } = this.props;
        const { nodeModel } = widgetModel;
        const config = widgetConfig;
        let { parameters,visibleExpression } = config;
        if (parameters && parameters.length) {
            //为了复用参数定义映射而拼装parameterDefiner
            let parameterDefiner = {
                bindingParamTree: [],
                noBinding: true,
                paramMapping: parameters.map(p => {
                    return {
                        mapping: [],
                        parameter: p
                    }
                })
            };
            let actionData = {
                config: {
                    actionType: "OUTPUT",
                    parameterDefiner: parameterDefiner,
                    outputName: config.outputName,
                    autoSubmit: config.autoSubmit,
                    enterSubmit: config.enterSubmit,
                    scenes: config.scenes,
                    labelSpan: config.labelSpan,
                    column: config.column,
                    showSearch: config.showSearch,
                    tabType: config.tabType || '',
                    tabSize: config.tabSize || 'default',
                    tabPosition: config.tabPosition || 'left',
                    visibleExpression: config.visibleExpression || '',
                    hasBackground: config.hasBackground,
                },
                elementId: config.uniqueKey
            };
            this.setState({ actionData });
        }

    }
    componentDidMount() {
        let nowProps = this.props;
        this.changeVisilbleFromExpression(nowProps)
    }
    componentWillReceiveProps(nextProps) {
        this.changeVisilbleFromExpression(nextProps)
    }
    // 根据通用配置表达式配置过滤器是否可见
    changeVisilbleFromExpression=(props)=> {
        let {widgetConfig={}, nodeParams={}, userParams={}, actionParams={}} = props;
        let { visibleExpression } = widgetConfig,globalParams=Object.assign({},nodeParams,userParams,actionParams);
        let isVisable = true;
        try {
            if(visibleExpression && visibleExpression.length > 4) {
                if (visibleExpression.includes('$')) {
                    visibleExpression = Utils.renderTemplateString(visibleExpression,globalParams)
                }
                isVisable = safeEval(visibleExpression,{nodeParams:globalParams});
            }
            this.setState({
                visible: isVisable
            })
        } catch(error) {
            this.setState({
                visible: isVisable
            })
        }
    }
    render() {
        const { actionData,visible } = this.state, { mode, widgetModel } = this.props;
        if (!actionData) {
            return <div style={{ width: "100%", height: "100%", justifyContent: "center", alignItems: "center", display: "flex" }}><h3>请定义过滤项</h3></div>
        }
        if(!visible) {
            return <div></div>
        }
        return (
            visible && <OamAction {...this.props}
                key={actionData.elementId}
                actionId={actionData.elementId}
                actionData={actionData}
                mode="custom"
                displayType={displayTypeMapping[widgetModel.type] || "filterBar"}
            />
        );
    }
}

export default Filter;