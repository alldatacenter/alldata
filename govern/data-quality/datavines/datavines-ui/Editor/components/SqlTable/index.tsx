import React, { useState } from 'react';
import { Table } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import moment from 'moment';
import { defaultRender } from '@/utils/helper';
import { IDvSqlTable, IDvSqlTableResultItem } from '../../type';

type SqlTableProps = {
    style?: React.CSSProperties,
    tableData: IDvSqlTable
}

const SqlTable = ({ style, tableData }: SqlTableProps) => {
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
    const columns: ColumnsType<IDvSqlTableResultItem> = (tableData.columns || []).map((item) => ({
        title: item.name,
        dataIndex: item.name,
        key: item.name,
        width: item.type === 'DATETIME' ? 200 : 160,
        ellipsis: true,
        render: (text) => {
            if (item.type === 'DATETIME') {
                return text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : '';
            }
            if (text) {
                return defaultRender(text, 160);
            }
            return text;
        },
    }));
    return (
        <div style={{ ...style, width: '100%' }}>
            <div style={{ padding: '0 10px' }}>
                <Table<IDvSqlTableResultItem>
                    size="small"
                    rowKey="id"
                    columns={columns}
                    dataSource={tableData.resultList || []}
                    onChange={onChange}
                    className="dv-table-small"
                    scroll={{
                        x: '100%',
                        y: 280,
                    }}
                    sticky
                    pagination={{
                        size: 'small',
                        total: (tableData.resultList || []).length,
                        showSizeChanger: true,
                        current: pageParams.pageNumber,
                        pageSize: pageParams.pageSize,
                    }}
                />
            </div>
        </div>
    );
};

export default SqlTable;
