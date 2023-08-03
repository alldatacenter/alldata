import React, { useState, useRef } from 'react';
import {
    Input, ModalProps, Form, FormInstance,
} from 'antd';
import {
    useModal, useImmutable, FormRender, IFormRender, usePersistFn, useContextModal,
} from 'src/common';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { getWorkSpaceList } from '@/action/workSpace';
import { useSelector } from '@/store';

type InnerProps = {
    form: FormInstance | undefined
}

const Inner = ({ form }: InnerProps) => {
    const intl = useIntl();
    const { data: workspaceId } = useContextModal();
    const { spaceList } = useSelector((r) => r.workSpaceReducer);
    const workspace = spaceList.find((item) => item.id === workspaceId);
    const schema: IFormRender = {
        name: 'user-create-space',
        layout: 'vertical',
        meta: [
            {
                label: intl.formatMessage({ id: 'create_space_name' }),
                name: 'name',
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required' }),
                    },
                ],
                initialValue: workspaceId ? workspace?.name : undefined,
                widget: <Input autoComplete="off" />,
            },
        ],
    };
    return <FormRender {...schema} form={form} />;
};

export const useAddSpace = (options: ModalProps) => {
    const [form] = Form.useForm() as [FormInstance];
    const [loading, setLoading] = useState(false);
    const loadingRef = useRef(loading);
    loadingRef.current = loading;
    const intl = useIntl();
    const workspaceIdRef = useRef<number | null>();
    const onOk = usePersistFn(() => {
        form.validateFields().then(async (values) => {
            try {
                setLoading(true);
                if (workspaceIdRef.current) {
                    await $http.put('/workspace', { ...values, id: workspaceIdRef.current });
                } else {
                    await $http.post('/workspace', values);
                }
                getWorkSpaceList();
                hide();
            } catch (error: any) {
            } finally {
                setLoading(false);
            }
        }).catch(() => {});
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'create_space' }),
        ...(options || {}),
        confirmLoading: loadingRef.current,
        onOk,
    });
    return {
        hide,
        show(data?: number | null) {
            workspaceIdRef.current = data;
            show(data);
        },
        Render: useImmutable(() => (<Render><Inner form={form} /></Render>)),
        ...rest,
    };
};
