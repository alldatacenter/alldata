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

import EChart from 'charts/components/echarts';
import { newColorMap } from 'config-page/utils';
import { TextBlockInfo } from 'common';
import echarts from 'echarts/lib/echarts';
import React from 'react';
import './simple-chart.scss';

const getOption = (chart: CP_SIMPLE_CHART.IData['chart']) => {
  return {
    backgroundColor: 'transparent',
    xAxis: {
      show: false,
      type: 'category',
      boundaryGap: false,
      data: chart.xAxis,
    },
    yAxis: {
      type: 'value',
      show: false,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(48,38,71,0.96)',
      borderWidth: 0,
      padding: [8, 16],
      formatter: (params: Obj[]) => {
        const serie = params[0];
        return `<div style="color:rgba(255,255,255,0.60);margin-bottom:8px;">${serie.name}</div>
        <div>
        <span>${serie.seriesName}</span>
        <span style='margin-left:10px;'>${serie.data}</span>
        </div>`;
      },
      textStyle: {
        color: '#fff',
      },
      axisPointer: {
        type: 'line',
        label: {
          show: false,
        },
        lineStyle: {
          type: 'dashed',
          color: 'rgba(48,38,71,0.40)',
        },
      },
    },
    series: chart.series.map((serie, index) => {
      return {
        name: serie.name,
        data: serie.data,
        type: 'line',
        smooth: false,
        lineStyle: {
          width: 3,
        },
        itemStyle: {
          color: newColorMap.warning4,
        },
        showSymbol: false,
        areaStyle: {
          opacity: 0.5,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: newColorMap.warning4,
            },
            {
              offset: 1,
              color: 'rgba(255,255,255,0.9)',
            },
          ]),
        },
      };
    }),
  };
};

const SimpleChart = (props: CP_SIMPLE_CHART.Props) => {
  const { execOperation, data, props: configProps, operations } = props;
  const { main, sub, compareText, compareValue, chart } = data || {};
  const { style } = configProps || {};

  return (
    <div className="cp-simple-chart flex items-center p-4" style={style}>
      <TextBlockInfo
        className="flex justify-center"
        main={main}
        sub={sub}
        desc={
          <div>
            <span className="color-primary font-bold mr-1">{compareText}</span>
            <span className={`color-sub ${+(compareValue || 0) >= 0 ? 'text-success' : 'text-error'}`}>
              {compareValue}
            </span>
          </div>
        }
      />
      <EChart option={getOption(chart)} notMerge style={{ height: 64 }} className="flex-1" />
    </div>
  );
};

export default SimpleChart;
