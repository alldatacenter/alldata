/**
 * Created by caoshuaibiao on 2019/1/22.
 * 映射表单
 */
import React, { PureComponent } from 'react';
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Card, Row, Col } from "antd";
import FormElementFactory from '../FormBuilder/FormElementFactory'

const JSONFormItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 2 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 14 },
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

class ParameterMappingForm extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            expert: false
        }
        let parameter = this.props.parameter;
        parameter.validate = this.validate;
    }


    validate = () => {
        let pass = false, { parameter } = this.props;
        this.props.form.validateFieldsAndScroll((err, values) => {
            if (!err) {
                pass = values;
                parameter.updateMetaData(values);
            }
        });
        return pass
    };

    buildingFormElements = () => {
        let formChildrens = [], { colCount } = this.props;
        let { parameter, form, mode, formItemLayout } = this.props, itemDefs = null;
        let { expert } = this.state;
        let type = form.getFieldValue('type');
        if (type !== parameter.type && type) {
            parameter.changeType(type);
            form.setFieldsValue({ "defaultValue": [] })
        }
        if (mode === 'instance') {
            itemDefs = parameter.getInstanceParamDef();
        } else {
            itemDefs = parameter.getMetaParamDef();
        }
        itemDefs.forEach(function (item) {
            let layout = formItemLayout || defaultFormItemLayout;
            if (item.type === 86 && colCount === 3) {
                item.height = 370;
                layout = JSONFormItemLayout;
            }
            formChildrens.push(FormElementFactory.createFormItem(item, form, layout));
        });
        if (colCount === 3) {
            return (
                <div style={{ marginTop: -10 }}>
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
                                    formChildrens[3],
                                    formChildrens[4],
                                    formChildrens[5],
                                    formChildrens[6],
                                    formChildrens[11],
                                ]
                            }
                        </Col>
                        <Col span={8}>
                            {
                                [
                                    formChildrens[9],
                                    formChildrens[10],
                                    formChildrens[12],
                                    formChildrens[13],
                                ]
                            }
                            <div style={{ textAlign: "right", marginRight: 30 }}>
                                <a onClick={() => this.setState({ expert: !expert })}>高级配置<LegacyIcon
                                    type={expert ? "caret-up" : "caret-down"} /></a>
                            </div>
                        </Col>
                    </Row>
                    {expert && <Row>
                        <Col style={{ textAlign: "left" }} span={22}>
                            {
                                [
                                    formChildrens[14],
                                ]
                            }
                        </Col>
                    </Row>}
                </div>
            );
        }
        return formChildrens;

    };

    render() {

        let formChildrens = this.buildingFormElements();
        return (
            <Card bordered={false} size="small">
                <Form>
                    {formChildrens}
                </Form>
            </Card>
        );
    }

    componentDidUpdate() {
        if (!this.props.parameter.validate) this.props.parameter.validate = this.validate;
    }
}
export default Form.create()(ParameterMappingForm);