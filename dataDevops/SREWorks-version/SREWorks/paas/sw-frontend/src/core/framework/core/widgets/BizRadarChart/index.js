import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
  RadarChart
} from "bizcharts";
import _ from "lodash"

@connect(({ node }) => ({
  nodeParams: node.nodeParams,
}))
export default class BizRadarChart extends Component {
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
    let { theme, appendPadding, height, width, chartTitle, angleField, radiusField, seriesField, period, isLegend, legendPosition, advancedConfig = {} } = widgetConfig;
    if (appendPadding && appendPadding.indexOf(',') > -1) {
      appendPadding = appendPadding.split(',').map(item => Number(item))
    }
    const data = [
      {
        item: 'Design',
        user: 'a',
        score: 70,
      },
      {
        item: 'Design',
        user: 'b',
        score: 30,
      },
      {
        item: 'Development',
        user: 'a',
        score: 60,
      },
      {
        item: 'Development',
        user: 'b',
        score: 70,
      },
      {
        item: 'Marketing',
        user: 'a',
        score: 60,
      },
      {
        item: 'Marketing',
        user: 'b',
        score: 50,
      },
      {
        item: 'Users',
        user: 'a',
        score: 40,
      },
      {
        item: 'Users',
        user: 'b',
        score: 50,
      },
      {
        item: 'Test',
        user: 'a',
        score: 60,
      },
      {
        item: 'Test',
        user: 'b',
        score: 70,
      },
      {
        item: 'Language',
        user: 'a',
        score: 70,
      },
      {
        item: 'Language',
        user: 'b',
        score: 50,
      },
      {
        item: 'Technology',
        user: 'a',
        score: 50,
      },
      {
        item: 'Technology',
        user: 'b',
        score: 40,
      },
      {
        item: 'Support',
        user: 'a',
        score: 30,
      },
      {
        item: 'Support',
        user: 'b',
        score: 40,
      },
      {
        item: 'Sales',
        user: 'a',
        score: 60,
      },
      {
        item: 'Sales',
        user: 'b',
        score: 40,
      },
      {
        item: 'UX',
        user: 'a',
        score: 50,
      },
      {
        item: 'UX',
        user: 'b',
        score: 60,
      },
    ];
    let finalData = chartData || widgetData || data;
    let advConf = {};
    if (advancedConfig && advancedConfig.length > 40) {
      advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
    }
    return (
      <RadarChart
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
        angleField={angleField || 'item'}
        radiusField={radiusField || 'score'}
        seriesField={seriesField || 'user'}
        radiusAxis={{
          grid: {
            line: {
              type: 'line',
            },
          },
        }}
        line={{
          visible: true,
        }}
        point={{
          visible: true,
          shape: 'circle',
        }}
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
