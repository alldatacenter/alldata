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
import { TUserItem } from '@/type/User';

type Detail = {
    username: string,
    email: string
}

type InnerProps = {
    form: FormInstance,
    detail?: null | TUserItem,
    innerRef?: any
}

export const CreateUserComponent = ({ form, detail, innerRef }: InnerProps) => {
    const intl = useIntl();
    const setBodyLoading = useLoading();
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const schema: IFormRender = {
        name: 'sla-form',
        layout: 'vertical',
        column: 1,
        gutter: 20,
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: intl.formatMessage({ id: 'userName_text' }),
                name: 'username',
                initialValue: detail?.username,
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <Input />,
            },
            {
                label: intl.formatMessage({ id: 'email_text' }),
                name: 'email',
                initialValue: detail?.email,
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
                    await $http.post('/workspace/inviteUser', params);
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

export const useAddUser = (options: ModalProps) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const innerRef = useRef<any>();
    const [editInfo, setEditInfo] = useState<TUserItem | null>(null);
    const editRef = useRef<TUserItem | null>(null);
    editRef.current = editInfo;

    const onOk = usePersistFn(async () => {
        innerRef.current.saveUpdate(hide);
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'workspace_user_invite' }),
        onOk,
        ...(options || {}),
    });
    return {
        Render: useImmutable(() => (<Render><CreateUserComponent innerRef={innerRef} form={form} detail={editRef.current} /></Render>)),
        show(data: any) {
            setEditInfo(data);
            show(data);
        },
        ...rest,
    };
};
