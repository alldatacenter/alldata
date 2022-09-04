import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
    BulletChart,
} from "bizcharts";
import _ from 'lodash'
@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
export default class Bullet extends Component {
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
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(nextProps.nodeParams, this.state.nodeParams)) {
            this.setState({
                nodeParams: _.cloneDeep(nextProps.nodeParams)
            })
        }
    }
    render() {
        const { widgetConfig = {}, widgetData } = this.props;
        let { chartData } = this.state;
        let { theme, appendPadding, height = '', width, xField, measureFeild, chartTitle, colorRange, targetField, measureColors, period, advancedConfig = {} } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        const data = [
            {
                title: '满意度',
                measures: [83],
                target: [90],
            },
            {
                title: '好评度',
                measures: [73],
                target: [90],
            },
            {
                title: '参评度',
                measures: [73],
                target: [94],
            },
        ];
        let finalData = chartData || widgetData || data;
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <BulletChart
                autoFit
                theme={theme || 'light'}
                data={finalData}
                appendPadding={appendPadding || [10, 0, 0, 10]}
                title={{
                    visible: !!chartTitle,
                    text: chartTitle || '子弹图',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)',
                    }
                }}
                width={(width && Number(width))}
                height={(height && Number(height))}
                measureField={measureFeild || 'measures'}
                rangeField='ranges'
                targetField={targetField || 'target'}
                xField={xField || 'title'}
                rangeMax={100}
                measureColors={measureColors || ['#43E089', '#52619B', '#43E089', 'red']}
                rangeColors={colorRange || ['#43E089', '#52619B', '#43E089', 'red']}
                {...advConf}
            />
        );
    }
}
