import React, { useState, useRef } from 'react';
import {
    Input, ModalProps, Form, FormInstance, Radio, message,
} from 'antd';
import { useIntl } from 'react-intl';
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
    const { data: record } = useContextModal();
    const [typeSource, setTypeSource] = useState<{label: string, value: string}[]>([]);
    const [dynamicMeta, setDynamicMeta] = useState<IFormRenderItem[]>([]);
    useMount(async () => {
        try {
            const res: string[] = (await $http.get('/sla/plugin/support')) || [];
            const data = res.map((item) => ({
                label: item,
                value: item,
            }));
            setTypeSource(data);
            if (record?.id) {
                await typeChange(record.type);
                const configObj = record.config ? JSON.parse(record.config) : {};
                form?.setFieldsValue({
                    ...configObj,
                    type: record.type,
                    name: record.name,
                });
            }
        } catch (error) {
        }
    });
    const typeChange = async (type: string) => {
        try {
            const res = (await $http.get(`/sla/sender/config/${type}`)) || [];
            if (res) {
                const data = JSON.parse(res) as NoticeDynamicItem[];
                setDynamicMeta(data.map((item) => {
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
                label: intl.formatMessage({ id: 'warn_sLAs_name' }),
                name: 'name',
                rules: [
                    {
                        required: true,
                        message: intl.formatMessage({ id: 'common_required_tip' }),
                    },
                ],
                widget: <Input autoComplete="off" />,
            },
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
                />,
            },
            ...dynamicMeta,
        ],
    };
    return <FormRender {...schema} form={form} />;
};

export const useCreateWidget = (options: ModalProps) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const setLoading = useLoading();
    const recordRef = useRef<any>(null);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const onOk = usePersistFn(async () => {
        form.validateFields().then(async (values) => {
            try {
                setLoading(true);
                const { type, name, ...rest } = values;
                const params = {
                    workspaceId,
                    type,
                    name,
                    config: JSON.stringify(rest),
                };
                if (recordRef.current?.id) {
                    await $http.put('/sla/sender', { ...params, id: recordRef.current?.id });
                } else {
                    await $http.post('/sla/sender', params);
                }
                message.success(intl.formatMessage({ id: 'common_success' }));
                hide();
            } catch (error) {
            } finally {
                setLoading(false);
            }
        }).catch((err) => {
            console.log(err);
        });
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: intl.formatMessage({ id: 'warn_create_widget' }),
        onOk,
        width: 600,
        ...(options || {}),
    });
    return {
        Render: useImmutable(() => (<Render><Inner form={form} /></Render>)),
        show(record: any) {
            recordRef.current = record;
            show(record);
        },
        ...rest,
    };
};
