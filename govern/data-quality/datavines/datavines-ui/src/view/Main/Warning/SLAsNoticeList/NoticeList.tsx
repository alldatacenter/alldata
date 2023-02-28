import React, { useState } from 'react';
import {
    Table, Button, Form, message,
} from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import { PlusOutlined } from '@ant-design/icons';
import { TWarnTableData, TWarnTableItem } from '@/type/warning';
import { useCreateWidget } from './hooks/CreateWidget';
import { $http } from '@/http';
import { useSelector } from '@/store';
import { SearchForm } from '@/component';
import { useMount, Popconfirm } from '@/common';

const Index = () => {
    const intl = useIntl();
    const form = Form.useForm()[0];
    const [loading, setLoading] = useState(false);
    const { Render: RenderWidgetModal, show } = useCreateWidget({
        afterClose() {
            getData();
        },
    });
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const [tableData, setTableData] = useState<TWarnTableData>({ list: [], total: 0 });
    const [pageParams, setPageParams] = useState({
        pageNumber: 1,
        pageSize: 10,
    });
    const onChange = ({ current, pageSize }: any) => {
        setPageParams({
            pageNumber: current,
            pageSize,
        });
    };
    const getData = async (values: any = null) => {
        try {
            setLoading(true);
            const params = {
                workspaceId,
                ...pageParams,
                ...(values || form.getFieldsValue()),
            };
            const res = (await $http.get('/sla/sender/page', params)) || [];
            setTableData({
                list: res?.records || [],
                total: res?.total || 0,
            });
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const onSearch = (_values: any) => {
        setPageParams({ ...pageParams, pageNumber: 1 });
        getData({
            ..._values,
            pageNumber: 1,
        });
    };
    useMount(() => {
        getData();
    });
    const onEdit = (record: TWarnTableItem) => {
        show(record);
    };
    const onDelete = async (id: any) => {
        try {
            setLoading(true);
            const res = await $http.delete(`sla/sender/${id}`);
            if (res) {
                getData();
                message.success(intl.formatMessage({ id: 'common_success' }));
            } else {
                message.success(intl.formatMessage({ id: 'common_fail' }));
            }
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const columns: ColumnsType<TWarnTableItem> = [
        {
            title: intl.formatMessage({ id: 'warn_widget_name' }),
            dataIndex: 'name',
            key: 'name',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_type' }),
            dataIndex: 'type',
            key: 'type',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_updater' }),
            dataIndex: 'updateBy',
            key: 'updateBy',
            render: (text: string) => <div>{text || '--'}</div>,
        },
        {
            title: intl.formatMessage({ id: 'warn_update_time' }),
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_action' }),
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 100,
            render: (text: string, record: TWarnTableItem) => (
                <>
                    <a onClick={() => { onEdit(record); }}>{intl.formatMessage({ id: 'common_edit' })}</a>
                    <Popconfirm
                        onClick={() => onDelete(record.id)}
                    />
                </>
            ),
        },
    ];
    return (
        <div>
            <div style={{ paddingTop: '0px' }}>
                <div className="dv-flex-between">
                    <SearchForm form={form} onSearch={onSearch} placeholder={intl.formatMessage({ id: 'common_search' })} />
                    <div style={{ textAlign: 'right', marginBottom: 10 }}>
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => { show(null); }}>
                            {intl.formatMessage({ id: 'warn_create_widget' })}
                        </Button>
                    </div>
                </div>
            </div>
            <Table<TWarnTableItem>
                loading={loading}
                size="middle"
                rowKey="id"
                columns={columns}
                dataSource={tableData.list || []}
                onChange={onChange}
                bordered
                pagination={{
                    size: 'small',
                    total: tableData.total,
                    showSizeChanger: true,
                    current: pageParams.pageNumber,
                    pageSize: pageParams.pageSize,
                }}
            />
            <RenderWidgetModal />
        </div>
    );
};

export default Index;
