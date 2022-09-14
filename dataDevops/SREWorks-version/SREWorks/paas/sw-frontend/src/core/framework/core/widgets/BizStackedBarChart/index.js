import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
  StackedBarChart
} from "bizcharts";
import _ from 'lodash'

@connect(({ node }) => ({
  nodeParams: node.nodeParams,
}))
export default class BizStackedBarChart extends Component {
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
    let { theme, appendPadding, height, width, chartTitle, xField, yField, stackField, period, isLegend, legendPosition, advancedConfig = {} } = widgetConfig;
    if (appendPadding && appendPadding.indexOf(',') > -1) {
      appendPadding = appendPadding.split(',').map(item => Number(item))
    }
    const data = [
      {
        地区: '华东',
        细分: '公司',
        销售额: 1454715.807999998,
      },
      {
        地区: '华东',
        细分: '消费者',
        销售额: 2287358.261999998,
      },
      {
        地区: '中南',
        细分: '公司',
        销售额: 1335665.3239999984,
      },
      {
        地区: '中南',
        细分: '消费者',
        销售额: 2057936.7620000008,
      },
      {
        地区: '东北',
        细分: '公司',
        销售额: 834842.827,
      },
      {
        地区: '东北',
        细分: '消费者',
        销售额: 1323985.6069999991,
      },
      {
        地区: '华北',
        细分: '公司',
        销售额: 804769.4689999995,
      },
      {
        地区: '华北',
        细分: '消费者',
        销售额: 1220430.5610000012,
      },
      {
        地区: '西南',
        细分: '公司',
        销售额: 469341.684,
      },
      {
        地区: '西南',
        细分: '消费者',
        销售额: 677302.8919999995,
      },
      {
        地区: '西北',
        细分: '公司',
        销售额: 253458.1840000001,
      },
      {
        地区: '西北',
        细分: '消费者',
        销售额: 458058.1039999998,
      },
    ];
    let finalData = chartData || widgetData || data;
    let advConf = {};
    if (advancedConfig && advancedConfig.length > 40) {
      advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
    }
    return (
      <StackedBarChart
        theme={theme || 'light'}
        appendPadding={appendPadding || [10, 0, 0, 10]}
        data={finalData}
        width={(width && Number(width))}
        height={(height && Number(height))}
        autoFit
        title={{
          visible: chartTitle ? true : false,
          text: chartTitle || '',
          style: {
            fontSize: 14,
            color: 'var(--PrimaryColor)',
          }
        }}
        xField={xField || '销售额'}
        yField={yField || '地区'}
        isStack
        stackField={stackField || '细分'}

        xAxis={{
          // type: 'dateTime',
          tickCount: 5,
        }}
        legend={{
          visible: isLegend,
          position: legendPosition,
          flipPage: true
        }}
        {...advConf}
      />
    );
  }
}
