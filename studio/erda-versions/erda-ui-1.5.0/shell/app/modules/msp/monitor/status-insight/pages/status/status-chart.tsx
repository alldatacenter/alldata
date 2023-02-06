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

import { size } from 'lodash';
import moment from 'moment';
import React from 'react';
import Echarts from 'charts/components/echarts';
import { LinearGradient } from 'echarts/lib/util/graphic';

interface IProps {
  [pro: string]: any;
  xAxisData: any[];
  data: any;
}

const StatusChart = (props: IProps) => {
  const { xAxisData, data } = props;
  const getOption = () => {
    const options = {
      title: {
        text: '',
        textStyle: {
          fontWeight: 'normal',
          fontSize: 12,
          color: '#F1F1F3',
        },
        left: '6%',
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{c0} ms',
        confine: true,
        axisPointer: {
          lineStyle: {
            color: '#57617B',
          },
        },
      },
      grid: {
        left: '0%',
        right: '0%',
        bottom: '0',
        top: '4',
      },
      xAxis: [
        {
          show: false,
          type: 'category',
          boundaryGap: false,
          data: xAxisData.map((item) => moment(item).format('HH:mm')),
        },
      ],
      yAxis: [
        {
          type: 'value',
          show: false,
          axisTick: {
            show: false,
          },
        },
      ],
      series: [
        {
          name: '',
          type: 'line',
          smooth: true,
          showSymbol: false,
          symbolSize: 0,
          lineStyle: {
            normal: {
              width: 1,
            },
          },
          areaStyle: {
            normal: {
              color: new LinearGradient(
                0,
                0,
                0,
                1,
                [
                  {
                    offset: 0,
                    color: 'rgba(1, 108, 209, 0.8)',
                  },
                  {
                    offset: 0.8,
                    color: 'rgba(1, 108, 209, 0.1)',
                  },
                ],
                false,
              ),
              shadowColor: 'rgba(228, 139, 76, 0.3)',
              shadowBlur: 10,
            },
          },
          itemStyle: {
            normal: {
              color: 'rgb(1, 108, 209)',
              borderColor: '#e48b4c',
            },
          },
          data,
        },
      ],
    };
    return options;
  };
  return <Echarts {...props} hasData={size(data) > 0} option={getOption()} />;
};
export default StatusChart;
