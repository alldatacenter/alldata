/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:40
 * @Description:操作属性定义面板
 */
import React from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import './formCommon.less'
import { Row, Col, } from 'antd';
import Constants from "../../../framework/model/Constants";
import FormElementType from "../../../../components/FormBuilder/FormElementType";
import FormElementFactory from "../../../../components/FormBuilder/FormElementFactory";

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 16 },
    },
};



class ActionSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config } = props;
        let paramsDef = [], { api, bodyType, form, method, column, labelSpan, beforeHandler, beforeSubmitHandler, hintFunction, refresh, responseHandler,jumpPath, label, blocks, submitLabel = "执行", confirmUrl, formLayout } = config;
        this.isStep = widgetModel.isStepAction();
        let scriptProps = { height: 360, disableShowDiff: true, mode: "javascript" };
        let beforeHandlerTpl = 'function beforeHandler(nodeParams){\n  return {type:"",message:"",title:"",pass:true,transformParams:false,dynamicItems:[]}\n}';
        let beforeSubmitHandlerTpl = 'function beforeSubmitHandler(nodeParams,formValues,httpClient){\n  return {type:"",message:"",title:"",pass:true,transformParams:{}}\n}';
        let hintHandlerTpl = 'function hintHandler(nodeParams,formValues){\n  return ""\n}';
        let responseHandlerTpl = 'function responseHandler(response){\n  return ""\n}';
        if (widgetModel.type === 'STEP_FORM') {
            paramsDef.push({
                type: FormElementType.SELECT, name: 'column', initValue: column, required: false, label: "一行几列", inputTip: "一行放置几个表单项",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }]
            });
            paramsDef.push({
                type: FormElementType.SELECT, name: 'labelSpan', initValue: labelSpan, required: false, label: "label占比", inputTip: "参数标签宽度占比,总24",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }, { value: "7", label: '7' }, { value: "8", label: '8' }]
            });
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'beforeHandler', initValue: beforeHandler || beforeHandlerTpl, required: false, label: "前置函数",
                tooltip: "定义打开Action前的用户业务逻辑,可以进行Action初始化参数转换/是否可操作等信息提示。nodeParams 节点参数;" +
                    "返回值对象含义：pass 是否可以启动操作；type: 提示信息类型 值为info/warn/error；message 提示信息,支持jsx；" +
                    "transformParams:表单初始化值对象,可进行参数赋值覆盖等；dynamicItems 动态表单集合定义,可动态控制表单项可见/新增/只读等；width 自定对话框宽度",
                defModel: scriptProps,
                template: beforeHandlerTpl
            });
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'hintFunction', initValue: hintFunction || hintHandlerTpl, required: false, label: "动态提示函数",
                tooltip: "动态提示,函数需要返回JSX字符串，nodeParams 节点参数对象,formValues表单参数对象；",
                defModel: scriptProps,
                template: hintHandlerTpl
            });
        } else if (this.isStep) {
            let enableBlock = [];
            let containerId = widgetModel.getContainerModel() && widgetModel.getContainerModel().getId();
            widgetModel.nodeModel.getBlocks().forEach(block => {
                let { elementId, label, category } = block;
                if ((category === Constants.BLOCK_CATEGORY_ACTION || category === Constants.BLOCK_CATEGORY_FORM) && elementId !== containerId) {
                    enableBlock.push(
                        {
                            value: elementId,
                            label: label
                        }
                    );
                }
            });
            paramsDef.push({
                type: FormElementType.MULTI_SELECT, name: 'blocks', initValue: blocks, required: true, label: "选择操作区块", inputTip: "选择的操作区块,按照选择顺序组成分步操作",
                optionValues: enableBlock
            });
        } else {
            //第一列
            paramsDef.push({ type: FormElementType.INPUT, name: 'api', initValue: api, required: true, label: "提交地址", tooltip: "表单提交到的地址,可以是指定http地址,也可以是挂载网关后的地址" });
            paramsDef.push({ type: FormElementType.INPUT, name: 'label', initValue: label, required: true, label: "操作名称", inputTip: "输入用于标识操作的名称" });
            paramsDef.push({
                type: FormElementType.RADIO, name: 'bodyType', initValue: bodyType || 1, required: true, label: "提交格式",
                tooltip: "x-www-form-urlencoded:传统Form表单方式；JSON:以json对象提交",
                optionValues: [{ value: 1, label: 'JSON' }, { value: 2, label: 'x-www-form-urlencoded' }]
            });
            paramsDef.push({
                type: FormElementType.RADIO, name: 'method', initValue: method || 'POST', required: true, label: "提交方式",
                tooltip: "操作提交请求的方式",
                optionValues: [{ value: 'POST', label: 'POST' }, { value: 'PUT', label: 'PUT' }, { value: 'DELETE', label: 'DELETE' }]
            });
            paramsDef.push({ type: FormElementType.INPUT, name: 'submitLabel', initValue: submitLabel, required: true, label: "操作按钮名称", inputTip: "提交表单的按钮名称" });
            //第二列
            paramsDef.push({
                type: FormElementType.SELECT, name: 'column', initValue: column, required: false, label: "一行几列", inputTip: "一行放置几个表单项",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }]
            });
            paramsDef.push({
                type: FormElementType.SELECT, name: 'labelSpan', initValue: labelSpan, required: false, label: "label占比", inputTip: "参数标签宽度占比,总24",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }, { value: "7", label: '7' }, { value: "8", label: '8' }]
            });
            paramsDef.push({ type: FormElementType.INPUT, name: 'jumpPath', initValue: jumpPath, required: false, label: "执行后跳转", tooltip: "地址可以是指定http地址,也可以是挂载网关后的相对地址", inputTip: "地址路径无http在当前窗口打开,有http在新窗口打开" });
            paramsDef.push({ type: 1, name: 'confirmUrl', initValue: confirmUrl, required: false, label: "二次提交地址", inputTip: "第一次提交至服务端根据返回的信息二次确认后提交的地址" });
            paramsDef.push({
                type: FormElementType.CHECKBOX, name: 'refresh', initValue: refresh, required: false, label: "执行后刷新页面",
                optionValues: [{ value: "Y", label: '是' }]
            });
            //第三列
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'beforeHandler', initValue: beforeHandler || beforeHandlerTpl, required: false, label: "前置函数",
                tooltip: "定义打开Action前的用户业务逻辑,可以进行Action初始化参数转换/是否可操作等信息提示。nodeParams 节点参数;" +
                    "返回值对象含义：pass 是否可以启动操作；type: 提示信息类型 值为info/warn/error；message 提示信息,支持jsx；" +
                    "transformParams:表单初始化值对象,可进行参数赋值覆盖等；dynamicItems 动态表单集合定义,可动态控制表单项可见/新增/只读等；width 自定对话框宽度",
                defModel: scriptProps,
                template: beforeHandlerTpl
            });
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'beforeSubmitHandler', initValue: beforeSubmitHandler || beforeSubmitHandlerTpl, required: false, label: "提交前置函数",
                tooltip: "提交时对提交的数据进行处理函数，nodeParams 节点参数对象,formValues表单参数对象；" +
                    "返回值对象含义：pass 是否可以提交；type: 提示信息类型 值为info/warn/error；message 提示信息,支持jsx；" +
                    "transformParams:表单提交值转换,可进行赋值覆盖追加等；",
                defModel: scriptProps,
                template: beforeSubmitHandlerTpl
            });
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'hintFunction', initValue: hintFunction || hintHandlerTpl, required: false, label: "动态提示函数",
                tooltip: "动态提示,函数需要返回JSX字符串，nodeParams 节点参数对象,formValues表单参数对象；",
                defModel: scriptProps,
                template: hintHandlerTpl
            });
            paramsDef.push({
                type: FormElementType.RADIO, name: 'formLayout', initValue: formLayout || 'horizontal', required: true, label: "表单项布局",
                tooltip: "单个表单的label和输入项之间的布局关系",
                optionValues: [{ value: 'horizontal', label: '水平' }, { value: 'vertical', label: '垂直' }, { value: 'inline', label: '同行' }]
            });
            paramsDef.push({
                type: FormElementType.MODAL_ACE, name: 'responseHandler', initValue: responseHandler || responseHandlerTpl, required: false, label: "提交后展示信息",
                tooltip: "action提交操作完成后的交互文案提示,默认提示‘操作已提交’",
                defModel: scriptProps,
                template: responseHandlerTpl
            });
        }
        this.itemDef = paramsDef;
    }
    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, this.isStep ? {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: 4 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: 20 },
            },
        } : formItemLayout));
        if (this.isStep) {
            return (
                <Row>
                    <Col span={12}>
                        {
                            [
                                formChildrens[0],
                            ]
                        }
                    </Col>
                </Row>
            )
        }
        return (
            <Row>
                <Col span={8}>
                    {
                        [
                            formChildrens[0],
                            formChildrens[1],
                            formChildrens[2],
                            formChildrens[3],
                            formChildrens[4],
                        ]
                    }
                </Col>
                <Col span={8}>
                    {
                        [
                            formChildrens[5],
                            formChildrens[6],
                            formChildrens[13],
                            formChildrens[7],
                            formChildrens[8],

                        ]
                    }
                </Col>
                <Col span={8}>
                    {
                        [
                            formChildrens[14],
                            formChildrens[9],
                            formChildrens[10],
                            formChildrens[11],
                            formChildrens[12]
                        ]
                    }
                </Col>
            </Row>

        )
    };

    render() {

        let formChildrens = this.buildingFormElements();
        return (
            <Form>
                {formChildrens}
            </Form>
        );
    }

}
const ActionSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(ActionSetting);

export default ActionSettingForm;