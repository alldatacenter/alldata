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

import { map } from 'lodash';
import React from 'react';
import { produce } from 'immer';
import ChartRender from './chart-render';

class MapChart extends React.Component {
  getOption = () => {
    const { formatter, seriseName, data } = this.props;
    const { results } = data;
    const option = {
      tooltip: {
        trigger: 'item',
        formatter,
      },
      visualMap: {
        type: 'piecewise',
        pieces: data.pieces || [{ gte: 5 }, { lt: 5 }],
        left: 'left',
        top: 'bottom',
        calculable: true,
      },
      series: map(results, (v) => {
        // 去除南海诸岛的显示：南海诸岛属于海南，本地图只需显示省级
        const value = produce(v, (draftData) => {
          draftData.data.push({
            name: '南海诸岛',
            itemStyle: {
              normal: {
                opacity: 0,
                label: { show: false },
                borderWidth: '0',
                borderColor: '#10242b',
                areaStyle: { color: '#10242b' },
              },
              emphasis: { opacity: 0, label: { show: false } },
            },
          });
        });
        return {
          name: value.type || seriseName || value.name || value.key,
          type: 'map',
          mapType: 'china',
          roam: true,
          scaleLimit: {
            min: 0.9,
            max: 6,
          },
          layoutCenter: ['50%', '50%'],
          layoutSize: '130%',
          label: {
            normal: {
              show: true,
            },
            emphasis: {
              show: true,
            },
          },
          data: value.data,
        };
      }),
    };
    return option;
  };

  render() {
    const { mapData } = this.props;
    const hasData = mapData && mapData.features && mapData.features.length > 0;
    return <ChartRender {...this.props} hasData={hasData} getOption={this.getOption} />;
  }
}

export default MapChart;
