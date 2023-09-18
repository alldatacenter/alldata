/* eslint-disable react/no-danger */
import React, { useRef, useState } from 'react';
import {
    ModalProps, Table,
} from 'antd';
import { ColumnsType } from 'antd/lib/table';
import {
    useModal, useImmutable, useMount,
} from 'src/common';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { defaultRender } from '@/utils/helper';

type ResultItem = {
    checkSubject: string;
    metricName: string;
    metricParameter: string;
    checkResult: string;
    expectedType: string;
    resultFormulaFormat: string;
}

type TResultItemData = {
    list: ResultItem[];
    total: number;
}

const Inner = (props: any) => {
    const [loading, setLoading] = useState(false);
    const [tableData, setTableData] = useState<TResultItemData>({ list: [], total: 0 });
    const intl = useIntl();
    const getData = async () => {
        try {
            setLoading(true);
            const res = (await $http.get<any>(`job/execution/list/result/${props.record.id}`)) || {};
            setTableData({ list: res || [], total: (res || []).length });
        } catch (error) {
            console.log(error);
        } finally {
            setLoading(false);
        }
    };
    const columns: ColumnsType<ResultItem> = [
        {
            title: intl.formatMessage({ id: 'jobs_task_check_subject' }),
            dataIndex: 'checkSubject',
            key: 'checkSubject',
            width: 260,
            render: (text: any) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_check_rule' }),
            dataIndex: 'metricName',
            key: 'metricName',
            width: 180,
            render: (text: any) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_check_result' }),
            dataIndex: 'checkResult',
            key: 'checkResult',
            width: 120,
            render: (text: any) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_check_expectVal_type' }),
            dataIndex: 'expectedType',
            key: 'expectedType',
            width: 180,
            render: (text: any) => defaultRender(text, 200),
        },
        {
            title: intl.formatMessage({ id: 'jobs_task_check_formula' }),
            dataIndex: 'resultFormulaFormat',
            key: 'resultFormulaFormat',
            width: 200,
            render: (text: any) => defaultRender(text, 200),
        },
    ];

    useMount(getData);
    return (
        <div>
            <Table<ResultItem>
                loading={loading}
                size="middle"
                bordered
                columns={columns}
                dataSource={tableData.list || []}
                pagination={false}
            />
        </div>
    );
};

export const useInstanceResult = (options: ModalProps) => {
    const intl = useIntl();
    const recordRef = useRef<any>();
    const {
        Render, show, ...rest
    } = useModal<any>({
        title: `${intl.formatMessage({ id: 'jobs_task_check_result' })}`,
        footer: null,
        width: '1000px',
        ...(options || {}),
        afterClose() {
            recordRef.current = null;
        },
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
