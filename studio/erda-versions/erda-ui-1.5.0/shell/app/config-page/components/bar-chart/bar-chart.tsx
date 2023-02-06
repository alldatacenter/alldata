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

import React from 'react';
import EChart from 'charts/components/echarts';
import { newColorMap, getClass } from 'config-page/utils';
import { EmptyHolder } from 'common';
import { map, uniq, merge, get } from 'lodash';
import theme from 'app/themes/theme';
import './bar-chart.scss';

const handleAxis = (opt: Obj, yAxisLabelLen = 10) => {
  const reOpt = { ...opt };
  if (opt.yAxis) {
    const adjustYAxis = (_axis: Obj) => {
      return {
        ..._axis,
        axisLabel: {
          formatter: (v: string) => (v.length > yAxisLabelLen ? `${v.substr(0, yAxisLabelLen)}...` : v),
          color: theme.chart.yAxisColor,
          ...(_axis.axisLabel || {}),
        },
      };
    };

    reOpt.yAxis = Array.isArray(reOpt.yAxis) ? reOpt.yAxis.map(adjustYAxis) : adjustYAxis(reOpt.yAxis);
  }

  if (opt.xAxis) {
    const adjustXAxis = (_axis: Obj) => {
      return {
        ..._axis,
        axisLabel: {
          color: theme.chart.xAxisColor,
          ...(_axis.axisLabel || {}),
        },
      };
    };

    reOpt.xAxis = Array.isArray(reOpt.xAxis) ? reOpt.xAxis.map(adjustXAxis) : adjustXAxis(reOpt.xAxis);
  }

  return reOpt;
};

const getOption = (option: Obj, configProps: CP_BAR_CHART.IProps) => {
  const { yAxisLabelLen, grayBg } = configProps || {};
  const commonOp: Obj = {
    grid: {
      bottom: 15,
      containLabel: true,
      right: 20,
      left: 20,
      top: 15,
    },
    xAxis: { splitLine: { show: false } },
  };
  let reOption = {
    ...(grayBg ? { backgroundColor: '' } : {}),
    tooltip: { trigger: 'axis' },
    yAxis: { type: 'value' },
    ...option,
    series: option.series.map((item: Obj) => ({
      type: 'bar',
      itemStyle: {
        normal: {
          barBorderRadius: [15, 15, 0, 0],
        },
      },
      ...item,
      label: item.label ? { show: true, ...item.label } : undefined,
    })),
  };
  if (option.legend) {
    reOption = merge(
      {
        legend: {
          itemWidth: 8,
          itemGap: 20,
          itemHeight: 8,
          bottom: 5,
          textStyle: { fontSize: 12, color: 'rgba(0,0,0,0.6)' },
        },
        grid: { bottom: 40 },
      },
      reOption,
    );
  }

  const isEmpty = !reOption.series?.filter((item: Obj) => item?.data)?.length;
  return {
    option: handleAxis(merge(commonOp, reOption), yAxisLabelLen),
    isEmpty,
  };
};

const CP_BarChart = (props: CP_BAR_CHART.Props) => {
  const { cId, data, props: configProps, extraContent, operations, execOperation } = props;
  const { yAxisLabelLen, title, tip, visible = true, chartStyle, ...rest } = configProps || {};
  const { option } = data || {};
  const { color, ...optionRest } = option || {};
  const presetColor = map(newColorMap);
  const reColor = color ? uniq(map(color, (cItem) => newColorMap[cItem] || cItem).concat(presetColor)) : presetColor;

  if (!visible) return null;

  const onEvents = {
    click: (params: any) => {
      const dataOp = get(params, 'data.operations.click') || operations?.click;
      if (dataOp) {
        execOperation(dataOp, {
          data: params.data,
          seriesIndex: params.seriesIndex,
          dataIndex: params.dataIndex,
        });
      }
    },
  };

  const { option: reOption, isEmpty } = getOption({ color: reColor, ...optionRest }, configProps);
  const ChartComp = <EChart key={cId} onEvents={onEvents} option={reOption} notMerge {...rest} style={chartStyle} />;

  return (
    <div className={`cp-bar-chart ${getClass(configProps)}`}>{isEmpty ? <EmptyHolder relative /> : ChartComp}</div>
  );
};

export default CP_BarChart;
