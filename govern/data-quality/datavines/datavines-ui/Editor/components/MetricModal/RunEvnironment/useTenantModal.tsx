import React, { useState } from 'react';
import { Table, Button, Input } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import { EyeOutlined } from '@ant-design/icons';
import { ModalProps } from 'antd/lib/modal';
import {
    Popconfirm, useMount, useModal, useImmutable, useFormRender, IFormRender, useContextModal, IF,
} from '../../../common';
import useRequest from '../../../hooks/useRequest';
import { useEditorContextState } from '../../../store/editor';

type IndexProps = {
}

type IDataSourceListItem = {
    'id': number,
    'tenant': string,
    'createBy': number,
    'createTime': string,
    'updateBy': number,
    'updateTime': string,
}

const Index: React.FC<IndexProps> = () => {
    const intl = useIntl();
    const { $http } = useRequest();
    const { data } = useContextModal();
    const [context] = useEditorContextState();
    const [pageParams, setpageParams] = useState({ pageNo: 1, pageSize: 10 });
    const [tableData, setTableData] = useState<{list: IDataSourceListItem[], total: number}>({ list: [], total: 0 });
    const {
        Render: RenderModal, show, hide, setModalProps,
    } = useFormRender({
        afterClose() {
            getTenantList();
        },
        onCustomOk: async ({ values, data: $data }) => {
            try {
                if ($data.data) {
                    await $http.put('tenant', { ...values, id: $data.data.id, workspaceId: data.workspaceId });
                } else {
                    await $http.post('tenant', { ...values, workspaceId: data.workspaceId });
                }
                hide();
            } catch (error) {
            }
        },
    });
    const showFormSchemaModal = (record: null | IDataSourceListItem) => {
        const schema: IFormRender = {
            name: 'tenantName',
            layout: 'vertical',
            meta: [
                {
                    label: 'name',
                    name: 'tenant',
                    rules: [
                        { required: true },
                    ],
                    initialValue: record?.tenant,
                    widget: <Input />,
                },
            ],
        };
        show({ schema, data: record });
        setTimeout(() => {
            setModalProps({
                title: intl.formatMessage({ id: `dv_metric_${record ? 'edit' : 'add'}` }),
            });
        });
    };
    const getTenantList = async () => {
        try {
            const tenantList = await $http.get(`tenant/list/${context.workspaceId}`);
            setpageParams({
                pageNo: 1,
                pageSize: 10,
            });
            setTableData({
                list: tenantList || [],
                total: (tenantList || []).length,
            });
        } catch (error) {
        }
    };
    useMount(async () => {
        getTenantList();
    });
    const columns: ColumnsType<IDataSourceListItem> = [
        {
            title: 'tenant',
            dataIndex: 'tenant',
            key: 'tenant',
            width: 160,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: 'createTime',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 140,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: 'updateTime',
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 140,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: '操作',
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 100,
            render: (text: string, record: IDataSourceListItem) => (
                <>
                    <a onClick={() => { showFormSchemaModal(record); }}>{intl.formatMessage({ id: 'dv_metric_edit' })}</a>
                    <IF visible={data?.currentValue !== record.id}>
                        <Popconfirm
                            onClick={async () => {
                                try {
                                    await $http.delete(`tenant/${record.id}`);
                                    getTenantList();
                                } catch (error) {
                                }
                            }}
                        />
                    </IF>
                </>
            ),
        },
    ];
    const pageChange = ({ current, pageSize }: any) => {
        setpageParams({
            pageNo: current,
            pageSize,
        });
    };
    return (
        <>
            <div style={{ textAlign: 'right' }}>
                <Button
                    onClick={
                        () => (showFormSchemaModal(null))
                    }
                >
                    {intl.formatMessage({ id: 'dv_metric_add' })}
                </Button>
            </div>
            <Table<IDataSourceListItem>
                size="middle"
                rowKey="id"
                columns={columns}
                dataSource={tableData.list || []}
                onChange={pageChange}
                pagination={{
                    size: 'small',
                    total: tableData.total,
                    current: pageParams.pageNo,
                    pageSize: pageParams.pageSize,
                }}
            />
            <RenderModal />
        </>

    );
};

export const useTenantModal = (options: ModalProps) => {
    const { Render, ...rest } = useModal<any>({
        title: '',
        width: 800,
        maskClosable: false,
        footer: null,
        ...options,
    });
    return {
        Render: useImmutable(() => (<Render><Index /></Render>)),
        ...rest,
    };
};
