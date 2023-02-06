// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { map, size } from 'lodash';
import React from 'react';
import ChartRender from './chart-render';
import i18n from 'i18n';

class BarChart extends React.Component {
  getOption = () => {
    const { legendData, data } = this.props;

    const option = {
      backgroundColor: '#fff',
      legend: {
        x: 'center',
        y: 'bottom',
        data: legendData,
      },
      tooltip: {
        trigger: 'item',
        axisPointer: {
          type: 'shadow',
        },
        formatter: `{a} <br/>{b}: {c} <br/>${i18n.t('charts:proportion')}: ({d}%)`,
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '9%',
        containLabel: true,
      },
      xAxis: [
        {
          type: 'category',
          data: ['Mon', 'Tue', 'Wed'],
          axisLabel: { show: false },
          splitLine: { lineStyle: { type: 'dashed' } },
          axisTick: {
            show: false,
            alignWithLabel: true,
          },
        },
      ],
      yAxis: [
        {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
        },
      ],
      series: map(data, (value) => {
        return {
          name: value.name,
          type: 'bar',
          barWidth: '10%',
          label: {
            normal: {
              show: true,
              position: 'top',
            },
          },
          data: [value.value],
        };
      }),
    };
    return option;
  };

  render() {
    return <ChartRender {...this.props} hasData={size(this.props.data) > 0} getOption={this.getOption} />;
  }
}

export default BarChart;
