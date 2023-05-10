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
import i18n from 'i18n';

interface IProps {
  [pro: string]: any;
  xAxisData: any[];
  data: any;
  period: any;
}

const StatusDetailChart = (props: IProps) => {
  const { xAxisData, data, period } = props;
  const getOption = () => {
    const timeFormatMap = {
      hour: 'mm:ss',
      day: 'HH:mm',
      week: 'MM-DD',
      month: 'MM-DD',
    };
    const options = {
      backgroundColor: '#fff',
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
        axisPointer: {
          lineStyle: {
            color: '#57617B',
          },
        },
      },
      grid: {
        left: '60',
        right: '40',
        bottom: '15%',
        top: '15%',
      },
      xAxis: [
        {
          type: 'category',
          boundaryGap: false,
          axisLine: {
            lineStyle: {
              color: '#57617B',
            },
          },
          data: xAxisData.map((item) => moment(item).format(timeFormatMap[period])),
          axisLabel: {
            interval: 0,
            rotate: 50,
            textStyle: {
              fontSize: 12,
            },
          },
        },
      ],
      yAxis: [
        {
          type: 'value',
          axisTick: {
            show: false,
          },
          axisLine: {
            show: false,
          },
          axisLabel: {
            textStyle: {
              fontSize: 12,
            },
          },
          splitLine: {
            show: false,
          },
        },
      ],
      series: [
        {
          name: i18n.t('response time'),
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
                    offset: 0.6,
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

export default StatusDetailChart;
