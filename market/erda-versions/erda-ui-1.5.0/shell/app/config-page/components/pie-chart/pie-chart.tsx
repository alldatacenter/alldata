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
import { newColorMap, colorMap as pColorMap, getClass, getFormatterString } from 'config-page/utils';
import { EmptyHolder, TextBlockInfo } from 'common';
import { map, uniq, merge, get, sumBy } from 'lodash';
import { bgColor } from 'app/charts/theme';
import classnames from 'classnames';

import './pie-chart.scss';

const colorMap = { ...pColorMap, ...newColorMap };

const getOption = (data: CP_PIE_CHART.IList[], _option: Obj, configProps: Obj, label?: string) => {
  const { total, centerLabel, value } = data?.[0] || {};
  let reData = data;
  if (data?.length === 1 && total !== undefined && total >= value) {
    reData = [...data, { name: '', value: total - value, color: bgColor }];
  }
  const color = map(reData || [], (item) => item.color && (colorMap[item.color] || item.color));
  const reLabel = label || centerLabel;
  const option = {
    ...(configProps.grayBg ? { backgroundColor: '' } : {}),
    ...(reLabel
      ? {
          graphic: {
            left: 'center',
            type: 'text',
            top: 'center',
            style: {
              text: reLabel,
              fill: 'rgba(0,0,0,0.6)',
            },
          },
        }
      : {}),
    series: [
      {
        type: 'pie',
        color: color.length ? color : undefined,
        radius: ['70%', '100%'],
        label: { show: false },
        emphasis: { scale: false },
        hoverAnimation: false,
        data: reData?.map(({ name, value: v }) => {
          return { name, value: v };
        }),
      },
    ],
  };
  const isEmpty = !data?.length;

  return {
    option: merge({}, _option, option),
    isEmpty,
  };
};

const ChartItem = (props: CP_PIE_CHART.Props) => {
  const { data: pData, props: configProps, operations, execOperation } = props;
  const {
    visible,
    style = {},
    chartStyle,
    textInfoStyle = {},
    size = 'normal',
    direction = 'row',
    title,
    tip,
    option,
    ...rest
  } = configProps;
  const { data, label } = pData;
  const styleMap = {
    small: {
      chartStyle: { width: 56, height: 56 },
      infoCls: { col: 'mt-3', row: 'ml-3' },
    },
    normal: {
      chartStyle: { width: 100, height: 100 },
      infoCls: { col: 'mt-3', row: 'ml-3' },
    },
    big: {
      chartStyle: { width: 180, height: 180 },
      infoCls: { col: 'mt-3', row: 'ml-3' },
    },
  };
  const styleObj = styleMap[size];
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

  const reChartStyle = chartStyle || styleObj?.chartStyle;
  const chartSetting = reChartStyle?.chartSetting || 'center';
  const cls = classnames({
    'items-center': chartSetting === 'center',
    'items-start': chartSetting === 'start',
  });

  const color = map(data, 'color');
  const presetColor = map(colorMap);
  const reColor = color ? uniq(map(color, (cItem) => colorMap[cItem] || cItem).concat(presetColor)) : presetColor;

  const { option: reOption, isEmpty } = getOption(data, option, configProps, label);
  const finalColor = reOption.color || reColor;
  const ChartComp = (
    <EChart onEvents={onEvents} option={{ color: reColor, ...reOption }} notMerge style={reChartStyle} {...rest} />
  );

  const total = data?.[0].total || sumBy(data, 'value');

  const Comp = (
    <div className={`flex ${cls}`}>
      {ChartComp}
      <div className={`flex-1 flex justify-between ${styleObj?.infoCls?.[direction]}`} style={{ minWidth: 100 }}>
        {data?.map((item: CP_PIE_CHART.IList, idx: number) => {
          if (item.info?.length) {
            return (
              <div className="flex items-center w-full justify-around">
                {item.info.map((infoItem) => (
                  <TextBlockInfo {...infoItem} size="small" style={textInfoStyle} />
                ))}
              </div>
            );
          }
          return (
            <TextBlockInfo
              size="small"
              key={item.name}
              className="flex-1"
              style={textInfoStyle}
              main={`${item.formatter ? getFormatterString(item.formatter, { v: item.value }) : item.value}`}
              sub={total ? `${((item.value * 100) / total).toFixed(1)}%` : '-'}
              extra={
                <div className="flex items-center">
                  <span className="rounded" style={{ width: 4, height: 10, backgroundColor: finalColor[idx] }} />
                  <span className="ml-1 text-desc text-xs leading-5">{item.name}</span>
                </div>
              }
            />
          );
        })}
      </div>
    </div>
  );
  return isEmpty ? <EmptyHolder relative /> : Comp;
};

const PieChart = (props: CP_PIE_CHART.Props) => {
  const { cId, props: configProps, extraContent, operations, execOperation, data: pData } = props;
  const { visible = true, style } = configProps || {};

  const { data, group } = pData || {};

  if (!visible) return null;

  let Comp = null;
  if (data) {
    Comp = <ChartItem {...props} />;
  } else if (group) {
    Comp = group.map((item) => (
      <div className="cp-pie-chart-box flex items-center justify-between">
        {item.map((g, idx) => (
          <ChartItem key={`${idx}`} {...props} data={{ data: g }} />
        ))}
      </div>
    ));
  }

  return (
    <div
      className={`cp-pie-chart p-3 flex flex-col overflow-y-auto justify-between ${getClass(configProps)}`}
      style={style}
    >
      {Comp}
    </div>
  );
};

export default PieChart;
