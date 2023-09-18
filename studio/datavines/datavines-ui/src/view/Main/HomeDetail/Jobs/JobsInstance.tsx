/* eslint-disable react/no-children-prop */
import React, { useState } from 'react';
import { Table, Form } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { useIntl } from 'react-intl';
import querystring from 'querystring';
import { TJobsInstanceTableData, TJobsInstanceTableItem } from '@/type/JobsInstance';
import { Title, SearchForm } from '@/component';
import { useMount, IF } from '@/common';
import { $http } from '@/http';
import { defaultRender } from '@/utils/helper';
import { useLogger } from './useLogger';
import { useInstanceErrorDataModal } from './useInstanceErrorDataModal';
import { useInstanceResult } from './useInstanceResult';

const JobsInstance = () => {
    const intl = useIntl();
    const form = Form.useForm()[0];
    const [loading, setLoading] = useState(false);
    const { Render: RenderErrorDataModal, show: showErrorDataModal } = useInstanceErrorDataModal({});
    const { Render: RenderResultModal, show: showResultModal } = useInstanceResult({});
    const { Render: RenderLoggerModal, show: showLoggerModal } = useLogger({});
    const [tableData, setTableData] = useState<TJobsInstanceTableData>({ list: [], total: 0 });
    const [pageParams, setPageParams] = useState({
        pageNumber: 1,
        pageSize: 10,
    });
    const [qs] = useState(querystring.parse(window.location.href.split('?')[1] || ''));
    const getData = async (values?: any, $pageParams?: any) => {
        try {
            setLoading(true);
            const res = (await $http.get('/job/execution/page', {
                jobId: qs.jobId,
                ...($pageParams || pageParams),
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
    const onChange = ({ current, pageSize }: any) => {
        setPageParams({
            pageNumber: current,
            pageSize,
        });
        getData(null, {
            pageNumber: current,
            pageSize,
        });
    };
    const onStop = async (record: TJobsInstanceTableItem) => {
        try {
            setLoading(true);
            await $http.delete(`job/execution/kill/${record.id}`);
            getData();
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const onLog = (record: TJobsInstanceTableItem) => {
        showLoggerModal(record);
    };
    const onResult = (record: TJobsInstanceTableItem) => {
        showResultModal(record);
    };
    const onErrorData = (record: TJobsInstanceTableItem) => {
        showErrorDataModal(record);
    };
    const columns: ColumnsType<TJobsInstanceTableItem> = [
        {
            title: intl.formatMessage({ id: 'jobs_task_name' }),
            dataIndex: 'name',
            key: 'name',
            width: 300,
            render: (text) => defaultRender(text, 300),
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_type' }),
            dataIndex: 'jobType',
            key: 'jobType',
            width: 140,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_status' }),
            dataIndex: 'status',
            key: 'status',
            width: 140,
            render: (text: string) => <div>{text}</div>,
        },
        {
            title: intl.formatMessage({ id: 'jobs_update_time' }),
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 160,
            render: (text: string) => <div>{text || '--'}</div>,
        },
        {
            title: intl.formatMessage({ id: 'common_action' }),
            fixed: 'right',
            key: 'right',
            dataIndex: 'right',
            width: 240,
            render: (text: string, record: TJobsInstanceTableItem) => (
                <>
                    <IF visible={record.status === 'submitted' || record.status === 'running'}>
                        <a style={{ marginRight: 5 }} onClick={() => { onStop(record); }}>{intl.formatMessage({ id: 'jobs_task_stop_btn' })}</a>
                    </IF>
                    <a style={{ marginRight: 5 }} onClick={() => { onLog(record); }}>{intl.formatMessage({ id: 'jobs_task_log_btn' })}</a>
                    <a style={{ marginRight: 5 }} onClick={() => { onResult(record); }}>{intl.formatMessage({ id: 'jobs_task_result' })}</a>
                    <a style={{ marginRight: 5 }} onClick={() => { onErrorData(record); }}>{intl.formatMessage({ id: 'jobs_task_error_data' })}</a>
                </>
            ),
        },
    ];
    return (
        <div
            className="dv-page-paddinng"
            style={{
                padding: '0px 20px 0px 0px',
                height: 'auto',
            }}
        >
            {/* <Title isBack children={undefined} /> */}
            <div>
                <div className="dv-flex-between">
                    <SearchForm form={form} onSearch={onSearch} placeholder={intl.formatMessage({ id: 'common_search' })} />
                </div>
            </div>
            <Table<TJobsInstanceTableItem>
                size="middle"
                loading={loading}
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
            <RenderLoggerModal />
            <RenderErrorDataModal />
            <RenderResultModal />
        </div>
    );
};

export default JobsInstance;
