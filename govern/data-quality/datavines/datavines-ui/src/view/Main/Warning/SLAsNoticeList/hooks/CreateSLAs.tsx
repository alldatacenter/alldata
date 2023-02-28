import React, { useRef, useState, useImperativeHandle } from 'react';
import {
    Input, ModalProps, Form, FormInstance, message,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    useModal, useImmutable, FormRender, IFormRender, usePersistFn, useLoading,
} from '@/common';
import { $http } from '@/http';
import { useSelector } from '@/store';
import { TWarnSLATableItem } from '@/type/warning';

type InnerProps = {
    form: FormInstance,
    detail?: TWarnSLATableItem | null,
    innerRef?: any
}

export const CreateSLAsComponent = ({ form, detail, innerRef }: InnerProps) => {
    const intl = useIntl();
    const setBodyLoading = useLoading();
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const schema: IFormRender = {
        name: 'sla-form',
        layout: 'horizontal',
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: intl.formatMessage({ id: 'warn_SLAs_name' }),
                name: 'name',
                initialValue: detail?.name,
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <Input />,
            },
            {
                label: intl.formatMessage({ id: 'common_desc' }),
                name: 'description',
                initialValue: detail?.description,
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <Input />,
            },
        ],
    };
    useImperativeHandle(innerRef, () => ({
        saveUpdate(hide?: () => any) {
            form.validateFields().then(async (values) => {
                try {
                    setBodyLoading(true);
                    const params = {
                        workspaceId,
                        ...values,
                    };
                    if (detail && detail.id) {
                        await $http.put('/sla', { ...params, id: detail.id });
                    } else {
                        await $http.post('/sla', params);
                    }
                    message.success(intl.formatMessage({ id: 'common_success' }));
                    if (hide) {
                        hide();
                    }
                } catch (error) {
                } finally {
                    setBodyLoading(false);
                }
            }).catch((err) => {
                console.log(err);
            });
        },
    }));
    return <FormRender {...schema} form={form} />;
};

export const useCreateSLAs = (options: ModalProps) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const innerRef = useRef<any>();
    const [editInfo, setEditInfo] = useState<TWarnSLATableItem | null>(null);
    const editRef = useRef<TWarnSLATableItem | null>(null);
    editRef.current = editInfo;

    const onOk = usePersistFn(async () => {
        innerRef.current.saveUpdate(hide);
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'warn_create_SLAs' }),
        onOk,
        ...(options || {}),
    });
    return {
        Render: useImmutable(() => (<Render><CreateSLAsComponent innerRef={innerRef} form={form} detail={editRef.current} /></Render>)),
        show(data: TWarnSLATableItem | null) {
            setEditInfo(data);
            show(data);
        },
        ...rest,
    };
};
