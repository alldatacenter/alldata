import React, { useRef, useImperativeHandle } from 'react';
import { Form, FormInstance } from 'antd';
import { ModalProps } from 'antd/lib/modal';
import FormRender, { IFormRender } from '../FormRender';
import useImmutable from '../useImmutable';
import useModal, { useContextModal } from '../useModal';

type InnerProps = {
    innerRef: {
        current: {
            form: FormInstance,
            getContextData: () => any
        }
    }
}

const Inner = ({ innerRef }: InnerProps) => {
    const { data } = useContextModal();
    const [form] = Form.useForm();
    const schema = (data?.schema || {}) as IFormRender;
    useImperativeHandle(innerRef, () => ({
        form,
        getContextData() {
            return data;
        },
    }));

    return <FormRender form={form} {...schema} />;
};

interface TUseFormRender extends ModalProps {
    onCustomOk?: (obj: { values: Record<string, any>, data: any}) => any;
}

const useFormRender = (options: TUseFormRender) => {
    const innerRef:InnerProps['innerRef'] = useRef<any>();
    const { Render, ...rest } = useModal<any>({
        title: '',
        width: 600,
        maskClosable: false,
        ...options,
        onOk: () => {
            innerRef.current.form.validateFields().then((values) => {
                if (options.onCustomOk) {
                    options.onCustomOk({
                        values,
                        data: innerRef.current.getContextData(),
                    });
                }
            }).catch(() => {});
        },
    });
    return {
        Render: useImmutable(() => (<Render><Inner innerRef={innerRef} /></Render>)),
        ...rest,
    };
};

export default useFormRender;
