import React, { useState } from 'react';
import {
    Table, Button, message, Form,
} from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { PlusOutlined } from '@ant-design/icons';
import { TWarnSLATableItem } from '@/type/warning';
import { useCreateSLAs } from './hooks/CreateSLAs';
import { useMount, Popconfirm } from '@/common';
import { SearchForm } from '@/component';
import { $http } from '@/http';
import { useSelector } from '@/store';

const Index = () => {
    const [loading, setLoading] = useState(false);
    const intl = useIntl();
    const form = Form.useForm()[0];
    const history = useHistory();
    const match = useRouteMatch();
    const { Render: RenderSLASModal, show } = useCreateSLAs({
        afterClose() {
            getData();
        },
    });
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const [tableData, setTableData] = useState<{ list: TWarnSLATableItem[], total: number}>({ list: [], total: 0 });
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
            const res = (await $http.get('/sla/page', params)) || [];
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
    const onGoMonitor = (record: TWarnSLATableItem) => {
        history.push(`${match.path}/SLAs?slaName=${decodeURIComponent(record.name as string)}&slaId=${record.id}`);
    };
    const onEdit = (record: TWarnSLATableItem) => {
        show(record);
    };
    const onDelete = async (id: number) => {
        try {
            setLoading(true);
            await $http.delete(`/sla/${id}`);
            getData();
            message.success(intl.formatMessage({ id: 'common_success' }));
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const columns: ColumnsType<TWarnSLATableItem> = [
        {
            title: intl.formatMessage({ id: 'sla_name' }),
            dataIndex: 'name',
            key: 'name',
            render: (text: string, record) => {
                if (!text) {
                    return null;
                }
                return (
                    <a onClick={() => {
                        onGoMonitor(record);
                    }}
                    >
                        {text}
                    </a>
                );
            },
        },
        {
            title: intl.formatMessage({ id: 'common_desc' }),
            dataIndex: 'description',
            key: 'description',
            render: (text: string) => <div>{text || '--'}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_updater' }),
            dataIndex: 'updater',
            key: 'updater',
            render: (text: string) => <div>{text || '--'}</div>,
        },
        {
            title: intl.formatMessage({ id: 'warn_update_time' }),
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (text: string) => <div>{text || '--'}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_action' }),
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 100,
            render: (text: string, record: TWarnSLATableItem) => (
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
                        <Button
                            type="primary"
                            onClick={() => { show(null); }}
                            icon={<PlusOutlined />}
                        >
                            {intl.formatMessage({ id: 'warn_create_SLAs' })}
                        </Button>
                    </div>
                </div>
            </div>

            <Table<TWarnSLATableItem>
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
            <RenderSLASModal />
        </div>
    );
};

export default Index;
