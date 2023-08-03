import React from 'react';
import { FormattedMessage } from 'react-intl';
import DashBoard from './Detail/dashBoard';

export type Tab = {
    label:any;
    children:null;
    key:string;
    name:string;
}

export type Col = {
    title:any;
    dataIndex:any;
    key:string;
    onCell?: (record: any) => {
        onClick: (event: any) => void;
    },
    render?:any,
    width?:string;
}

export const dataBaseTabs:Tab[] = [
    {
        label: <FormattedMessage id="job_table" />, name: 'Tables', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_schema_changes" />, name: 'Schema Changes', children: null, key: '1',
    },
];
export const dataBaseCol:Col[][] = [[{
    title: <FormattedMessage id="job_table" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
},
{
    title: <FormattedMessage id="job_last_refresh_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
},
{
    title: <FormattedMessage id="job_column" />,
    dataIndex: 'columns',
    key: 'columns',
}, {
    title: 'Metrics',
    dataIndex: 'metrics',
    key: 'metrics',
}], [
    {
        title: <FormattedMessage id="warn_sLAs_type" />,
        dataIndex: 'changeType',
        key: 'changeType',
    },
    {
        title: <FormattedMessage id="job_database" />,
        dataIndex: 'databaseName',
        key: 'databaseName',
    },
    {
        title: <FormattedMessage id="job_table" />,
        dataIndex: 'tableName',
        key: 'tableName',
    },
    {
        title: <FormattedMessage id="job_column" />,
        dataIndex: 'columnName',
        key: 'columnName',
    },
    {
        title: <FormattedMessage id="warn_update_time" />,
        dataIndex: 'updateTime',
        key: 'updateTime',
    },
]];
export const tableTabs:Tab[] = [
    {
        label: <FormattedMessage id="job_profile" />, name: 'Profile', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_column" />, name: 'Column', children: null, key: '1',
    },
    {
        label: <FormattedMessage id="job_metrics" />, name: 'Metrics', children: null, key: '2',
    },
    {
        label: <FormattedMessage id="job_schema_changes" />, name: 'Schema Changes', children: null, key: '3',
    },
    {
        label: <FormattedMessage id="job_issue" />, name: 'Issues', children: null, key: '4',
    },
];
export const tableCol:Col[][] = [[{
    title: <FormattedMessage id="job_column" />,
    dataIndex: 'name',
    key: 'name',
}, {
    title: <FormattedMessage id="warn_sLAs_type" />,
    dataIndex: 'type',
    key: 'type',

}, {
    title: <FormattedMessage id="job_null" />,
    dataIndex: 'nullCount',
    key: 'nullCount',
    render: (_: any, { nullCount, nullPercentage }: any) => (
        <span>
            {nullCount}
            {' '}
            [
            {nullPercentage}
            ]
        </span>
    ),

}, {
    title: <FormattedMessage id="job_notNull" />,
    dataIndex: 'notNullCount',
    key: 'notNullCount',
    render: (_: any, { notNullCount, notNullPercentage }: any) => (
        <span>
            {notNullCount}
            {' '}
            [
            {notNullPercentage}
            ]
        </span>
    ),

}, {
    title: <FormattedMessage id="job_unique" />,
    dataIndex: 'uniqueCount',
    key: 'uniqueCount',
    render: (_: any, { uniqueCount, uniquePercentage }: any) => (
        <span>
            {uniqueCount}
            {' '}
            [
            {uniquePercentage}
            ]
        </span>
    ),

}, {
    title: <FormattedMessage id="job_distinct" />,
    dataIndex: 'distinctCount',
    key: 'distinctCount',
    render: (_: any, { distinctCount, distinctPercentage }: any) => (
        <span>
            {distinctCount}
            {' '}
            [
            {distinctPercentage}
            ]
        </span>
    ),
}], [
    {
        title: <FormattedMessage id="job_column" />,
        dataIndex: 'name',
        key: 'name',
        render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
    },
    {
        title: <FormattedMessage id="warn_sLAs_type" />,
        dataIndex: 'type',
        key: 'type',
    },
    {
        title: <FormattedMessage id="job_comment" />,
        dataIndex: 'comment',
        key: 'comment',
    },
    {
        title: <FormattedMessage id="job_metrics" />,
        dataIndex: 'metrics',
        key: 'metrics',
    }, {
        title: <FormattedMessage id="job_last_update_time" />,
        dataIndex: 'updateTime',
        key: 'updateTime',
    },
], [{
    title: <FormattedMessage id="warn_sLAs_name" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
},
{
    title: <FormattedMessage id="job_status" />,
    dataIndex: 'status',
    key: 'status',
    width: '100px',
},
{
    title: <FormattedMessage id="job_trend" />,
    dataIndex: 'columns',
    key: 'columns',
    render: (_: any, { charts, id }: any) => (
        <DashBoard
            style={{
                height: '100px',
                width: '100%',
            }}
            id={id}
            option={
                {
                    color: ['#ffd56a'],
                    grid: {
                        top: 10,
                        right: 0,
                        bottom: 10,
                    },
                    tooltip: {},
                    xAxis: {
                        type: 'category',
                        axisLabel: { show: false },
                        data: charts.map((item:{
                        datetime:string;
                    }) => item.datetime),
                    },
                    yAxis: {
                        type: 'value',
                    },
                    series: [
                        {
                            data: charts.map((item:{
                            value:number;
                        }) => item.value),
                            type: 'line',
                            smooth: true,
                        },
                    ],
                }
            }
        />
    ),
}, {
    title: <FormattedMessage id="job_last_update_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '200px',
}, {
    title: <FormattedMessage id="job_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
}], [{
    title: <FormattedMessage id="warn_sLAs_type" />,
    dataIndex: 'changeType',
    key: 'changeType',
}, {
    title: <FormattedMessage id="job_database" />,
    dataIndex: 'databaseName',
    key: 'databaseName',
}, {
    title: <FormattedMessage id="job_table" />,
    dataIndex: 'tableName',
    key: 'tableName',
}, {
    title: <FormattedMessage id="job_column" />,
    dataIndex: 'columnName',
    key: 'columnName',
}, {
    title: <FormattedMessage id="job_before" />,
    dataIndex: 'changeBefore',
    key: 'changeBefore',
}, {
    title: <FormattedMessage id="job_after" />,
    dataIndex: 'changeAfter',
    key: 'changeAfter',
}, {
    title: <FormattedMessage id="warn_update_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
}], [{
    title: <FormattedMessage id="job_metrics" />,
    dataIndex: 'metricName',
    key: 'metricName',
    width: '190px',
},
{
    title: <FormattedMessage id="job_title" />,
    dataIndex: 'title',
    key: 'title',
},
{
    title: <FormattedMessage id="job_status" />,
    dataIndex: 'status',
    key: 'status',
    width: '80px',
}, {
    title: <FormattedMessage id="job_Create_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '160px',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
}]];
export const colTabs:Tab[] = [
    {
        label: <FormattedMessage id="job_metrics" />, name: 'Metrics', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_issue" />, name: 'Issues', children: null, key: '1',
    },
    {
        label: <FormattedMessage id="job_schema_changes" />, name: 'Schema Changes', children: null, key: '2',
    },
];
export const colCol:Col[][] = [[{
    title: <FormattedMessage id="warn_sLAs_name" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}, {
    title: <FormattedMessage id="job_status" />,
    dataIndex: 'status',
    key: 'status',
    width: '100px',
}, {
    title: <FormattedMessage id="job_trend" />,
    dataIndex: 'trend',
    key: 'trend',
    render: (_: any, { charts, id }: any) => (
        <DashBoard
            style={{
                height: '80px',
                width: '100%',
            }}
            id={id}
            option={
                {
                    color: ['#ffd56a'],
                    grid: {
                        top: 10,
                        right: 0,
                        bottom: 10,
                    },
                    tooltip: {},
                    xAxis: {
                        type: 'category',
                        axisLabel: { show: false },
                        data: charts.map((item:{
                        datetime:string;
                    }) => item.datetime),
                    },
                    yAxis: {
                        type: 'value',
                    },
                    series: [
                        {
                            data: charts.map((item:{
                            value:number;
                        }) => item.value),
                            type: 'line',
                            smooth: true,
                        },
                    ],
                }
            }
        />
    )
    ,
}, {
    title: <FormattedMessage id="job_last_update_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
}], [{
    title: <FormattedMessage id="job_metrics" />,
    dataIndex: 'metricName',
    key: 'metricName',
    width: '190px',
},
{
    title: <FormattedMessage id="job_title" />,
    dataIndex: 'title',
    key: 'title',
},
{
    title: <FormattedMessage id="job_status" />,
    dataIndex: 'status',
    key: 'status',
    width: '80px',
}, {
    title: <FormattedMessage id="job_Create_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '160px',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
}], [{
    title: <FormattedMessage id="warn_sLAs_type" />,
    dataIndex: 'changeType',
    key: 'changeType',
}, {
    title: <FormattedMessage id="job_database" />,
    dataIndex: 'databaseName',
    key: 'databaseName',
}, {
    title: <FormattedMessage id="job_table" />,
    dataIndex: 'tableName',
    key: 'tableName',
}, {
    title: <FormattedMessage id="job_column" />,
    dataIndex: 'columnName',
    key: 'columnName',
}, {
    title: <FormattedMessage id="job_before" />,
    dataIndex: 'changeBefore',
    key: 'changeBefore',
}, {
    title: <FormattedMessage id="jobs_nminute_before" />,
    dataIndex: 'changeAfter',
    key: 'changeAfter',
}, {
    title: <FormattedMessage id="warn_update_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
}]];
