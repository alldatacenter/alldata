/* eslint-disable react/no-danger */
import React, { useRef, useState } from 'react';
import { ModalProps, Table } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import {
    useModal, useImmutable, usePersistFn, useWatch,
} from 'src/common';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { defaultRender } from '@/utils/helper';

type InnerProps = {
    [key: string]: any
}
type tableItem = {
    [key: string]: any;
}
const Inner = (props: InnerProps) => {
    const [loading, setLoading] = useState(false);
    const [tableData, setTableData] = useState<{ list: tableItem[], total: number}>({ list: [], total: 0 });
    const [pageParams, setPageParams] = useState({
        pageNumber: 1,
        pageSize: 10,
    });
    const [columns, setColumns] = useState<ColumnsType<tableItem>>([]);
    const getData = async () => {
        try {
            setLoading(true);
            const res = (await $http.get('/job/execution/errorDataPage', {
                taskId: props.record.id,
                ...pageParams,
            })) || [];
            setColumns((res.columns || []).map((item: any) => ({
                title: item.name,
                dataIndex: item.name,
                key: 'name',
                width: 180,
                render: (text: any) => defaultRender(text, 180),
            })));
            setTableData({
                list: res?.resultList || [],
                total: res.totalCount || 0,
            });
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    useWatch([pageParams], async () => {
        getData();
    }, { immediate: true });
    const onChange = ({ current, pageSize }: any) => {
        setPageParams({
            pageNumber: current,
            pageSize,
        });
    };
    return (
        <div>
            <Table<tableItem>
                size="middle"
                loading={loading}
                rowKey="id"
                columns={columns}
                dataSource={tableData.list || []}
                onChange={onChange}
                scroll={{
                    x: (columns.length) * 120,
                }}
                pagination={{
                    size: 'small',
                    total: tableData.total,
                    showSizeChanger: true,
                    current: pageParams.pageNumber,
                    pageSize: pageParams.pageSize,
                }}
            />
        </div>
    );
};

export const useInstanceErrorDataModal = (options: ModalProps) => {
    const intl = useIntl();
    const recordRef = useRef<any>();
    const onOk = usePersistFn(() => {
        hide();
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: `${intl.formatMessage({ id: 'jobs_task_error_data_view' })}`,
        className: 'dv-modal-fullscreen',
        footer: null,
        width: '90%',
        ...(options || {}),
        afterClose() {
            recordRef.current = null;
        },
        onOk,
    });
    return {
        Render: useImmutable(() => (<Render><Inner record={recordRef.current} /></Render>)),
        show(record: any) {
            recordRef.current = record;
            show(record);
        },
        ...rest,
    };
};
