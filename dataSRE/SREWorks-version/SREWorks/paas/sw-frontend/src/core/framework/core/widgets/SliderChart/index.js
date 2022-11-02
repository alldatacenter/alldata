import React, { Component } from 'react';
import { connect } from 'dva';
import { loadChartData } from '../../../../../utils/loadChartData';
import safeEval from '../../../../../utils/SafeEval';
import {
	DonutChart
} from "bizcharts";
import _ from 'lodash'
@connect(({ node }) => ({
	nodeParams: node.nodeParams,
}))
class SliderChart extends Component {
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
		let { theme, appendPadding, height = '', width, chartTitle, isLegend, legendPosition, angleFeild, colorFeild, outRadius, innerRadius, staticTitle, staticContent, advancedConfig = {}, period } = widgetConfig;
		if (appendPadding && appendPadding.indexOf(',') > -1) {
			appendPadding = appendPadding.split(',').map(item => Number(item))
		}
		const data = [
			{
				type: '分类一',
				value: 27,
			},
			{
				type: '分类二',
				value: 25,
			},
			{
				type: '分类三',
				value: 18,
			},
			{
				type: '分类四',
				value: 15,
			},
			{
				type: '分类五',
				value: 10,
			},
			{
				type: '其它',
				value: 5,
			},
		];
		let finalData = chartData || widgetData || data;
		let advConf = {};
		if (advancedConfig && advancedConfig.length > 40) {
			advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
		}
		return (
			<DonutChart
				width={(width && Number(width))}
				height={(height && Number(height))}
				theme={theme || 'light'}
				padding={appendPadding || 'auto'}
				data={finalData}
				autoFit={true}
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
				angleField={angleFeild || 'value'}
				colorField={colorFeild || 'type'}
				radius={outRadius || 0.8}
				innerRadius={innerRadius || 0.8}
				statistic={{
					title: false,
					content: false
				}}
				{...advConf}

			/>
		);
	}
}

export default SliderChart;