/**
 * @author caoshuaibiao
 * @date 2021/6/30 20:12
 * @Description: 流式布局中行的属性设定
 */

import React from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Row, Col, } from 'antd';
import Constants from "../../framework/model/Constants";
import FormElementType from "../../../components/FormBuilder/FormElementType";
import FormElementFactory from "../../../components/FormBuilder/FormElementFactory";
import uuidv4 from 'uuid/v4';

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 18 },
    },
};



class RowSetting extends React.Component {

    constructor(props) {
        super(props);
        let { row } = props;
        let { spans = "12,12", height, rang = [0, 24] } = row;
        let paramsDef = [];
        paramsDef.push({
            type: 15, name: 'rang', initValue: rang, required: true, label: "布局属性", tooltip: "拖动首尾handle进行列的划分",
            defModel: {
                range: true,
                defaultValue: [0, 24],
                max: 24,
                enableAdd: true,
                addText: "添加列"
            }
        });
        paramsDef.push({ type: FormElementType.INPUT, name: 'height', initValue: height, validateReg: "[0-9]$##只能填写数字,单位像素", required: false, label: "高度", tooltip: "单位px,不填写时按照组件内容大小撑满" });
        this.itemDef = paramsDef;

    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        return (
            <Row>
                <Col span={24}>
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
const RowSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => {
        //通过rang计算出span
        let rang = allValues.rang, spans = [], { row } = props;
        for (let i = 1; i < rang.length; i++) {
            spans.push(rang[i] - rang[i - 1]);
        }
        let { elements = [] } = row, newSpans = [];
        spans.forEach((span, index) => {
            if (span) {
                if (!elements[index]) {
                    elements[index] = {
                        uniqueKey: uuidv4(),
                    };
                }
                newSpans.push(span);
            }
        })
        Object.assign(allValues, {
            spans: newSpans.join(","),
            elements: elements
        })
        props.onValuesChange && props.onValuesChange(changedValues, allValues)
    }
})(RowSetting);

export default RowSettingForm;