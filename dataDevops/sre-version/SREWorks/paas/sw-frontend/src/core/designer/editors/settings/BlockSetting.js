/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:48
 * @Description:区块属性设定
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



class BlockSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config } = props;
        let paramsDef = [], { block, title, wrapper = Constants.CARD_WRAPPER_NONE } = config;
        let blockSelectOptions = widgetModel.nodeModel.getBlocks().map(block => {
            let { elementId, label } = block;
            return {
                value: elementId,
                label: label
            }
        });
        paramsDef.push({ type: FormElementType.INPUT, name: 'title', initValue: title, required: false, label: "标题", tooltip: "组件显示的卡片标题" });
        paramsDef.push({
            type: FormElementType.SELECT, name: 'block', initValue: block, required: true, label: "选择区块", inputTip: "选择展示在此处的区块",
            optionValues: blockSelectOptions
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'wrapper', initValue: wrapper, required: false, label: "包装卡片",
            optionValues: [{ value: Constants.CARD_WRAPPER_DEFAULT, label: '基本' },
            { value: Constants.CARD_WRAPPER_ADVANCED, label: '高级' },
            { value: Constants.CARD_WRAPPER_TITLE_TRANSPARENT, label: '标题透明' },
            { value: Constants.CARD_WRAPPER_TRANSPARENT, label: '透明' },
            { value: Constants.CARD_WRAPPER_NONE, label: '无' }]
        });
        this.itemDef = paramsDef;

    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        return (
            <Row>
                <Col span={8}>
                    {
                        formChildrens
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
const BlockSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(BlockSetting);

export default BlockSettingForm;