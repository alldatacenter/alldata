import React, { Component } from 'react';
import { loadChartData } from '../../../../../utils/loadChartData';
import { connect } from 'dva';
import safeEval from '../../../../../utils/SafeEval';
import {
  StackedColumnChart
} from "bizcharts";
import _ from 'lodash'

@connect(({ node }) => ({
  nodeParams: node.nodeParams,
}))
export default class BizStackedColumnChart extends Component {
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
        year: '1991',
        value: 3,
        type: 'Lon',
      },
      {
        year: '1992',
        value: 4,
        type: 'Lon',
      },
      {
        year: '1993',
        value: 3.5,
        type: 'Lon',
      },
      {
        year: '1994',
        value: 5,
        type: 'Lon',
      },
      {
        year: '1995',
        value: 4.9,
        type: 'Lon',
      },
      {
        year: '1996',
        value: 6,
        type: 'Lon',
      },
      {
        year: '1997',
        value: 7,
        type: 'Lon',
      },
      {
        year: '1998',
        value: 9,
        type: 'Lon',
      },
      {
        year: '1999',
        value: 13,
        type: 'Lon',
      },
      {
        year: '1991',
        value: 3,
        type: 'Bor',
      },
      {
        year: '1992',
        value: 4,
        type: 'Bor',
      },
      {
        year: '1993',
        value: 3.5,
        type: 'Bor',
      },
      {
        year: '1994',
        value: 5,
        type: 'Bor',
      },
      {
        year: '1995',
        value: 4.9,
        type: 'Bor',
      },
      {
        year: '1996',
        value: 6,
        type: 'Bor',
      },
      {
        year: '1997',
        value: 7,
        type: 'Bor',
      },
      {
        year: '1998',
        value: 9,
        type: 'Bor',
      },
      {
        year: '1999',
        value: 13,
        type: 'Bor',
      },
    ];
    let finalData = chartData || widgetData || data;
    let advConf = {};
    if (advancedConfig && advancedConfig.length > 40) {
      advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
    }
    return (
      <StackedColumnChart
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
        xField={xField || 'year'}
        yField={yField || 'value'}
        isStack
        stackField={stackField || 'type'}
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
