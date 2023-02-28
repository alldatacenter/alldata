import React, { useState } from 'react';
import {
    Table, Popconfirm, Button, Form, message,
} from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import { TNoticeTableData, TNoticeTableItem } from '@/type/warning';
import { SearchForm } from '@/component';
import { useMount } from '@/common';
import { useSelector } from '@/store';
import { $http } from '@/http';
import { useNotificationFormModal } from './useNotificationFormModal';

const Index = () => {
    const intl = useIntl();
    const form = Form.useForm()[0];
    const [loading, setLoading] = useState(false);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const [tableData, setTableData] = useState<TNoticeTableData>({ list: [], total: 0 });
    const { Render: RenderNotificationFormModal, show } = useNotificationFormModal({
        afterClose() {
            getData();
        },
    });
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
            const res = (await $http.get('/sla/notification/page', {
                workspaceId,
                ...pageParams,
                ...(values || form.getFieldsValue()),
            })) || [];
            setTableData({
                list: res?.records || [],
                total: res.total || 0,
            });
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    useMount(() => {
        getData();
    });
    const onSearch = (_values: any) => {
        setPageParams({ ...pageParams, pageNumber: 1 });
        getData({
            ..._values,
            pageNumber: 1,
        });
    };
    const onEdit = (record: TNoticeTableItem) => {
        show(record);
    };
    const onDelete = async (record: TNoticeTableItem) => {
        try {
            setLoading(true);
            await $http.delete(`sla/notification/${record.id}`);
            getData();
            message.success(intl.formatMessage({ id: 'common_success' }));
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const columns: ColumnsType<TNoticeTableItem> = [
        {
            title: intl.formatMessage({ id: 'warn_setting_notice_sender' }),
            dataIndex: 'senderName',
            fixed: 'left',
            key: 'senderName',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_type' }),
            dataIndex: 'type',
            key: 'type',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'jobs_updater' }),
            dataIndex: 'updateBy',
            key: 'updateBy',
            width: 100,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'jobs_update_time' }),
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 180,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_action' }),
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 120,
            render: (text: string, record: TNoticeTableItem) => (
                <>
                    <a style={{ marginRight: 5 }} onClick={() => { onEdit(record); }}>{intl.formatMessage({ id: 'common_edit' })}</a>
                    <Popconfirm
                        title={intl.formatMessage({ id: 'common_delete_tip' })}
                        onConfirm={() => { onDelete(record); }}
                        okText={intl.formatMessage({ id: 'common_Ok' })}
                        cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                    >
                        <a>{intl.formatMessage({ id: 'common_delete' })}</a>
                    </Popconfirm>

                </>
            ),
        },
    ];
    return (
        <div className="dv-page-paddinng">
            <div style={{ paddingTop: '20px' }}>
                <div className="dv-flex-between">
                    <SearchForm form={form} onSearch={onSearch} placeholder={intl.formatMessage({ id: 'common_search' })} />
                    <div>
                        <Button
                            type="primary"
                            style={{ marginRight: 15 }}
                            onClick={() => {
                                show(null);
                            }}
                        >
                            {intl.formatMessage({ id: 'warn_setting_notice_add' })}

                        </Button>
                    </div>
                </div>
            </div>
            <Table<TNoticeTableItem>
                loading={loading}
                size="middle"
                rowKey="id"
                columns={columns}
                dataSource={tableData.list || []}
                onChange={onChange}
                pagination={{
                    size: 'small',
                    total: tableData.total,
                    showSizeChanger: true,
                    current: pageParams.pageNumber,
                    pageSize: pageParams.pageSize,
                }}
            />
            <RenderNotificationFormModal />
        </div>
    );
};

export default Index;
