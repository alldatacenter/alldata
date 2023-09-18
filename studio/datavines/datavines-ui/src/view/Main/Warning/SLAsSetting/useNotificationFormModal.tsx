import React, { useState, useRef } from 'react';
import {
    Input, ModalProps, Form, FormInstance, Radio, message,
} from 'antd';
import { useIntl } from 'react-intl';
import querystring from 'querystring';
import {
    useModal, useContextModal, useImmutable, FormRender, IFormRenderItem, IFormRender, usePersistFn, useMount, CustomSelect, useLoading,
} from '@/common';
import { $http } from '@/http';
import { useSelector } from '@/store';
import { NoticeDynamicItem } from '@/type/Notification';
import { pickProps } from '@/utils';

type InnerProps = {
    form: FormInstance | undefined
}

const Inner = ({ form }: InnerProps) => {
    const intl = useIntl();
    const { data } = useContextModal();
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const [typeSource, setTypeSource] = useState<{label: string, value: string}[]>([]);
    const [dynamicMeta, setDynamicMeta] = useState<IFormRenderItem[]>([]);
    const [senderList, setSenderList] = useState<any[]>([]);
    useMount(async () => {
        try {
            const res: string[] = (await $http.get('/sla/plugin/support')) || [];
            const source = res.map((item) => ({
                label: item,
                value: item,
            }));
            setTypeSource(source);
            if (data) {
                const type = data.type || 'email';
                await typeChange(type);
                const config = data.config ? JSON.parse(data.config) : {};
                form?.setFieldsValue({
                    type,
                    senderId: data.senderId,
                    ...config,
                });
            }
        } catch (error) {
        }
    });
    const typeChange = async (type: string) => {
        try {
            const res = (await $http.get(`/sla/notification/config/${type}`)) || [];
            const params = {
                type,
                workspaceId,
            };
            const list = (await $http.get('/sla/sender/list', params)) || [];
            setSenderList(list);
            if (res) {
                const $res = JSON.parse(res) as NoticeDynamicItem[];
                setDynamicMeta($res.map((item) => {
                    const object = {
                        label: item.title,
                        name: item.field,
                        initialValue: item.value || undefined,
                        rules: (item.validate || []).map(($item) => (pickProps($item, ['message', 'required']))),
                    };
                    if (item.type === 'radio') {
                        return {
                            ...object,
                            widget: (
                                <Radio.Group>
                                    {
                                        (item.options || []).map((sub) => <Radio key={`${sub.value}`} value={sub.value} disabled={sub.disabled}>{sub.label}</Radio>)
                                    }
                                </Radio.Group>
                            ),
                        };
                    }
                    return {
                        ...object,
                        widget: <Input autoComplete="off" />,
                    };
                }));
            }
        } catch (error) {
        }
    };
    const schema: IFormRender = {
        name: 'notice-form',
        layout: 'vertical',
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: intl.formatMessage({ id: 'warn_sLAs_type' }),
                name: 'type',
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <CustomSelect
                    onChange={typeChange}
                    source={typeSource}
                    style={{ width: '100%' }}
                />,
            },
            {
                label: intl.formatMessage({ id: 'warn_setting_notice_sender' }),
                name: 'senderId',
                dependencies: ['type'],
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                onVisible() {
                    const typeValue = form?.getFieldValue('type');
                    if (typeValue) {
                        return true;
                    }
                    return false;
                },
                widget: <CustomSelect
                    sourceLabelMap="name"
                    sourceValueMap="id"
                    source={senderList}
                    style={{ width: '100%' }}
                />,
            },
            ...dynamicMeta,
        ],
    };
    return <FormRender {...schema} form={form} />;
};

export const useNotificationFormModal = (options: ModalProps) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const setLoading = useLoading();
    const [editInfo, setEditInfo] = useState<any>(null);
    const editRef = useRef<any>(null);
    editRef.current = editInfo;
    const [qs] = useState(querystring.parse(window.location.href.split('?')[1] || ''));
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const onOk = usePersistFn(async () => {
        form.validateFields().then(async (values) => {
            try {
                setLoading(true);
                const { senderId, type, ...rest } = values;
                const params = {
                    workspaceId,
                    type,
                    senderId,
                    slaId: qs.slaId,
                    config: JSON.stringify(rest),
                };
                if (editRef.current?.id) {
                    await $http.put('/sla/notification', { ...params, id: editRef.current?.id });
                } else {
                    await $http.post('/sla/notification', params);
                }
                message.success(intl.formatMessage({ id: 'common_success' }));
                hide();
            } catch (error) {
                console.log(error);
            } finally {
                setLoading(false);
            }
        }).catch((err) => {
            console.log(err);
        });
    });
    const {
        Render, show, hide, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'warn_setting_notice_add' }),
        onOk,
        width: 600,
        ...(options || {}),
    });
    return {
        Render: useImmutable(() => (<Render><Inner form={form} /></Render>)),
        show(data: any) {
            setEditInfo(data);
            show(data);
        },
        ...rest,
    };
};
