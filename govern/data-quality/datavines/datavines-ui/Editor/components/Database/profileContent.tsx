import useRequest from '@Editor/hooks/useRequest';
import { Tooltip } from 'antd';
import React, { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
import DashBoard from './Detail/dashBoard';

const Index = ({ uuid, type }:{uuid:string;type:string}) => {
    const { $http } = useRequest();
    const intl = useIntl();
    const getData = async () => {
        const res = await $http.get(`/catalog/profile/column/${uuid}`);
        setData(res);
        if (res.top10Distribution) {
            setOption(
                {
                    title: {
                        text: 'Top 10',
                        left: 'center',
                    },
                    color: ['#ffd56a', '#ff4d4f'],
                    grid: {
                        top: 30,
                        right: 0,
                        bottom: 30,
                    },
                    tooltip: {
                        formatter: (data:any) => `${data.marker}${data.data.name}<br/>count:${data.data.value}<br/>percentage:${data.data.percentage}`,
                    },
                    xAxis: {
                        type: 'category',
                        data: res.top10Distribution.map((item:{
                        name:string;
                    }) => item.name),
                        axisLabel: {
                            overflow: 'truncate',
                            width: 100,
                        },
                    },
                    yAxis: {
                        type: 'value',
                    },
                    series: [
                        {
                            name: 'Count',
                            data: res.top10Distribution.map((item:{
                                count:number;percentage:string;name:string;
                        }) => ({
                                value: item.count,
                                name: item.name,
                                percentage: item.percentage,
                            })),
                            type: 'bar',
                            barMaxWidth: 10,
                        },
                    ],
                },
            );
        }
    };
    const [data, setData] = useState({
        name: '',
        uuid: '',
        type: '',
        dataType: null,
        nullCount: '',
        nullPercentage: '',
        notNullCount: '',
        notNullPercentage: '',
        uniqueCount: '',
        uniquePercentage: '',
        distinctCount: '',
        distinctPercentage: '',
        top10Distribution: null,
        maxValue: '',
        minValue: '',
        sumValue: '',
        avgValue: '',
        stdDev: '',
        variance: '',
        minLength: '',
        maxLength: '',
        avgLength: '',
        blankCount: '',
    });
    const [option, setOption] = useState({});
    useEffect(() => {
        // /catalog/profile/column/{uuid}
        // console.log('家在', uuid);
        if (uuid) {
            getData();
        }
    }, []);

    return (
        <div style={{
            minHeight: '250px',
            width: '100%',
            display: 'flex',
        }}
        >
            <div style={{ width: '250px' }}>
                <p className="profile-text">
                    <span>
                        {intl.formatMessage({ id: 'job_Maximum' })}
                        :
                    </span>
                    <Tooltip title={data.maxValue}><span>{data.maxValue}</span></Tooltip>
                </p>
                <p className="profile-text">
                    <span>
                        {intl.formatMessage({ id: 'job_Minimum' })}
                        :
                    </span>
                    <Tooltip title={data.minValue}><span>{data.minValue}</span></Tooltip>
                </p>
                {
                    type === 'string'
                        ? (
                            <>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Max_Length' })}
                                        :
                                    </span>
                                    <Tooltip title={data.maxLength}><span>{data.maxLength}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Min_Length' })}
                                        :
                                    </span>
                                    <Tooltip title={data.minLength}><span>{data.minLength}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Avg_Length' })}
                                        :
                                    </span>
                                    <Tooltip title={data.avgLength}><span>{data.avgLength}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Blank' })}
                                        :
                                    </span>
                                    <Tooltip title={data.blankCount}><span>{data.blankCount}</span></Tooltip>
                                </p>
                            </>
                        )
                        : ''
                }
                {
                    type === 'numeric'
                        ? (
                            <>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Sum' })}
                                        :
                                    </span>
                                    <Tooltip title={data.sumValue}><span>{data.sumValue}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Avg' })}
                                        :
                                    </span>
                                    <Tooltip title={data.avgValue}><span>{data.avgValue}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_StdDev' })}
                                        :
                                    </span>
                                    <Tooltip title={data.stdDev}><span>{data.stdDev}</span></Tooltip>
                                </p>
                                <p className="profile-text">
                                    <span>
                                        {intl.formatMessage({ id: 'job_Variance' })}
                                        :
                                    </span>
                                    <Tooltip title={data.variance}><span>{data.variance}</span></Tooltip>
                                </p>
                            </>
                        ) : ''
                }

            </div>
            <div style={{ flex: '1' }}>
                <DashBoard style={{ minHeight: '200px', height: '100%', width: '100%' }} id={`profileContent${uuid}`} option={option} />
            </div>

        </div>
    );
};

export default Index;
