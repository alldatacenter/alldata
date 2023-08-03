import React, { useEffect, useState } from 'react';
import {
    Table, Popconfirm, message,
    Tabs,
    TabsProps,
} from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import querystring from 'querystring';
import { useAddEditJobsModal } from 'src/view/Main/HomeDetail/Jobs/useAddEditJobsModal';
import { TWarnMetricTableData, TWarnMetricTableItem } from '@/type/warning';
import { GoBack } from '@/component';
import { $http } from '@/http';
import { useMount } from '@/common';
import Notification from '../SLAsSetting/Notification';
import store from '@/store';
import { defaultRender } from '@/utils/helper';

const Index = () => {
    const intl = useIntl();
    const [loading, setLoading] = useState(false);
    const { Render: RenderJobsModal, show: showJobsModal } = useAddEditJobsModal({
        title: intl.formatMessage({ id: 'jobs_tabs_title' }),
        afterClose() {
            getData();
        },
    });
    const [qs] = useState<any>(querystring.parse(window.location.href.split('?')[1] || ''));
    const [tableData, setTableData] = useState<TWarnMetricTableData>({ list: [], total: 0 });
    const [pageParams, setPageParams] = useState({
        pageNumber: 1,
        pageSize: 10,
    });
    const getData = async () => {
        try {
            setLoading(true);
            const res = (await $http.get('/sla/job/page', {
                ...pageParams,
                slaId: qs.slaId,
            })) || [];
            setTableData({
                list: res?.records || [],
                total: res?.total || 0,
            });
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    useMount(() => {
        getData();
    });
    useEffect(() => {
        getData();
    }, [pageParams]);
    const onChange = ({ current, pageSize }: any) => {
        setPageParams({
            pageNumber: current,
            pageSize,
        });
    };
    const onEdit = (record: TWarnMetricTableItem) => {
        store.dispatch({
            type: 'save_datasource_modeType',
            payload: record.type === 'DATA_QUALITY' ? 'quality' : 'comparison',
        });
        showJobsModal({
            slaId: qs.slaId,
            record: { ...record, id: record.jobId },
        });
    };
    const onDelete = async (record: TWarnMetricTableItem) => {
        try {
            setLoading(true);
            await $http.delete(`/job/${record.id}`);
            message.success('Delete Success');
            getData();
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const columns: ColumnsType<TWarnMetricTableItem> = [
        {
            title: intl.formatMessage({ id: 'sla_rule_job_name' }),
            dataIndex: 'jobName',
            fixed: 'left',
            key: 'jobName',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'dv_metric_database' }),
            dataIndex: 'databaseName',
            key: 'databaseName',
            width: 200,
            render: (text: string) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'dv_metric_table' }),
            dataIndex: 'tableName',
            key: 'tableName',
            width: 200,
            render: (text: string) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'dv_metric_column' }),
            dataIndex: 'columnName',
            key: 'columnName',
            width: 200,
            render: (text: string) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'jobs_updater' }),
            dataIndex: 'updateBy',
            key: 'updateBy',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'jobs_update_time' }),
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_action' }),
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 160,
            render: (text: string, record: TWarnMetricTableItem) => (
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
    const items: TabsProps['items'] = [
        {
            key: '1',
            label: intl.formatMessage({ id: 'warn_association_rule_jobs' }),
            children: <Table<TWarnMetricTableItem>
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
            />,
        },
        {
            key: '2',
            label: intl.formatMessage({ id: 'warn_notice' }),
            children: <Notification />,
        },
    ];
    return (
        <div
            className="dv-page-paddinng"
            style={
                {
                    padding: '20px 0px 20px 0px',
                }
            }
        >
            <div className="dv-flex-between" style={{ textAlign: 'right', marginBottom: 10, paddingTop: 10 }}>
                <span>
                    <GoBack />
                    <span style={{ fontWeight: 500, marginLeft: 20, fontSize: 16 }}>{qs.slaName}</span>
                    <div style={{ fontSize: 14, marginLeft: 15, display: 'inline-block' }}>
                        {(intl.formatMessage({ id: 'warn_monitor_tip' }) || '').replace(/\{\{data\}\}/g, (qs.slaName || '') as string)}

                    </div>
                </span>
                {/* <Button type="primary" onClick={() => { onSettings(); }}>
                    {intl.formatMessage({ id: 'common_settings' })}
                </Button> */}
            </div>
            {/* <div style={{ fontSize: 14, marginLeft: 15 }}>{(intl.formatMessage({ id: 'warn_monitor_tip' }) || '').replace(/\{\{data\}\}/g, (qs.slaName || '') as string)}</div> */}
            {/* <Divider style={{ marginBottom: '0px' }} /> */}
            <Tabs defaultActiveKey="1" items={items} style={{ marginTop: 10 }} />
            {/* <JobList datasourceId="14" isWarning /> */}

            <RenderJobsModal />
        </div>
    );
};

export default Index;
