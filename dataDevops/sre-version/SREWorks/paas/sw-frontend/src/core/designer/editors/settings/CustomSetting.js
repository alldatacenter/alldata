/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:49
 * @Description:组件Schema生成属性表单定义
 */
import React from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import './formCommon.less'
import { Row, Col, } from 'antd';
import Constants from "../../../framework/model/Constants";
import FormElementType from "../../../../components/FormBuilder/FormElementType";
import FormElementFactory from "../../../../components/FormBuilder/FormElementFactory";
import * as util from "../../../../utils/utils";

const customFormItemLayout = {
    labelCol: {
        xs: { span: 8 },
        sm: { span: 6 },
        md: { span: 4 },
    },
    wrapperCol: {
        xs: { span: 16 },
        sm: { span: 18 },
        md: { span: 20 },
    },
};



class CustomSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config, widgetMeta } = props;
        let paramsDef = [];
        let configSchema = widgetMeta.configSchema.schema;
        //增加定义期运行时变量替换功能，后续根据需求添加其他运行时变量
        let runtimeParamsSet = {
            "__select_blocks__": widgetModel.nodeModel.getBlocks().map(block => {
                let { elementId, label } = block;
                return {
                    value: elementId,
                    label: label
                }
            })
        };
        configSchema = util.renderTemplateJsonObject(configSchema, runtimeParamsSet);
        paramsDef.push(
            {
                type: 79, name: 'customConfig', initValue: { config }, required: false, label: " ", mode: "form",
                defModel: {
                    schema: {
                        type: "object",
                        properties: {
                            config: configSchema
                        }
                    }
                }
            }
        );
        this.itemDef = paramsDef;
    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, customFormItemLayout));
        return (
            <Row>
                <Col span={22}>
                    {
                        [

                            formChildrens[0],
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
const CustomSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(allValues.customConfig.config, allValues.customConfig.config)
})(CustomSetting);

export default CustomSettingForm;