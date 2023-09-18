import React, { useEffect, ReactElement, useMemo } from 'react';
import {
    Row, Col, Form, FormItemProps, FormProps, FormInstance, RowProps, ColProps,
} from 'antd';
import { Gutter } from 'antd/lib/grid/row';

function pickProps(source: Record<string, any>, props: string[]) {
    const target: Record<string, any> = {};
    props.forEach((propName) => {
        if (Object.prototype.hasOwnProperty.call(source, propName)) {
            target[propName] = source[propName];
        }
    });
    return target;
}

const FormPropsPickArray = [
    'className',
    'style',
    'colon',
    'component',
    'fields',
    'initialValues',
    'labelAlign',
    'layout',
    'name',
    'preserve',
    'requiredMark',
    'scrollToFirstError',
    'size',
    'validateMessages',
    'validateTrigger',
    'labelCol',
    'wrapperCol',
    'onFieldsChange',
    'onFinish',
    'onFinishFailed',
    'onValuesChange',
];

const FormItemPropsPickArray = [
    'className',
    'style',
    'colon',
    'dependencies',
    'extra',
    'getValueFromEvent',
    'getValueProps',
    'hasFeedback',
    'help',
    'hidden',
    'htmlFor',
    'initialValue',
    'label',
    'labelAlign',
    'labelCol',
    'messageVariables',
    'name',
    'normalize',
    'noStyle',
    'preserve',
    // 'required',
    'rules',
    'shouldUpdate',
    'tooltip',
    'trigger',
    'validateFirst',
    'validateStatus',
    'validateTrigger',
    'valuePropName',
    'wrapperCol',
];

export interface IFormRenderItem extends FormItemProps {
    render?: (...args: any[]) => any;
    onVisible?: (...args: any[]) => boolean,
    widget?: React.ReactNode
}
type TNoopFunction = (...args: any) => any;
export interface IFormRender extends FormProps {
    form?: FormInstance,
    meta?: IFormRenderItem[],
    formProps?: FormProps,
    formItemProps?: FormItemProps,
    onMount?: TNoopFunction,
    RowProps?: RowProps, // column > 1
    ColProps?: ColProps, // column > 1
    column?: number,
    gutter?: Gutter | [Gutter, Gutter],
    hasForm?: boolean;
}

const FormRender: React.FC<IFormRender> = (props) => {
    const {
        onMount, meta = [], gutter,
    } = props;
    const column = (props.column || 1) as number;
    const [form] = (props.form ? useMemo(() => [props.form], [props.form]) : Form.useForm()) as [FormInstance];
    const formProps = {
        ...(pickProps(props, FormPropsPickArray)),
        ...(props.formProps || {}),
    };
    useEffect(() => {
        if (typeof onMount === 'function') {
            onMount({ form });
        }
        return () => {
            form.resetFields();
        };
    }, []);

    const renderElement = (element: IFormRenderItem, index: number) => {
        const { render, widget } = element;
        const formItemProps = {
            ...(props.formItemProps || {}),
            ...(pickProps(element, FormItemPropsPickArray)),
        };
        if (render) {
            return (
                <React.Fragment key={(element.name || index) as string}>
                    {render({ formItemProps, form, element })}
                </React.Fragment>
            );
        }
        if (!widget) {
            return null;
        }
        const getFormItem = () => {
            const Comp = (
                <React.Fragment key={(element.name || index) as string}>
                    <Form.Item {...formItemProps}>
                        {widget}
                    </Form.Item>
                </React.Fragment>
            );
            if ((element.dependencies || []).length > 0) {
                return (
                    <Form.Item noStyle dependencies={element.dependencies || []}>
                        {() => {
                            if (typeof element.onVisible === 'function') {
                                if (element.onVisible()) {
                                    return Comp;
                                }
                                return null;
                            }
                            return null;
                        }}
                    </Form.Item>
                );
            }
            return Comp;
        };
        return getFormItem();
    };

    const renderLayout = (elements: any[]) => {
        if (column === 1) {
            return elements;
        }
        const rows: ReactElement[] = [];
        const colspan = 24 / column;
        for (let i = 0; i < elements.length; i += column) {
            const cols: ReactElement[] = [];
            for (let j = 0; j < column; j += 1) {
                cols.push(
                    <Col key={j} span={colspan} {...(props.ColProps || {})}>
                        {elements[i + j]}
                    </Col>,
                );
            }
            rows.push(
                <Row key={i} gutter={gutter} {...(props.RowProps || {})}>
                    {cols}
                </Row>,
            );
        }
        return rows;
    };
    return (
        <Form {...formProps} form={form}>
            {
                renderLayout(meta?.map(renderElement))
            }
        </Form>
    );
};

export default FormRender;
