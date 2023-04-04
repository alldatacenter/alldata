/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChartDataSectionType } from 'app/constants';
import Chart from 'app/models/Chart';
import { ChartSelectionManager } from 'app/models/ChartSelectionManager';
import {
  ChartConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  FormatFieldAction,
  LabelStyle,
  SelectedItem,
  XAxis,
  XAxisColumns,
  YAxis,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getColumnRenderName,
  getExtraSeriesRowData,
  getGridStyle,
  getSelectedItemStyles,
  getStyles,
  hadAxisLabelOverflowConfig,
  setOptionsByAxisLabelOverflow,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { precisionCalculation } from 'app/utils/number';
import currency from 'currency.js';
import { init } from 'echarts';
import { CalculationType } from 'globalConstants';
import { UniqArray } from 'utils/object';
import Config from './config';
import {
  OrderConfig,
  WaterfallBorderStyle,
  WaterfallDataListConfig,
} from './types';

class WaterfallChart extends Chart {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  protected rowDataList: {
    rowData: { [x: string]: any };
  }[] = [];

  constructor(props?) {
    super(
      props?.id || 'waterfall-chart',
      props?.name || 'viz.palette.graph.names.waterfallChart',
      props?.icon || 'waterfall',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: 1,
        aggregate: 1,
      },
    ];
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (options.containerId === undefined || !context.document) {
      return;
    }

    this.chart = init(
      context.document.getElementById(options.containerId)!,
      'default',
    );

    this.selectionManager = new ChartSelectionManager(this.mouseEvents);
    this.selectionManager.attachWindowListeners(context.window);
    this.selectionManager.attachZRenderListeners(this.chart);
    this.chart.on('click', ({ dataIndex, componentIndex, ...rest }) => {
      // NOTE: 1. 累计不响应事件； 2. 下部透明柱状图不响应事件
      if (this.rowDataList.length <= dataIndex || componentIndex === 0) return;
      this.selectionManager?.echartsClickEventHandler({
        ...rest,
        dataIndex: dataIndex,
        componentIndex: '',
        data: { ...this.rowDataList[dataIndex] },
      });
    });
  }

  onUpdated(options: BrokerOption, context: BrokerContext): void {
    if (!options.dataset || !options.dataset.columns || !options.config) {
      return;
    }
    if (!this.isMatchRequirement(options.config)) {
      this.chart?.clear();
      return;
    }
    this.selectionManager?.updateSelectedItems(options?.selectedItems);
    const newOptions = this.getOptions(
      options.dataset,
      options.config,
      context,
      options.selectedItems,
    );
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  onUnMount(options: BrokerOption, context: BrokerContext): void {
    this.selectionManager?.removeWindowListeners(context.window);
    this.selectionManager?.removeZRenderListeners(this.chart);
    this.rowDataList = [];
    this.chart?.dispose();
  }

  onResize(options: BrokerOption, context: BrokerContext): void {
    this.chart?.resize({ width: context?.width, height: context?.height });
    hadAxisLabelOverflowConfig(this.chart?.getOption()) &&
      this.onUpdated(options, context);
  }

  private getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    context,
    selectedItems?: SelectedItem[],
  ) {
    const styleConfigs = config.styles || [];
    const dataConfigs = config.datas || [];
    const groupConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Group)
      .flatMap(config => config.rows || []);
    const aggregateConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Aggregate)
      .flatMap(config => config.rows || []);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );

    const series = this.getSeries(
      styleConfigs,
      chartDataSet,
      aggregateConfigs,
      groupConfigs,
      context?.translator,
      selectedItems,
    );

    return {
      barWidth: this.getSeriesBarWidth(styleConfigs),
      ...series,
    };
  }

  private getSeriesBarWidth(styles: ChartStyleConfig[]): number {
    const [width] = getStyles(styles, ['bar'], ['width']);
    return width;
  }

  private getSeries(
    styles: ChartStyleConfig[],
    chartDataSet: IChartDataSet<string>,
    aggregateConfigs: ChartDataSectionField[],
    group: ChartDataSectionField[],
    t?: (key: string, disablePrefix?: boolean, options?: any) => any,
    selectedItems?: SelectedItem[],
  ) {
    const xAxisColumns: XAxisColumns = {
      type: 'category',
      tooltip: { show: true },
      data: UniqArray(chartDataSet.map(dc => dc.getCell(group[0]))),
    };
    const yAxisNames: string[] = aggregateConfigs.map(getColumnRenderName);
    const [isIncrement, ascendColor, descendColor] = getStyles(
      styles,
      ['bar'],
      ['isIncrement', 'ascendColor', 'descendColor'],
    );
    const label = this.getLabel(styles, aggregateConfigs[0].format);
    this.rowDataList = [];
    const dataList = chartDataSet.map(dc => {
      this.rowDataList.push(getExtraSeriesRowData(dc));
      return dc.getCell(aggregateConfigs[0]);
    });

    const { baseData, ascendOrder, descendOrder } = this.getDataList(
      isIncrement,
      dataList,
      xAxisColumns,
      styles,
      t,
      selectedItems,
    );

    const baseDataObj = {
      name: yAxisNames[0],
      type: 'bar',
      sampling: 'average',
      stack: 'stack',
      data: baseData,
      itemStyle: {
        normal: {
          barBorderColor: 'rgba(0,0,0,0)',
          color: 'rgba(0,0,0,0)',
        },
        emphasis: {
          barBorderColor: 'rgba(0,0,0,0)',
          color: 'rgba(0,0,0,0)',
        },
      },
    };

    const ascendOrderObj = {
      name: t?.('common.increase'),
      type: 'bar',
      sampling: 'average',
      stack: 'stack',
      itemStyle: {
        color: ascendColor,
        ...this.getSeriesItemStyle(styles),
      },
      data: ascendOrder,
      label,
      labelLayout: { hideOverlap: true },
    };

    const descendOrderObj = {
      name: t?.('common.decrease'),
      type: 'bar',
      sampling: 'average',
      stack: 'stack',
      data: descendOrder,
      itemStyle: {
        color: descendColor,
        ...this.getSeriesItemStyle(styles),
      },
      label,
      labelLayout: { hideOverlap: true },
    };
    const axisInfo = {
      xAxis: this.getXAxis(styles, xAxisColumns),
      yAxis: this.getYAxis(styles, yAxisNames),
    };

    // @TM 溢出自动根据bar长度设置标尺
    const option = setOptionsByAxisLabelOverflow({
      chart: this.chart,
      xAxis: axisInfo.xAxis,
      yAxis: axisInfo.yAxis,
      grid: getGridStyle(styles),
      series: [baseDataObj, ascendOrderObj, descendOrderObj],
      yAxisNames,
    });
    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
        formatter: param => {
          const text = param.map((pa, index) => {
            let data = pa.value;
            if (!index && typeof param[1].value === 'number') {
              data = precisionCalculation(CalculationType.ADD, [
                data,
                param[1].value,
              ]);
            }
            return `${pa.seriesName}: ${toFormattedValue(
              data,
              aggregateConfigs[0].format,
            )}`;
          });
          const xAxis = param[0]['axisValue'];
          if (xAxis === t?.('common.total')) {
            return '';
          } else {
            text.unshift(xAxis as string);
            return text.join('<br/>');
          }
        },
      },
      ...option,
    };
  }

  private getSeriesItemStyle(styles: ChartStyleConfig[]): WaterfallBorderStyle {
    const [borderStyle, borderRadius] = getStyles(
      styles,
      ['bar'],
      ['borderStyle', 'radius'],
    );
    return {
      borderRadius,
      borderType: borderStyle?.type,
      borderWidth: borderStyle?.width,
      borderColor: borderStyle?.color,
    };
  }

  private getDataList(
    isIncrement: boolean,
    dataList: string[],
    xAxisColumns: XAxisColumns,
    styles: ChartStyleConfig[],
    t?: (key: string, disablePrefix?: boolean, options?: any) => any,
    selectedItems?: SelectedItem[],
  ): WaterfallDataListConfig {
    const [totalColor] = getStyles(styles, ['bar'], ['totalColor']);
    const baseData: Array<number> = [];
    const ascendOrder: OrderConfig[] = [];
    const descendOrder: OrderConfig[] = [];
    dataList.forEach((data, index) => {
      const newData: number = isNaN(currency(data).value)
        ? 0
        : currency(data).value;
      const lastData: number = isNaN(currency(dataList[index - 1]).value)
        ? 0
        : currency(dataList[index - 1]).value;
      if (index > 0) {
        if (isIncrement) {
          const result: number =
            lastData >= 0
              ? precisionCalculation(CalculationType.ADD, [
                  lastData,
                  baseData[index - 1],
                ])
              : baseData[index - 1];
          if (newData >= 0) {
            baseData.push(result);
            ascendOrder.push({
              value: newData,
              ...getSelectedItemStyles('', index, selectedItems || []),
            });
            descendOrder.push('-');
          } else {
            baseData.push(
              precisionCalculation(CalculationType.ADD, [result, newData]),
            );
            ascendOrder.push('-');
            descendOrder.push({
              value: Math.abs(newData),
              ...getSelectedItemStyles('', index, selectedItems || []),
            });
          }
        } else {
          const result: number = precisionCalculation(
            CalculationType.SUBTRACT,
            [newData, lastData],
          );
          if (result >= 0) {
            ascendOrder.push({
              value: result,
              ...getSelectedItemStyles('', index, selectedItems || []),
            });
            descendOrder.push('-');
            baseData.push(lastData);
          } else {
            ascendOrder.push('-');
            descendOrder.push({
              value: Math.abs(result),
              ...getSelectedItemStyles('', index, selectedItems || []),
            });
            baseData.push(
              precisionCalculation(CalculationType.SUBTRACT, [
                lastData,
                Math.abs(result),
              ]),
            );
          }
        }
      } else {
        if (newData >= 0) {
          ascendOrder.push({
            value: newData,
            ...getSelectedItemStyles('', index, selectedItems || []),
          });
          descendOrder.push('-');
          baseData.push(0);
        } else {
          ascendOrder.push('-');
          descendOrder.push({
            value: Math.abs(newData),
            ...getSelectedItemStyles('', index, selectedItems || []),
          });
          baseData.push(0);
        }
      }
    });
    if (isIncrement && xAxisColumns?.data?.length) {
      xAxisColumns.data.push(t?.('common.total'));
      const resultData = precisionCalculation(CalculationType.ADD, [
        dataList[dataList.length - 1],
        baseData[baseData.length - 1],
      ]);
      if (resultData > 0) {
        ascendOrder.push({
          value: resultData,
          itemStyle: {
            color: totalColor,
          },
        });
        descendOrder.push('-');
      } else {
        descendOrder.push({
          value: Math.abs(resultData),
          itemStyle: {
            color: totalColor,
          },
        });
        ascendOrder.push('-');
      }
    }
    return {
      baseData,
      ascendOrder,
      descendOrder,
    };
  }

  private getLabel(
    styles: ChartStyleConfig[],
    format: FormatFieldAction | undefined,
  ): LabelStyle {
    const [show, position, font] = getStyles(
      styles,
      ['label'],
      ['showLabel', 'position', 'font'],
    );
    return {
      show,
      position,
      ...font,
      formatter: ({ value }) => `${toFormattedValue(value, format)}`,
    };
  }

  private getXAxis(
    styles: ChartStyleConfig[],
    xAxisColumns: XAxisColumns,
  ): XAxis {
    const [
      showAxis,
      inverse,
      lineStyle,
      showLabel,
      font,
      rotate,
      showInterval,
      interval,
      overflow,
    ] = getStyles(
      styles,
      ['xAxis'],
      [
        'showAxis',
        'inverseAxis',
        'lineStyle',
        'showLabel',
        'font',
        'rotate',
        'showInterval',
        'interval',
        'overflow',
      ],
    );
    const [showVerticalLine, verticalLineStyle] = getStyles(
      styles,
      ['splitLine'],
      ['showVerticalLine', 'verticalLineStyle'],
    );

    return {
      ...xAxisColumns,
      inverse,
      axisLabel: {
        show: showLabel,
        rotate,
        interval: showInterval ? interval : 'auto',
        overflow,
        ...font,
      },
      axisLine: {
        show: showAxis,
        lineStyle,
      },
      axisTick: {
        show: showLabel,
        lineStyle,
      },
      splitLine: {
        show: showVerticalLine,
        lineStyle: verticalLineStyle,
      },
    };
  }

  private getYAxis(styles: ChartStyleConfig[], yAxisNames: string[]): YAxis {
    const [
      showAxis,
      inverse,
      lineStyle,
      showLabel,
      font,
      showTitleAndUnit,
      unitFont,
      nameLocation,
      nameGap,
      nameRotate,
      min,
      max,
    ] = getStyles(
      styles,
      ['yAxis'],
      [
        'showAxis',
        'inverseAxis',
        'lineStyle',
        'showLabel',
        'font',
        'showTitleAndUnit',
        'unitFont',
        'nameLocation',
        'nameGap',
        'nameRotate',
        'min',
        'max',
      ],
    );
    const [format] = getStyles(
      styles,
      ['yAxis', 'modal'],
      ['YAxisNumberFormat'],
    );
    const name = showTitleAndUnit ? yAxisNames.join(' / ') : null;
    const [showHorizonLine, horizonLineStyle] = getStyles(
      styles,
      ['splitLine'],
      ['showHorizonLine', 'horizonLineStyle'],
    );

    return {
      type: 'value',
      name,
      nameLocation,
      nameGap,
      nameRotate,
      inverse,
      min,
      max,
      axisLabel: {
        show: showLabel,
        formatter: v => toFormattedValue(v, format),
        ...font,
      },
      axisLine: {
        show: showAxis,
        lineStyle,
      },
      axisTick: {
        show: showLabel,
        lineStyle,
      },
      nameTextStyle: unitFont,
      splitLine: {
        show: showHorizonLine,
        lineStyle: horizonLineStyle,
      },
    };
  }
}

export default WaterfallChart;
