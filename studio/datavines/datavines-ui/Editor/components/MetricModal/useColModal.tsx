/* eslint-disable react/no-danger */
import React, { useRef } from 'react';
import { ModalProps, Table } from 'antd';
import {
    useModal, useImmutable, usePersistFn,
} from 'src/common';
import { useIntl } from 'react-intl';

type InnerProps = {
    [key: string]: any
}
type tableItem = {
    [key: string]: any;
}
const Inner = (props: InnerProps) => {
    const intl = useIntl();
    const columns = [{
        title: intl.formatMessage({ id: 'job_column_Name' }),

        dataIndex: 'name',
        key: 'name',
    }];
    return (
        <div>
            <Table<tableItem>
                size="middle"
                rowKey="id"
                columns={columns}
                dataSource={props.record?.list || []}
                scroll={{
                    y: 'calc(100vh - 400px)',
                }}
                pagination={false}
            />
        </div>
    );
};

export const useColModal = (options: ModalProps) => {
    const intl = useIntl();
    const recordRef = useRef<any>();
    const onOk = usePersistFn(() => {
        hide();
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: `${intl.formatMessage({ id: 'job_column' })}`,

        footer: null,
        width: '40%',
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
