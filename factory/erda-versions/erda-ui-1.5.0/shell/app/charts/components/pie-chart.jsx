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

class PieChart extends React.Component {
  getOption = () => {
    const { data, legendData } = this.props;
    const label = {
      normal: {
        show: true,
      },
      emphasis: {
        show: true,
        textStyle: {
          fontSize: '20',
          fontWeight: 'bold',
        },
      },
    };
    const option = {
      tooltip: {
        trigger: 'item',
        confine: true,
        formatter: (params) => {
          const { seriesName, name, value, percent, marker } = params;
          return `${seriesName} <br/> <span> ${marker}${name} : ${value?.toFixed(2)} (${percent}%) </span>`;
        },
      },
      legend: {
        orient: 'vertical',
        right: 'right',
        // left: 15,
        top: 15,
        data: legendData,
        formatter: (name) => {
          return name.length > 10 ? `${name.substr(0, 10)}...` : name;
        },
        textStyle: {
          color: '#ccc',
        },
      },
      series: map(data.results, (serie) => {
        return {
          name: serie.name,
          type: 'pie',
          radius: '55%',
          center: ['50%', '46%'],
          label,
          itemStyle: {
            emphasis: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          data: serie.data,
        };
      }),
    };
    return option;
  };

  render() {
    return <ChartRender {...this.props} hasData={size(this.props.data.results) > 0} getOption={this.getOption} />;
  }
}

export default PieChart;
