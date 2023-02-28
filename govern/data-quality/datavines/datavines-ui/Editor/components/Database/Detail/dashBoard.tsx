import * as echarts from 'echarts/core';
import { LineChart, BarChart } from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    GridComponent,
    DatasetComponent,
    TransformComponent,
    LegendComponent,
} from 'echarts/components';
// 标签自动布局，全局过渡动画等特性
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { Empty } from 'antd';
// 引入 Canvas 渲染器，注意引入 CanvasRenderer 或者 SVGRenderer 是必须的一步
import { CanvasRenderer } from 'echarts/renderers';
import React, { useEffect, useState } from 'react';

echarts.use([
    TitleComponent,
    TooltipComponent,
    GridComponent,
    DatasetComponent,
    TransformComponent,
    LineChart,
    LabelLayout,
    UniversalTransition,
    CanvasRenderer,
    BarChart,
    LegendComponent,
]);
const Index = ({ id, option, style = {} }:{id:string, option:any, style?:any}) => {
    const [myChart, setMyChart] = useState<any>(null);
    useEffect(() => {
        if (!option) {
            setMyChart(null);
            return;
        }
        if (!option?.series || !option?.series?.length || option.series[0]?.data?.length === 0) {
            if (document.getElementById(id) && document.getElementById(id)?.innerHTML) {
                // eslint-disable-next-line no-unused-expressions
                myChart?.dispose && myChart.current?.dispose();
                setMyChart(null);
                const dom = document.getElementById(id) as HTMLElement;
                dom.innerHTML = '';
            }
            return;
        }
        if (!myChart) {
            const charts = echarts.init(document.getElementById(id) as HTMLElement);
            setMyChart(charts);
            charts.setOption(option, true);
        } else {
            myChart.setOption(option, true);
        }
        console.log('option', option);
    }, [option]);

    return (
        <div style={{
            position: 'relative',
        }}
        >
            <div
                id={id}
                style={{
                    alignItems: 'center',
                    justifyContent: 'center',
                    position: 'relative',
                    height: '500px',
                    ...style,
                }}
            />
            {!myChart
                ? (
                    <Empty
                        style={{
                            height: style?.height ? style?.height : '500px',
                            position: 'absolute',
                            top: '0',
                            left: '0',
                            width: '100%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexDirection: 'column',
                            marginBlock: '0px',
                        }}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                ) : ''}
        </div>
    );
};

export default Index;
