/**
 * Created by caoshuaibiao on 2021/3/3.
 * 操作
 */
import React, { Component } from 'react';
import OamAction from '../../OamAction';
import { Spin, message } from 'antd';
import _ from 'lodash';
//过滤器类型进行显示类型映射

class Action extends Component {

    constructor(props) {
        super(props);
        this.state = {
            actionData: null,
        }
    }

    componentWillMount() {
        //widget 生成原 Action所需的过滤器数据
        const { widgetModel, widgetConfig } = this.props;
        const { nodeModel } = widgetModel;
        let { form, blocks, uniqueKey, parameters, ...other } = widgetConfig;
        //分步表单,分步表单只能通过区块去定义,分步操作不支持页面存在其他区块
        if (widgetModel.type === 'STEP_ACTION' && blocks) {
            //由区块提取其中定义的表单，然后拼装成原分步action中的数据模型
            let actionData = {
                config: {
                    ...other,
                    name: form,
                    actionType: "STEPS",
                    stepActions: [],
                },
                id: 99999,
                elementId: uniqueKey
            };
            for (let b = 0; b < blocks.length; b++) {
                let block = blocks[b];
                let blockDef = nodeModel.getBlocks().filter(blockMeta => blockMeta.elementId === block)[0];
                //区块可能已经删除不存在了
                if (!blockDef) {
                    continue;
                }
                let { elements = [], label } = blockDef;
                //分步仅支持每个步骤一个表单，因此也取步骤区块中的第一个表单
                let actionElement = null;
                if (elements[0] && elements[0].type === 'FluidGrid') {
                    let { rows } = elements[0].config;
                    rows.forEach(row => {
                        actionElement = row.elements.filter(elementMeta => (elementMeta.type === 'ACTION' || elementMeta.type === 'STEP_FORM'))[0];
                    })
                } else {
                    actionElement = elements.filter(elementMeta => (elementMeta.type === 'ACTION' || elementMeta.type === 'STEP_FORM'))[0];
                }
                if (actionElement) {
                    let { parameters, ...otherConfig } = actionElement.config;
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
                    let stepActionData = {
                        config: {
                            ...otherConfig,
                            name: otherConfig.name || otherConfig.label || otherConfig.uniqueKey,
                            label: label,
                            actionType: b === blocks.length - 1 ? "API" : "READ",
                            parameterDefiner: parameterDefiner,
                            isDrawer: true,
                        },
                        id: 99999,
                        elementId: otherConfig.uniqueKey
                    };
                    actionData.config.stepActions.push(stepActionData);
                }
            }
            if (actionData.config.stepActions.length) {
                this.setState({ actionData });
            }
        } else {
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
                        ...other,
                        name: widgetConfig.name || widgetConfig.label || uniqueKey,
                        actionType: widgetModel.type === 'STEP_FORM' ? "READ" : "API",
                        parameterDefiner: parameterDefiner,
                    },
                    id: 99999,
                    name: widgetConfig.name || widgetConfig.label || uniqueKey,
                    elementId: uniqueKey
                };
                if (this.props.currentAction && this.props.currentAction.isDrawer) {
                    actionData.config['isDrawer'] = true;
                }
                this.setState({ actionData });
            }
        }
    }

    render() {
        const { actionData } = this.state;
        const { widgetModel } = this.props;
        if (!actionData) {
            return <div style={{ width: "100%", height: "100%", justifyContent: "center", alignItems: "center", display: "flex" }}><h3>{widgetModel.type === 'STEP_ACTION' ? '请设置分步操作' : '请定义步骤表单'}</h3></div>
        }
        return (
            <OamAction {...this.props}
                key={actionData.elementId}
                actionId={actionData.elementId}
                mode="custom"
                displayType={"panel"}
                actionData={actionData}
            />
        );
    }
}

export default Action;