import React, { Component } from 'react';
import { connect } from 'dva'
import { loadChartData } from '../../../../../utils/loadChartData';
import safeEval from '../../../../../utils/SafeEval';
import {
    getTheme,
    PieChart
} from "bizcharts";
import _ from 'lodash'
@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
class Pie extends Component {
    constructor(props) {
        super(props);
        this.state = {
            chartData: null,
            nodeParams: _.cloneDeep(props.nodeParams)
        }
        this.timerInterval = null
    }
    componentDidMount() {
        let { widgetConfig = {} } = this.props;
        let { period } = widgetConfig
        if (period) {
            this.intervalLoad()
            this.timerInterval = setInterval(() => {
                this.intervalLoad()
            }, Number(period) || 10000)
        }
    }
    async intervalLoad() {
        let { widgetConfig = {} } = this.props;
        let allProps = { ...this.props }
        let data = await loadChartData(allProps, widgetConfig);
        this.setState({
            chartData: data
        })
    }
    componentWillUnmount() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval)
        }
    }
    render() {
        const { widgetConfig = {}, widgetData } = this.props;
        let { chartData } = this.state;
        let { theme, appendPadding, angleFeild, chartTitle, colorFeild, pieRadius, height = '', width, isLegend, legendPosition, period, advancedConfig = {} } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        const data = [
            { item: "事例一", percent: 0.4 },
            { item: "事例二", percent: 0.21 },
            { item: "事例三", percent: 0.17 },
            { item: "事例四", percent: 0.13 },
            { item: "事例五", percent: 0.09 },
        ];
        const colors = data.reduce((pre, cur, idx) => {
            pre[cur.item] = getTheme().colors10[idx];
            return pre;
        }, {});

        const cols = {
            percent: {
                formatter: (val) => {
                    val = val * 100 + "%";
                    return val;
                },
            },
        };
        let finalData = chartData || widgetData || data;
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <PieChart appendPadding={appendPadding || [10, 0, 0, 10]}
                theme={theme || 'light'}
                height={height && Number(height)}
                width={width && Number(width)}
                autoFit={true}
                data={finalData}
                scale={cols}
                title={{
                    visible: !!chartTitle,
                    text: chartTitle || '',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)',
                    }
                }}
                legend={{
                    visible: isLegend,
                    position: legendPosition
                }}
                angleField={angleFeild || 'percent'}
                colorField={colorFeild || 'item'}
                radius={pieRadius || 0.8}
                interactions={['element-active']}
                {...advConf}
            />
        );
    }
}

export default Pie;