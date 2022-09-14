/**
 * Created by caoshuaibiao on 2021/1/11.
 * 自定义表单定义器
 */
import React, { PureComponent } from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    message,
    Button,
    Card,
    Tooltip,
    Popover,
    Modal,
    Row,
    Col,
    Radio,
    Spin,
    Collapse,
    Select,
    Input,
} from 'antd';
import FormElementFactory from './FormElementFactory';
import ParameterDefiner from '../ParameterMappingBuilder/ParameterDefiner';

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 2 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
    },
};


class FormEditor extends PureComponent {

    constructor(props) {
        super(props);
        let { parameterDefiner = { bindingParamTree: [], paramMapping: [] } } = this.props;
        parameterDefiner.noBinding = 'noBinding';
        this.parameterDefiner = new ParameterDefiner(parameterDefiner);
        let def = this.parameterDefiner.getDefaultWidgetDef();
        if (!Array.isArray(def)) {
            def = [def];
        }
        this.state = { parameterItems: def };

    }

    componentDidMount() {
        const { onReady } = this.props;
        onReady && onReady(this);
    }

    getParameterDefiner = () => {
        this.parameterDefiner.getParameters().forEach(parameter => {
            parameter.validate && parameter.validate();
        });
        this.parameterDefiner.validate && this.parameterDefiner.validate();
        return JSON.parse(JSON.stringify(this.parameterDefiner));
    };

    buildingFormElements = () => {

        let formChildrens = [], { parameterItems } = this.state;
        let { form } = this.props;
        parameterItems.forEach(function (item) {
            formChildrens.push(FormElementFactory.createFormItem(item, form, formItemLayout));
        });

        return formChildrens;
    };


    render() {
        let formChildrens = this.buildingFormElements();
        return (
            <Card size="small" bordered={false}>
                <Form>
                    {formChildrens}
                </Form>
            </Card>
        );
    }
}
// export default Form.create()(FormEditor);
export default FormEditor;