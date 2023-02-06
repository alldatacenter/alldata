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

/* eslint-disable no-undef */
import { IStaticData } from '@erda-ui/dashboard-configurator';
import { getFormatter } from './index';
import { cutStr } from 'common/utils';
import { areaColors } from '../theme';
import { map, sortBy, some } from 'lodash';
import moment from 'moment';

const calMax = (arr) => {
  let max = 0;
  arr.forEach((el) => {
    el.forEach((el1) => {
      if (!(el1 === undefined || el1 === '')) {
        if (max < Number(el1)) {
          max = Number(el1);
        }
      }
    });
  });
  const maxVal = Math.ceil((max / 9.5) * 10);
  return maxVal > 5 ? maxVal + (5 - (maxVal % 5)) : maxVal;
};

export const getLineOption = (data: IStaticData, optionExtra = {}) => {
  const { xData, metricData, yAxisLength, xAxisIsTime } = data;
  const {
    lines,
    yAxisNames = [],
    timeSpan,
    tooltipFormatter,
    legendFormatter,
    decimal = 2,
    isBarChangeColor,
    isLabel,
    noAreaColor,
  } = optionExtra;
  if (!metricData.length) {
    return {};
  }

  const moreThanOneDay = timeSpan ? timeSpan.seconds > 24 * 3600 : false;
  const results = sortBy(metricData, 'axisIndex');
  let yAxis = [] as object[];
  // 处理markLine
  const markLines = lines || [];
  let markLine = {};
  if (markLines.length) {
    markLine = {
      silent: true,
      label: {
        normal: {
          show: true,
          position: 'middle',
          formatter: (params) => {
            const uType = results[0].unitType;
            const { unit } = results[0];

            const y = getFormatter(uType, unit).format(params.data.yAxis, decimal || 2);
            return `${params.name}: ${y}`;
          },
        },
      },
      data: markLines.map(({ name, value }) => [
        { x: '7%', yAxis: value, name },
        { x: '93%', yAxis: value },
      ]),
    };
  }

  const series = [] as any[];
  const maxArr = [];
  map(metricData, (metric, i) => {
    const yAxisIndex = metric.axisIndex || 0;
    const areaColor = areaColors[i];

    series.push({
      type: metric.chartType || 'line',
      name: metric.tag || metric.name || metric.key,
      yAxisIndex,
      data: !isBarChangeColor
        ? metric.data
        : map(metric.data, (item, j) => {
            const sect = Math.ceil(metric.data.length / changeColors.length);
            return Object.assign({}, item, {
              itemStyle: { normal: { color: changeColors[Number.parseInt(j / sect, 10)] } },
            });
          }),
      label: {
        normal: {
          show: isLabel,
          position: 'top',
          formatter: (label) => {
            return label.data.label;
          },
        },
      },
      markLine: i === 0 ? markLine : {},
      connectNulls: true,
      symbol: 'emptyCircle',
      symbolSize: 1,
      barMaxWidth: 50,
      areaStyle: {
        normal: {
          color: noAreaColor ? 'transparent' : areaColor,
        },
      },
    });
    const curMax = metric.data ? calMax([metric.data]) : [];
    maxArr[yAxisIndex] = maxArr[yAxisIndex] && maxArr[yAxisIndex] > curMax ? maxArr[yAxisIndex] : curMax;
    const curUnitType = metric.unitType || ''; // y轴单位类型
    const curUnit = metric.unit || ''; // y轴单位
    yAxis[yAxisIndex] = {
      name: metric.name || yAxisNames[yAxisIndex] || '',
      nameTextStyle: {
        padding: [0, 0, 0, 5],
      },
      position: yAxisIndex === 0 ? 'left' : 'right',
      offset: 10,
      min: 0,
      splitLine: {
        show: true,
      },
      axisTick: {
        show: false,
      },
      axisLine: {
        show: false,
      },
      unitType: curUnitType,
      unit: curUnit,
      axisLabel: {
        margin: 0,
        formatter: (val) => getFormatter(curUnitType, metric.unit).format(val, decimal),
      },
    };
  });

  if (markLines.length && lines[0]) {
    const yMax = Math.min(lines[0].value * 1.05, Number.MAX_SAFE_INTEGER);
    if (!Number.isNaN(yMax)) {
      yAxis[0].max = yMax;
    }
  }

  const formatTime = (timeStr: string) => moment(Number(timeStr)).format(moreThanOneDay ? 'M月D日 HH:mm' : 'HH:mm');

  const getTTUnitType = (i: number) => {
    const curYAxis = yAxis[i] || yAxis[yAxis.length - 1];
    return [curYAxis.unitType, curYAxis.unit];
  };

  const genTTArray = (param) =>
    param.map((unit, i) => {
      return `<span style='color: ${unit.color}'>${cutStr(unit.seriesName, 20)} : ${getFormatter(
        ...getTTUnitType(i),
      ).format(unit.value, 2)}</span><br/>`;
    });

  let defaultTTFormatter = (param) => `${param[0].name}<br/>${genTTArray(param).join('')}`;

  if (xAxisIsTime) {
    defaultTTFormatter = (param) => {
      const endTime = xData[param[0].dataIndex + 1];
      if (!endTime) {
        return `${formatTime(param[0].name)}<br />${genTTArray(param).join('')}`;
      }
      return `${formatTime(param[0].name)} 到 ${formatTime(endTime)}<br/>${genTTArray(param).join('')}`;
    };
  }
  const lgFormatter = (name: string) => {
    const defaultName = legendFormatter ? legendFormatter(name) : name;
    return cutStr(defaultName, 20);
  };

  const haveTwoYAxis = yAxisLength > 1;
  if (haveTwoYAxis) {
    yAxis = yAxis.map((item, i) => {
      // 有数据和无数据的显示有差异
      const hasData = some(results[i].data || [], (_data) => Number(_data) !== 0);
      let { name } = item;
      if (!hasData) {
        name =
          i === 0 ? `${'  '.repeat(item.name.length + 1)}${item.name}` : `${item.name}${'  '.repeat(item.name.length)}`;
      }

      // if (i > 1) { // 右侧有超过两个Y轴
      //   yAxis[i].offset = 80 * (i - 1);
      // }
      const maxValue = item.max || maxArr[i];
      return { ...item, name, max: maxValue, interval: maxValue / 5 };
      // 如果有双y轴，刻度保持一致
    });
  } else {
    yAxis[0].name = yAxisNames[0] || '';
  }
  const defaultOption = {
    tooltip: {
      trigger: 'axis',
      transitionDuration: 0,
      confine: true,
      axisPointer: {
        type: 'none',
      },
      formatter: tooltipFormatter || defaultTTFormatter,
    },
    legend: {
      bottom: 10,
      padding: [15, 5, 0, 5],
      orient: 'horizontal',
      align: 'left',
      // data: legendData,
      formatter: lgFormatter,
      type: 'scroll',
      tooltip: {
        show: true,
        formatter: (t) => cutStr(t.name, 100),
      },
    },
    grid: {
      top: haveTwoYAxis ? 30 : 25,
      left: 15,
      right: haveTwoYAxis ? 30 : 5,
      bottom: 40,
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        // data: xAxis || time || [], /* X轴数据 */
        axisTick: {
          show: false /* 坐标刻度 */,
        },
        axisLine: {
          show: false,
        },
        axisLabel: {
          formatter: (value: string | number) => moment(Number(value)).format(moreThanOneDay ? 'M/D HH:mm' : 'HH:mm'),
        },
        splitLine: {
          show: false,
        },
      },
    ],
    yAxis,
    textStyle: {
      fontFamily: 'arial',
    },
    series,
  };

  return defaultOption;
};
