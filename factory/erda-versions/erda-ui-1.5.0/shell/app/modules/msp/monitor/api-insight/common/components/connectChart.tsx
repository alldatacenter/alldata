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

// 适配多组多维度数据的 render
import { map, merge, size, isEmpty, sortBy, some, keys, values } from 'lodash';
import moment from 'moment';
import React from 'react';
import { IF } from 'common';
import { cutStr } from 'common/utils';
import { Select } from 'antd';
import { getFormatter } from 'app/charts/utils/formatter';
import { areaColors } from 'app/charts/theme';
import ChartRender from 'app/charts/components/chart-render';
import i18n from 'i18n';

const { Option } = Select;

const CHANGE_COLORS = ['rgb(0, 209, 156)', 'rgb(251, 162, 84)', 'rgb(247, 91, 96)'];

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

const ConnectChart = (props) => {
  const {
    data,
    seriseName,
    legendFormatter,
    decimal = 2,
    isBarChangeColor,
    tooltipFormatter,
    yAxisNames = [],
    isLabel,
    noAreaColor,
    timeSpan,
    opt,
  } = props;
  const groups = keys(data.results);
  const [selectedGroup, setSelectedGroup] = React.useState();
  const getOption = () => {
    const moreThanOneDay = timeSpan ? timeSpan.seconds > 24 * 3600 : false;
    const { results: originData, xAxis, time, lines } = data;
    const results = sortBy(originData[selectedGroup] || values(originData)[0], 'axisIndex');
    const legendData = [];
    let yAxis = [];
    const series = [];
    const maxArr = [];

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

    map(results, (value, i) => {
      const { axisIndex, name, tag, unit } = value;
      (tag || name) && legendData.push({ name: tag || name });
      const yAxisIndex = axisIndex || 0;
      const areaColor = areaColors[i];
      series.push({
        type: value.chartType || 'line',
        name: value.tag || seriseName || value.name || value.key,
        yAxisIndex,
        data: !isBarChangeColor
          ? value.data
          : map(value.data, (item, j) => {
              const sect = Math.ceil(value.data.length / CHANGE_COLORS.length);
              return { ...item, itemStyle: { normal: { color: CHANGE_COLORS[Number.parseInt(j / sect, 10)] } } };
            }),
        label: {
          normal: {
            show: isLabel,
            position: 'top',
            formatter: (label) => label.data.label,
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
      const curMax = value.data ? calMax([value.data]) : [];
      maxArr[yAxisIndex] = maxArr[yAxisIndex] && maxArr[yAxisIndex] > curMax ? maxArr[yAxisIndex] : curMax;
      const curUnitType = value.unitType || ''; // y轴单位
      const curUnit = value.unit || ''; // y轴单位
      yAxis[yAxisIndex] = {
        name: name || yAxisNames[yAxisIndex] || '',
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
          formatter: (val) => getFormatter(curUnitType, unit).format(val, decimal),
        },
      };
    });

    const formatTime = (timeStr) => moment(Number(timeStr)).format(moreThanOneDay ? 'D/M HH:mm' : 'HH:mm');

    const getTTUnitType = (i) => {
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

    if (time) {
      defaultTTFormatter = (param) => {
        const endTime = time[param[0].dataIndex + 1];
        if (!endTime) {
          return `${formatTime(param[0].name)}<br />${genTTArray(param).join('')}`;
        }
        return `${formatTime(param[0].name)} - ${formatTime(endTime)}<br/>${genTTArray(param).join('')}`;
      };
    }
    const lgFormatter = (name) => {
      const defaultName = legendFormatter ? legendFormatter(name) : name;
      return cutStr(defaultName, 20);
    };

    const haveTwoYAxis = yAxis.length > 1;
    if (haveTwoYAxis) {
      yAxis = yAxis.map((item, i) => {
        // 有数据和无数据的显示有差异
        const hasData = some(results[i].data || [], (_data) => Number(_data) !== 0);
        let { name } = item;
        if (!hasData) {
          name =
            i === 0
              ? `${'  '.repeat(item.name.length + 1)}${item.name}`
              : `${item.name}${'  '.repeat(item.name.length)}`;
        }

        if (i > 1) {
          // 右侧有超过两个Y轴
          yAxis[i].offset = 80 * (i - 1);
        }
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
        data: legendData,
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
          data: xAxis || time || [] /* X轴数据 */,
          axisTick: {
            show: false /* 坐标刻度 */,
          },
          axisLine: {
            show: false,
          },
          axisLabel: {
            formatter: xAxis
              ? (value) => value
              : (value) => moment(Number(value)).format(moreThanOneDay ? 'D/M HH:mm' : 'HH:mm'),
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
    return merge(defaultOption, opt);
  };

  const { xAxis, time, results } = data;
  let hasData = size(results) > 0 && !isEmpty(xAxis || time);
  if (time === undefined && xAxis === undefined) {
    hasData = size(results) > 0;
  }

  const handleChange = (value) => {
    setSelectedGroup(value);
  };

  return (
    <>
      <IF check={hasData}>
        <div className="chart-selecter">
          {i18n.t('msp:select instance')}：
          <Select className="my-3" value={selectedGroup || groups[0]} style={{ width: 200 }} onChange={handleChange}>
            {map(groups, (item) => (
              <Option value={item} key={item}>
                {item}
              </Option>
            ))}
          </Select>
        </div>
      </IF>
      <ChartRender {...props} hasData={hasData} getOption={getOption} />
    </>
  );
};

export default ConnectChart;
