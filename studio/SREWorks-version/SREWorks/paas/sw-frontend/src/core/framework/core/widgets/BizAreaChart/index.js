import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
    AreaChart
} from "bizcharts";
import _ from 'lodash'

@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
export default class BizAreaChart extends Component {
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
        let { theme, appendPadding, height, width, smooth, chartTitle, isLegend, legendPosition, xField, yField, period, advancedConfig = {} } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        let data = [
            { year: '1971', value: 44 },
            { year: '1972', value: 40 },
            { year: '1974', value: 45 },
            { year: '1974', value: 37 },
            { year: '1975', value: 39 },
            { year: '1976', value: 36 },
            { year: '1977', value: 37 },
            { year: '1978', value: 34 },
            { year: '1979', value: 34 },
            { year: '1980', value: 37 },
            { year: '1981', value: 44 },
            { year: '1982', value: 40 },
            { year: '1983', value: 38 },
            { year: '1984', value: 48 },
            { year: '1985', value: 49 },
            { year: '1986', value: 46 },
            { year: '1987', value: 47 },
            { year: '1988', value: 33 },
            { year: '1989', value: 34 },
            { year: '1990', value: 27 },
            { year: '1991', value: 18 },
            { year: '1992', value: 19 },
            { year: '1993', value: 12 },
            { year: '1994', value: 15 },
            { year: '1995', value: 11 },
            { year: '1996', value: 7 },
            { year: '1997', value: 13 },
            { year: '1998', value: 5 },
            { year: '1999', value: 3 },

        ];
        let finalData = chartData || widgetData || data;
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <AreaChart
                theme={theme || 'light'}
                appendPadding={appendPadding || [10, 0, 0, 10]}
                title={{
                    visible: chartTitle ? true : false,
                    text: chartTitle || '面积图',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)',
                    }
                }}
                width={(width && Number(width))}
                height={(height && Number(height))}
                autoFit
                xField={xField || 'year'}
                yField={yField || 'value'}
                smooth={smooth}
                legend={{
                    visible: isLegend,
                    position: legendPosition
                }}
                data={finalData}
                {...advConf}
            />
        );
    }
}
