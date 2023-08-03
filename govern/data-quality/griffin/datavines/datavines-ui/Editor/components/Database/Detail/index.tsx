import React, {
    useState, useRef, useEffect, forwardRef, useImperativeHandle,
} from 'react';
import { Tabs } from 'antd';
import { useIntl } from 'react-intl';
import './index.less';
import useRequest from '@Editor/hooks/useRequest';
import DashBoard from './dashBoard';
import Runs from './runs';
import { Inner } from '../../../../src/view/Main/HomeDetail/Jobs/useAddEditJobsModal';

const Index = (props: any, ref:any) => {
    const { id, selectDatabases, changeTabKey } = props;
    const intl = useIntl();
    useImperativeHandle(ref, () => ({ runsRef }));
    const innerRef = useRef();
    const { $http } = useRequest();
    const metricParameter = {
        database: selectDatabases[1] ? selectDatabases[1].name : ' ',
        table: selectDatabases[2] ? selectDatabases[2].name : ' ',
        column: selectDatabases[3] ? selectDatabases[3].name : ' ',
    };
    const baseData = {
        data: {
            id: selectDatabases[1] ? selectDatabases[1].id : '',
            record: {
                id,
                parameter: JSON.stringify({ metricParameter }),
                parameterItem: {
                    metricParameter,
                },
            },
        },
    };

    const initData = async () => {
        const res = await $http.get('/catalog/list/entity/metric/dashboard', {
            jobId: id,
        });
        setOption({
            color: ['#ffd56a'],
            tooltip: {},
            xAxis: {
                type: 'category',
                data: res.map((item:{
                    datetime:string;
                }) => item.datetime),
            },
            yAxis: {
                type: 'value',
            },
            series: [
                {
                    data: res.map((item:{
                        value:number;
                    }) => item.value),
                    type: 'line',
                    smooth: true,
                },
            ],
        });
    };
    useEffect(() => {
        initData();
    }, []);
    const [option, setOption] = useState<any>(null);

    const runsRef = useRef(null);
    const initialTableItems = [
        { label: intl.formatMessage({ id: 'editor_dv_DashBoard' }), children: <DashBoard option={option} id={id} />, key: '1' },
        {
            label: intl.formatMessage({ id: 'editor_dv_Configuation' }),
            children: <Inner
                styleChildren={{
                    height: 'calc(100vh - 324px)',
                }}
                styleTabContent={{
                    height: 'calc(100vh - 324px)',
                }}
                baseData={baseData}
                innerRef={innerRef}
            />,
            key: '2',
        },
        { label: intl.formatMessage({ id: 'editor_dv_Runs' }), children: <Runs id={id} ref={runsRef} />, key: '3' },
    ];
    const [activeTableKey, setActiveKey] = useState(initialTableItems[0].key);
    return (
        <div className="dv-database-Detail">
            <div className="dv-database-table">
                <Tabs
                    size="small"
                    activeKey={activeTableKey}
                    items={initialTableItems}
                    onChange={(value:string) => {
                        changeTabKey(value);
                        setActiveKey(value);
                    }}
                    style={{ marginBottom: '0px' }}
                    className="dv-tab-list"
                />
            </div>
        </div>
    );
};

export default forwardRef(Index);
