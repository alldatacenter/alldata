import React, { useEffect, useState } from 'react';
import { Table } from 'antd';
import { useIntl } from 'react-intl';
import { ColumnsType } from 'antd/es/table';
import { $http } from '@/http';
import { IF } from '@/common';
import { TJobsInstanceTableItem } from '@/type/JobsInstance';
import { useInstanceErrorDataModal } from '@/view/Main/HomeDetail/Jobs/useInstanceErrorDataModal';
import { useInstanceResult } from '@/view/Main/HomeDetail/Jobs/useInstanceResult';
import { useLogger } from '@/view/Main/HomeDetail/Jobs/useLogger';

const Index = ({ id }:{id:string}) => {
    const intl = useIntl();
    const [loading, setLoading] = useState(false);
    const { Render: RenderErrorDataModal, show: showErrorDataModal } = useInstanceErrorDataModal({});
    const { Render: RenderResultModal, show: showResultModal } = useInstanceResult({});
    const { Render: RenderLoggerModal, show: showLoggerModal } = useLogger({});
    const onStop = async (record: TJobsInstanceTableItem) => {
        try {
            setLoading(true);
            await $http.delete(`/task/kill/${record.id}`);
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
        console.log(record);
        showResultModal(record);
    };
    const onErrorData = (record: TJobsInstanceTableItem) => {
        console.log(record);
        showErrorDataModal(record);
    };
    const [tableData, setTableData] = useState([]);
    const columns: ColumnsType<TJobsInstanceTableItem> = [{
        title: 'Task Name',
        dataIndex: 'name',
        key: 'name',
    }, {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
    }, {
        title: 'Update Time',
        dataIndex: 'updateTime',
        key: 'updateTime',
    }, {
        title: intl.formatMessage({ id: 'common_action' }),
        fixed: 'right',
        key: 'right',
        dataIndex: 'right',
        width: 240,
        render: (text: string, record: any) => (
            <>
                <IF visible={record.status === 'submitted' || record.status === 'running'}>
                    <a style={{ marginRight: 5 }} onClick={() => { onStop(record); }}>{intl.formatMessage({ id: 'jobs_task_stop_btn' })}</a>
                </IF>
                <a style={{ marginRight: 5 }} onClick={() => { onLog(record); }}>{intl.formatMessage({ id: 'jobs_task_log_btn' })}</a>
                <a style={{ marginRight: 5 }} onClick={() => { onResult(record); }}>{intl.formatMessage({ id: 'jobs_task_result' })}</a>
                <a style={{ marginRight: 5 }} onClick={() => { onErrorData(record); }}>{intl.formatMessage({ id: 'jobs_task_error_data' })}</a>
            </>
        ),
    }];
    const getData = async () => {
        const res = await $http.get('/job/execution/page', {
            jobId: id,
            pageNumber: 1,
            pageSize: 999,
        });
        setTableData(res.records);
    };
    useEffect(() => {
        getData();
    }, []);

    return (
        <div>
            <Table
                rowKey="id"
                dataSource={tableData}
                columns={columns}
            />
            <RenderLoggerModal />
            <RenderErrorDataModal />
            <RenderResultModal />
        </div>

    );
};

export default Index;
