import React, { useRef, useState, useImperativeHandle } from 'react';
import {
    Input, ModalProps, Form, FormInstance, message,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    useModal, useImmutable, FormRender, IFormRender, usePersistFn, useUnMount,
} from '@/common';

type InnerProps = {
    form: FormInstance,
    name?: string | null,
    innerRef?: any
}

export const MetricNameComponent = ({ form, name, innerRef }: InnerProps) => {
    const intl = useIntl();
    const schema: IFormRender = {
        name: 'form',
        layout: 'vertical',
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: '',
                name: 'name',
                initialValue: name,
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <Input autoComplete="off" maxLength={255} />,
            },
        ],
    };

    useUnMount(() => {
        form.resetFields();
    });
    useImperativeHandle(innerRef, () => ({
        saveUpdate(callback?: (...args: any[]) => any) {
            form.validateFields().then(async (values) => {
                callback?.(values);
            }).catch((err) => {
                console.log(err);
            });
        },
    }));
    return <FormRender {...schema} form={form} />;
};

type TDataItem = { ok: (...args: any[]) => any, name: string };

export const useMetricName = (options: ModalProps) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const innerRef = useRef<any>();
    const [data, setData] = useState<TDataItem>();
    const dataRef = useRef<TDataItem>();
    dataRef.current = data;

    const onOk = usePersistFn(async () => {
        innerRef.current.saveUpdate((values: { name: string }) => {
            dataRef?.current?.ok(values);
            hide?.();
        });
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'editor_dv_metric_name' }),
        onOk,
        ...(options || {}),
    });
    return {
        Render: useImmutable(() => (<Render><MetricNameComponent innerRef={innerRef} form={form} name={dataRef.current?.name} /></Render>)),
        show($data: TDataItem) {
            setData($data);
            show($data);
        },
        ...rest,
    };
};
