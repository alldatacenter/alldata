import React from 'react';
import { FormattedMessage } from 'react-intl';
import DashBoard from './Detail/dashBoard';

// const intl = useIntl();
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
        label: <FormattedMessage id="job_Table" />, name: 'Tables', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_Schema_Changes" />, name: 'Schema Changes', children: null, key: '1',
    },
];
export const dataBaseCol:Col[][] = [[{
    title: <FormattedMessage id="job_Table" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
    // onCell: (record) => ({
    //     onClick: (event) => {
    //         console.log('record', record, event);
    //     },
    // }),
},
{
    title: <FormattedMessage id="job_last_refresh_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
},
{
    title: <FormattedMessage id="job_Column" />,
    dataIndex: 'columns',
    key: 'columns',
}, {
    title: 'Metrics',
    dataIndex: 'metrics',
    key: 'metrics',
}], [
    {
        title: <FormattedMessage id="warn_SLAs_type" />,
        dataIndex: 'changeType',
        key: 'changeType',
    },
    {
        title: <FormattedMessage id="job_database" />,
        dataIndex: 'databaseName',
        key: 'databaseName',
    },
    {
        title: <FormattedMessage id="job_Table" />,
        dataIndex: 'tableName',
        key: 'tableName',
    },
    {
        title: <FormattedMessage id="job_Column" />,
        dataIndex: 'columnName',
        key: 'columnName',
    },
]];
export const tableTabs:Tab[] = [
    {
        label: <FormattedMessage id="job_Profile" />, name: 'Profile', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_Column" />, name: 'Column', children: null, key: '1',
    },
    {
        label: <FormattedMessage id="job_Metrics" />, name: 'Metrics', children: null, key: '2',
    },
    {
        label: <FormattedMessage id="job_Schema_Changes" />, name: 'Schema Changes', children: null, key: '3',
    },
    {
        label: <FormattedMessage id="job_issue" />, name: 'Issues', children: null, key: '4',
    },
];
export const tableCol:Col[][] = [[{
    title: <FormattedMessage id="job_Column" />,
    dataIndex: 'name',
    key: 'name',
    // render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}, {
    title: <FormattedMessage id="warn_SLAs_type" />,
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
    title: <FormattedMessage id="job_Unique" />,
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
    title: <FormattedMessage id="job_Distinct" />,
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
        title: <FormattedMessage id="job_Column" />,
        dataIndex: 'name',
        key: 'name',
        render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
        // onCell: (record) => ({
        //     onClick: (event) => {
        //         console.log('record', record, event);
        //     },
        // }),
    },
    {
        title: <FormattedMessage id="warn_SLAs_type" />,
        dataIndex: 'type',
        key: 'type',
    },
    {
        title: <FormattedMessage id="job_Comment" />,
        dataIndex: 'comment',
        key: 'comment',
    },
    {
        title: <FormattedMessage id="job_Metrics" />,
        dataIndex: 'metrics',
        key: 'metrics',
    }, {
        title: <FormattedMessage id="job_Last_Update_Time" />,
        dataIndex: 'updateTime',
        key: 'updateTime',
    },
], [{
    title: <FormattedMessage id="warn_SLAs_name" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
},
{
    title: <FormattedMessage id="job_Status" />,
    dataIndex: 'status',
    key: 'status',
    width: '100px',
},
{
    title: <FormattedMessage id="job_Trend" />,
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
    title: <FormattedMessage id="job_Last_Update_Time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '200px',
}, {
    title: <FormattedMessage id="job_Action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
    // render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}], [{
    title: <FormattedMessage id="warn_SLAs_type" />,
    dataIndex: 'changeType',
    key: 'changeType',
}, {
    title: <FormattedMessage id="job_database" />,
    dataIndex: 'databaseName',
    key: 'databaseName',
}, {
    title: <FormattedMessage id="job_Table" />,
    dataIndex: 'tableName',
    key: 'tableName',
}, {
    title: <FormattedMessage id="job_Column" />,
    dataIndex: 'columnName',
    key: 'columnName',
}, {
    title: <FormattedMessage id="job_Before" />,
    dataIndex: 'changeBefore',
    key: 'changeBefore',
}, {
    title: <FormattedMessage id="job_After" />,
    dataIndex: 'changeAfter',
    key: 'changeAfter',
}, {
    title: <FormattedMessage id="warn_update_time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
}], [{
    title: <FormattedMessage id="job_Metrics" />,
    dataIndex: 'metricName',
    key: 'metricName',
    width: '190px',
},
{
    title: <FormattedMessage id="job_Title" />,
    dataIndex: 'title',
    key: 'title',
},
{
    title: <FormattedMessage id="job_Status" />,
    dataIndex: 'status',
    key: 'status',
    width: '80px',
}, {
    title: <FormattedMessage id="job_Create_Time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '160px',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
    // render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}]];
export const colTabs:Tab[] = [
    {
        label: <FormattedMessage id="job_Metrics" />, name: 'Metrics', children: null, key: '0',
    },
    {
        label: <FormattedMessage id="job_issue" />, name: 'Issues', children: null, key: '1',
    },
    {
        label: <FormattedMessage id="job_Schema_Changes" />, name: 'Schema Changes', children: null, key: '2',
    },
];
export const colCol:Col[][] = [[{
    title: <FormattedMessage id="warn_SLAs_name" />,
    dataIndex: 'name',
    key: 'name',
    render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}, {
    title: <FormattedMessage id="job_Status" />,
    dataIndex: 'status',
    key: 'status',
    width: '100px',
}, {
    title: <FormattedMessage id="job_Trend" />,
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
    title: <FormattedMessage id="job_Last_Update_Time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
    // render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}], [{
    title: <FormattedMessage id="job_Metrics" />,
    dataIndex: 'metricName',
    key: 'metricName',
    width: '190px',
},
{
    title: <FormattedMessage id="job_Title" />,
    dataIndex: 'title',
    key: 'title',
},
{
    title: <FormattedMessage id="job_Status" />,
    dataIndex: 'status',
    key: 'status',
    width: '80px',
}, {
    title: <FormattedMessage id="job_Create_Time" />,
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: '160px',
}, {
    title: <FormattedMessage id="common_action" />,
    dataIndex: 'action',
    key: 'action',
    width: '80px',
    // render: (_: any, { name }: any) => <span className="text-underline">{name}</span>,
}], [{
    title: <FormattedMessage id="warn_SLAs_type" />,
    dataIndex: 'changeType',
    key: 'changeType',
}, {
    title: <FormattedMessage id="job_database" />,
    dataIndex: 'databaseName',
    key: 'databaseName',
}, {
    title: <FormattedMessage id="job_Table" />,
    dataIndex: 'tableName',
    key: 'tableName',
}, {
    title: <FormattedMessage id="job_Column" />,
    dataIndex: 'columnName',
    key: 'columnName',
}, {
    title: <FormattedMessage id="job_Before" />,
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
