import React, { Component } from 'react';
import { LiquidChart } from 'bizcharts';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import _ from 'lodash'
@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
export default class FluidCharts extends Component {
    constructor(props) {
        super(props);
        this.state = {
            chartData: null,
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
        let { theme, appendPadding, height, width, minNum, maxNum, chartTitle, period, fluidColor, advancedConfig = {}, describeTitle } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        let data = 2600;
        let finalData = chartData || widgetData || data;
        if(chartData=== 0 || widgetData === 0) {
            finalData = 0
        }
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <LiquidChart
                autoFit
                theme={theme || 'light'}
                appendPadding={appendPadding || [10, 0, 0, 10]}
                title={{
                    visible: !!chartTitle,
                    text: chartTitle || '',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)'
                    }
                }}
                min={Number(minNum) || 0}
                max={Number(maxNum) || 10000}
                height={height && Number(height)}
                width={width && Number(width)}
                value={finalData}
                color={fluidColor && fluidColor.color}
                statistic={{
                    title: {
                        content: describeTitle || '',
                        style: {
                            fontSize: 16,
                        },
                    },
                    content: {
                        style: {
                            fill: "#000"
                        },
                        customHtml(container, view, item) {
                            return `${(item.percent * 100).toFixed(2)}%`
                        }
                    }
                }
                }
                {...advConf}
            />
        );
    }
}
