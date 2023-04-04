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
import {
  ChartConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  LabelStyle,
  LegendStyle,
  LineStyle,
  SelectedItem,
  SeriesStyle,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getAxisLabel,
  getAxisLine,
  getAxisTick,
  getColumnRenderName,
  getExtraSeriesDataFormat,
  getExtraSeriesRowData,
  getGridStyle,
  getReference2,
  getSelectedItemStyles,
  getSeriesTooltips4Polar2,
  getSplitLine,
  getStyles,
  hadAxisLabelOverflowConfig,
  setOptionsByAxisLabelOverflow,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import Chart from '../../../models/Chart';
import { ChartSelectionManager } from '../../../models/ChartSelectionManager';
import Config from './config';
import { DoubleYChartXAxis, DoubleYChartYAxis, Series } from './types';
import { getYAxisIntervalConfig } from './utils';
class BasicDoubleYChart extends Chart {
  dependency = [];
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  constructor() {
    super('double-y', 'chartName', 'fsux_tubiao_shuangzhoutu');
    this.meta.requirements = [
      {
        group: 1,
        aggregate: [2, 999],
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
    this.selectionManager.attachEChartsListeners(this.chart);
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
    if (!options.dataset || !options.dataset.columns || !options.config) {
      return;
    }
    if (!this.isMatchRequirement(options.config)) {
      return this.chart?.clear();
    }
    this.selectionManager?.updateSelectedItems(options?.selectedItems);
    const newOptions = this.getOptions(
      options.dataset,
      options.config,
      options.selectedItems,
    );
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  onUnMount(options: BrokerOption, context: BrokerContext) {
    this.selectionManager?.removeWindowListeners(context.window);
    this.selectionManager?.removeZRenderListeners(this.chart);
    this.chart?.dispose();
  }

  onResize(options: BrokerOption, context: BrokerContext) {
    this.chart?.resize(context);
    hadAxisLabelOverflowConfig(this.chart?.getOption()) &&
      this.onUpdated(options, context);
  }

  private getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    selectedItems?: SelectedItem[],
  ) {
    const dataConfigs = config.datas || [];
    const styleConfigs = config.styles || [];
    const settingConfigs = config.settings || [];

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );
    const groupConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Group)
      .flatMap(config => config.rows || []);
    const infoConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);

    const leftMetricsConfigs = dataConfigs
      .filter(
        c => c.type === ChartDataSectionType.Aggregate && c.key === 'metricsL',
      )
      .flatMap(config => config.rows || []);
    const rightMetricsConfigs = dataConfigs
      .filter(
        c => c.type === ChartDataSectionType.Aggregate && c.key === 'metricsR',
      )
      .flatMap(config => config.rows || []);

    if (!leftMetricsConfigs.concat(rightMetricsConfigs)?.length) {
      return {};
    }

    const yAxisNames: string[] = leftMetricsConfigs
      .concat(rightMetricsConfigs)
      .map(getColumnRenderName);

    // @TM 溢出自动根据bar长度设置标尺
    const option = setOptionsByAxisLabelOverflow({
      chart: this.chart,
      xAxis: this.getXAxis(styleConfigs, groupConfigs, chartDataSet),
      yAxis: this.getYAxis(
        styleConfigs,
        leftMetricsConfigs,
        rightMetricsConfigs,
        chartDataSet,
      ),
      grid: getGridStyle(styleConfigs),
      series: this.getSeries(
        styleConfigs,
        settingConfigs,
        leftMetricsConfigs,
        rightMetricsConfigs,
        chartDataSet,
        selectedItems,
      ),
      yAxisNames,
    });

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
        },
        formatter: this.getTooltipFormmaterFunc(
          styleConfigs,
          groupConfigs,
          leftMetricsConfigs.concat(rightMetricsConfigs),
          [],
          infoConfigs,
          chartDataSet,
        ),
      },
      legend: this.getLegend(styleConfigs, yAxisNames),
      ...option,
    };
  }

  private getSeries(
    styles: ChartStyleConfig[],
    settingConfigs: ChartStyleConfig[],
    leftDeminsionConfigs,
    rightDeminsionConfigs,
    chartDataSet: IChartDataSet<string>,
    selectedItems?: SelectedItem[],
  ): Series[] {
    const _getSeriesByDemisionPostion =
      () =>
      (
        config: ChartDataSectionField,
        styles: ChartStyleConfig[],
        settings: ChartStyleConfig[],
        data: IChartDataSet<string>,
        direction: string,
        yAxisIndex: number,
        cIndex: number,
      ): Series => {
        const [graphType, graphStyle] = getStyles(
          styles,
          [direction],
          ['graphType', 'graphStyle'],
        );
        return {
          yAxisIndex,
          name: getColumnRenderName(config),
          type: graphType || 'line',
          sampling: 'average',
          data: chartDataSet.map((dc, dIndex) => ({
            ...config,
            ...getExtraSeriesRowData(dc),
            ...getExtraSeriesDataFormat(config?.format),
            ...getSelectedItemStyles(cIndex, dIndex, selectedItems || []),
            value: dc.getCell(config),
          })),
          ...this.getItemStyle(config),
          ...this.getGraphStyle(graphType, graphStyle),
          ...this.getLabelStyle(styles, direction),
          ...this.getSeriesStyle(styles),
          ...getReference2(settings, data, config, false),
        };
      };

    const series = []
      .concat(
        leftDeminsionConfigs.map((lc, cIndex) =>
          _getSeriesByDemisionPostion()(
            lc,
            styles,
            settingConfigs,
            chartDataSet,
            'leftY',
            0,
            cIndex,
          ),
        ),
      )
      .concat(
        rightDeminsionConfigs.map((rc, cIndex) =>
          _getSeriesByDemisionPostion()(
            rc,
            styles,
            settingConfigs,
            chartDataSet,
            'rightY',
            1,
            cIndex + leftDeminsionConfigs.length,
          ),
        ),
      );
    return series;
  }

  private getItemStyle(config): { itemStyle: { color: string | undefined } } {
    const color = config?.color?.start;
    return {
      itemStyle: {
        color,
      },
    };
  }

  private getGraphStyle(
    graphType,
    style,
  ): { lineStyle?: LineStyle; barWidth?: string; color?: string } {
    if (graphType === 'line') {
      return { lineStyle: style };
    } else {
      return {
        barWidth: style?.width,
        color: style?.color,
      };
    }
  }

  private getXAxis(
    styles: ChartStyleConfig[],
    xAxisConfigs: ChartDataSectionField[],
    chartDataSet: IChartDataSet<string>,
  ): DoubleYChartXAxis {
    const fisrtXAxisConfig = xAxisConfigs[0];
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
      type: 'category',
      tooltip: { show: true },
      inverse,
      axisLabel: getAxisLabel(
        showLabel,
        font,
        showInterval ? interval : null,
        rotate,
        overflow,
      ),
      axisLine: getAxisLine(showAxis, lineStyle),
      axisTick: getAxisTick(showLabel, lineStyle),
      splitLine: getSplitLine(showVerticalLine, verticalLineStyle),
      data: chartDataSet.map(d => d.getCell(fisrtXAxisConfig)),
    };
  }

  private getYAxis(
    styles: ChartStyleConfig[],
    leftDeminsionConfigs: ChartDataSectionField[],
    rightDeminsionConfigs: ChartDataSectionField[],
    chartDataSet: IChartDataSet<string>,
  ): DoubleYChartYAxis[] {
    const [showHorizonLine, horizonLineStyle] = getStyles(
      styles,
      ['splitLine'],
      ['showHorizonLine', 'horizonLineStyle'],
    );

    const yAxisIntervalConfig = getYAxisIntervalConfig(
      leftDeminsionConfigs,
      rightDeminsionConfigs,
      chartDataSet,
    );

    const _yAxisTemplate = (position, name): DoubleYChartYAxis => {
      const [showAxis, inverse, font, showLabel] = getStyles(
        styles,
        [`${position}Y`],
        ['showAxis', 'inverseAxis', 'font', 'showLabel'],
      );
      const [format] = getStyles(
        styles,
        [`${position}Y`, 'modal'],
        ['YAxisNumberFormat'],
      );
      return {
        type: 'value',
        position,
        showTitleAndUnit: true,
        name,
        nameLocation: 'middle',
        nameGap: 50,
        nameRotate: 90,
        nameTextStyle: {
          color: '#666',
          fontFamily: 'PingFang SC',
          fontSize: 12,
        },
        min: yAxisIntervalConfig[position + 'Min'],
        max: yAxisIntervalConfig[position + 'Max'],
        interval: yAxisIntervalConfig[position + 'Interval'],
        inverse,
        axisLine: getAxisLine(showAxis),
        axisLabel: {
          show: showLabel,
          formatter: v => toFormattedValue(v, format),
          ...font,
        },
        splitLine: getSplitLine(showHorizonLine, horizonLineStyle),
      };
    };

    const leftYAxisNames = leftDeminsionConfigs
      .map(getColumnRenderName)
      .join('/');
    const rightYAxisNames = rightDeminsionConfigs
      .map(getColumnRenderName)
      .join('/');

    return [
      _yAxisTemplate('left', leftYAxisNames),
      _yAxisTemplate('right', rightYAxisNames),
    ];
  }

  private getLegend(styles, seriesNames): LegendStyle {
    const [show, type, font, legendPos, selectAll] = getStyles(
      styles,
      ['legend'],
      ['showLegend', 'type', 'font', 'position', 'selectAll'],
    );
    let positions = {};
    let orient = '';

    switch (legendPos) {
      case 'top':
        orient = 'horizontal';
        positions = { top: 8, left: 8, right: 8, height: 32 };
        break;
      case 'bottom':
        orient = 'horizontal';
        positions = { bottom: 8, left: 8, right: 8, height: 32 };
        break;
      case 'left':
        orient = 'vertical';
        positions = { left: 8, top: 16, bottom: 24, width: 96 };
        break;
      default:
        orient = 'vertical';
        positions = { right: 8, top: 16, bottom: 24, width: 96 };
        break;
    }
    const selected: { [x: string]: boolean } = seriesNames.reduce(
      (obj, name) => ({
        ...obj,
        [name]: selectAll,
      }),
      {},
    );

    return {
      ...positions,
      show,
      type,
      orient,
      selected,
      data: seriesNames,
      textStyle: font,
    };
  }

  private getLabelStyle(
    styles: ChartStyleConfig[],
    direction: string,
  ): LabelStyle {
    const [showLabel, position, LabelFont] = getStyles(
      styles,
      [direction + 'Label'],
      ['showLabel', 'position', 'font'],
    );

    return {
      label: {
        show: showLabel,
        position,
        ...LabelFont,
        formatter: params => {
          const { value, data } = params;
          const formattedValue = toFormattedValue(value, data.format);
          const labels: string[] = [];
          labels.push(formattedValue);
          return labels.join('\n');
        },
      },
      labelLayout: { hideOverlap: true },
    };
  }

  private getTooltipFormmaterFunc(
    styleConfigs: ChartStyleConfig[],
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    colorConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
    chartDataSet: IChartDataSet<string>,
  ): (params) => string {
    return seriesParams => {
      return getSeriesTooltips4Polar2(
        chartDataSet,
        seriesParams[0],
        groupConfigs,
        colorConfigs,
        aggregateConfigs,
        infoConfigs,
      );
    };
  }

  private getSeriesStyle(styles: ChartStyleConfig[]): SeriesStyle {
    const [smooth, stack, step, symbol] = getStyles(
      styles,
      ['graph'],
      ['smooth', 'stack', 'step', 'symbol'],
    );
    return { smooth, step, symbol: symbol ? 'emptyCircle' : 'none', stack };
  }
}

export default BasicDoubleYChart;
