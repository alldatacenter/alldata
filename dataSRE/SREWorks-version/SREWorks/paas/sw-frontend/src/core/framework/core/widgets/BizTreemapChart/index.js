import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
  TreemapChart
} from "bizcharts";
import _ from 'lodash'

@connect(({ node }) => ({
  nodeParams: node.nodeParams,
}))
export default class BizTreemapChart extends Component {
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
    let { theme, appendPadding, height, width, chartTitle, period, isLegend, legendPosition, advancedConfig = {} } = widgetConfig;
    if (appendPadding && appendPadding.indexOf(',') > -1) {
      appendPadding = appendPadding.split(',').map(item => Number(item))
    }
    // 数据源
    const data = {
      name: 'root',
      value: 2237,
      children: [
        { name: '分类 1', value: 560 },
        { name: '分类 2', value: 500 },
        { name: '分类 3', value: 150 },
        { name: '分类 4', value: 140 },
        { name: '分类 5', value: 115 },
        { name: '分类 6', value: 95 },
        { name: '分类 7', value: 90 },
        { name: '分类 8', value: 75 },
        { name: '分类 9', value: 98 },
        { name: '分类 10', value: 60 },
        { name: '分类 11', value: 45 },
        { name: '分类 12', value: 40 },
        { name: '分类 13', value: 40 },
        { name: '分类 14', value: 35 },
        { name: '分类 15', value: 40 },
        { name: '分类 16', value: 40 },
        { name: '分类 17', value: 40 },
        { name: '分类 18', value: 30 },
        { name: '分类 19', value: 28 },
        { name: '分类 20', value: 16 },
      ],
    };
    let finalData = chartData || widgetData || data;
    let advConf = {};
    if (advancedConfig && advancedConfig.length > 40) {
      advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
    }
    return (
      <TreemapChart
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
        xAxis={{
          // type: 'dateTime',
          tickCount: 5,
        }}
        legend={{
          visible: isLegend,
          position: legendPosition
        }}
        {...advConf}
      />
    );
  }
}
