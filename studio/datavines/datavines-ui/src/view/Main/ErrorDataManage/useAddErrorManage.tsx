import React, { useState, useRef } from 'react';
import {
    Input, ModalProps, Form, FormInstance, message,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    useModal, useContextModal, useImmutable, FormRender, IFormRenderItem, IFormRender, usePersistFn, useMount, CustomSelect, useLoading,
} from '@/common';
import { $http } from '@/http';
import { useSelector } from '@/store';
import { errorDynamicItem } from '@/type/errorData';
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
            const res: any[] = (await $http.get('errorDataStorage/type/list')) || [];
            const data = res.map((item) => ({
                label: item.label,
                value: item.key,
            }));
            setTypeSource(data);
            if (record?.id) {
                await typeChange(record.type);
                const configObj = record.param ? JSON.parse(record.param) : {};
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
            const res = (await $http.get(`/errorDataStorage/config/${type}`)) || [];
            if (res) {
                const data = JSON.parse(res) as errorDynamicItem[];
                setDynamicMeta(data.map((item) => {
                    const object = {
                        label: item.title,
                        name: item.field,
                        initialValue: item.value || undefined,
                        rules: (item.validate || []).map(($item) => (pickProps($item, ['message', 'required']))),
                    };
                    return {
                        ...object,
                        widget: <Input disabled={item?.props?.disabled} placeholder={item?.props?.placeholder} />,
                    };
                }));
            }
        } catch (error) {
            setDynamicMeta([]);
        }
    };
    const schema: IFormRender = {
        name: 'notice-form',
        layout: 'vertical',
        column: 1,
        gutter: 20,
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: intl.formatMessage({ id: 'error_table_store_name' }),
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
                label: intl.formatMessage({ id: 'error_table_store_type' }),
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

export const useAddErrorManage = (options: ModalProps) => {
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
                    param: JSON.stringify(rest),
                };
                if (recordRef.current?.id) {
                    await $http.put('/errorDataStorage', { ...params, id: recordRef.current?.id });
                } else {
                    await $http.post('/errorDataStorage', params);
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
        title: intl.formatMessage({ id: 'error_create_btn' }),
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
