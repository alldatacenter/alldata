/**
 * Created by caoshuaibiao on 2021/4/12.
 * 表单项编辑器，每一个表单项是一个parameter模型定义
 */

import React, { PureComponent } from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Card, Row, Col, Collapse } from "antd";
import FormElementFactory from '../../../components/FormBuilder/FormElementFactory';
import DataSourceEditor from "./DataSourceEditor";
import Parameter from '../../../components/ParameterMappingBuilder/Parameter';
import FormElementType from '../../../components/FormBuilder/FormElementType';
import _ from "lodash";

const { Panel } = Collapse;
const advancedItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 5 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 19 },
    },
};
const defaultFormItemLayout = {
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

class FormItemEditor extends PureComponent {

    constructor(props) {
        super(props);
        let { parameter } = props;
        this.state = { parameter: new Parameter(parameter) };
    }


    //为了更新order
    componentWillReceiveProps(nextProps, nextContext) {
        if (!_.isEqual(this.props, nextProps)) {
            this.setState({
                parameter: new Parameter(nextProps.parameter)
            })
        }
    }


    buildingFormElements = () => {
        let formChildrens = [];
        let { form, formItemLayout } = this.props, itemDefs = null, { parameter } = this.state;
        let type = form.getFieldValue('type');
        if (type !== parameter.type && type) {
            parameter.changeType(type);
            form.setFieldsValue({ "defaultValue": [] })
        }
        itemDefs = parameter.getMetaParamDef();
        itemDefs.forEach(function (item) {
            let layout = formItemLayout || defaultFormItemLayout;
            formChildrens.push(FormElementFactory.createFormItem(item, form, layout));
        });

        return (
            <div>
                <Row>
                    <Col span={8}>
                        {
                            [
                                formChildrens[0],
                                formChildrens[1],
                                formChildrens[2],
                                formChildrens[7],
                                formChildrens[8],
                            ]
                        }
                    </Col>
                    <Col span={8}>
                        {
                            [

                                formChildrens[11],
                                formChildrens[9],
                                formChildrens[10],
                                formChildrens[12],
                                formChildrens[13],
                            ]
                        }
                    </Col>
                    <Col span={8}>
                        {
                            [
                                formChildrens[3],
                                formChildrens[4],
                                formChildrens[5],
                                formChildrens[15],
                            ]
                        }
                    </Col>
                </Row>
            </div>

        )
    };

    render() {

        let formChildrens = this.buildingFormElements(), { parameter, onValuesChange, form } = this.props;
        return (
            <Form>
                <Collapse defaultActiveKey={['base', "ds"]} bordered={false}>
                    <Panel header="基础定义" key="base" forceRender={true}>
                        {formChildrens}
                    </Panel>
                    <Panel header="数据源定义" key="ds">
                        <div style={{ width: "50%" }}>
                            <DataSourceEditor value={parameter.dataSourceMeta || {}} onValuesChange={(changedValues, allValues) => {
                                parameter.dataSourceMeta = allValues;
                                onValuesChange && onValuesChange({ dataSourceMeta: allValues }, parameter);
                            }} />
                        </div>
                    </Panel>
                    <Panel header="高级配置" key="advanced">
                        <Row>
                            <Col span={12}>
                                {
                                    FormElementFactory.createFormItem({
                                        type: 83, name: 'defModel', initValue: parameter.defModel, required: false,
                                        label: "参数JSON配置", height: 320, showDiff: false,
                                        tooltip: "表单项一些配置在基础配置中无法体现,需要在此进行定义,具体配置项见文档",
                                        defModel: { height: 320, disableShowDiff: true, mode: "json" }
                                    }, form, advancedItemLayout)
                                }
                            </Col>
                            <Col span={12}>
                                {
                                    FormElementFactory.createFormItem({
                                        type: 83, name: 'effectHandler',
                                        initValue: parameter.effectHandler || 'function effectHandler(nodeParams,formValues){\n  return {}\n}',
                                        required: false,
                                        label: "联动函数",
                                        tooltip: "表单项值更改后,引起其他表单项值更改的处理函数,入参为当前表单所有值及节点参数域,返回值为影响的表单值对象",
                                        defModel: { height: 320, disableShowDiff: true, mode: "javascript" }
                                    },
                                        form, advancedItemLayout)
                                }
                            </Col>
                        </Row>
                    </Panel>
                </Collapse>
            </Form>
        );
    }

}
export default Form.create(
    {
        onValuesChange: (props, changedValues, allValues) => {
            props.onValuesChange && props.onValuesChange(changedValues, allValues.type ? Object.assign({}, props.value, allValues) : null);
        }
    }
)(FormItemEditor);