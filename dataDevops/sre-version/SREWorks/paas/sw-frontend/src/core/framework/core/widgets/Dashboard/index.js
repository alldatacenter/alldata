import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
    GaugeChart
} from "bizcharts";
import _ from 'lodash'

@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
export default class Dashboard extends Component {
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
        let { theme, appendPadding, height, width, minNum, maxNum, dataRange, chartTitle, colorRange, chartContent, period, advancedConfig = {} } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        let data = 64
        let finalData = chartData || widgetData || data;
        if (dataRange && dataRange.indexOf(';') > -1) {
            let arr = dataRange.split(';');
            dataRange = arr.map(item => Number(item))
        }
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <GaugeChart
                theme={theme || 'light'}
                appendPadding={appendPadding || [10, 0, 0, 10]}
                title={{
                    visible: !!chartTitle,
                    text: chartTitle || '',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)',
                    }
                }}
                width={(width && Number(width))}
                height={(height && Number(height))}
                autoFit
                value={finalData}
                min={minNum || 0}
                max={maxNum || 100}
                range={dataRange || [0, 25, 50, 75, 100]}
                color={colorRange || ['#39B8FF', '#52619B', '#43E089', 'red']}
                statistic={{
                    title: {
                        content: chartContent || ''
                    },
                    content: {
                        content: finalData
                    }
                }}
                {...advConf}
            />
        );
    }
}
