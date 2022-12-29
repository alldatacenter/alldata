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
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getColumnRenderName,
  getStyles,
  getValue,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import Chart from '../../../models/Chart';
import { ChartSelectionManager } from '../../../models/ChartSelectionManager';
import Config from './config';
import {
  DataConfig,
  GaugeAxisStyle,
  GaugeDetailStyle,
  GaugePointerStyle,
  GaugeProgressStyle,
  GaugeSplitLineStyle,
  GaugeStyle,
  GaugeTitleStyle,
  SeriesConfig,
} from './types';

class BasicGaugeChart extends Chart {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  protected isArea = false;
  protected isStack = false;

  constructor(props?) {
    super(
      props?.id || 'gauge',
      props?.name || 'viz.palette.graph.names.gaugeChart',
      props?.icon || 'gauge',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: 0,
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
    this.selectionManager.attachZRenderListeners(this.chart);
    this.selectionManager.attachEChartsListeners(this.chart);
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
    if (!options.dataset || !options.dataset.columns || !options.config) {
      return;
    }
    if (!this.isMatchRequirement(options.config)) {
      this.chart?.clear();
      return;
    }
    const newOptions = this.getOptions(options.dataset, options.config);
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  onUnMount(options: BrokerOption, context: BrokerContext): void {
    this.selectionManager?.removeZRenderListeners(this.chart);
    this.chart?.dispose();
  }

  onResize(options: BrokerOption, context: BrokerContext): void {
    this.chart?.resize(context);
  }

  getOptions(dataset: ChartDataSetDTO, config: ChartConfig) {
    const styleConfigs = config.styles || [];
    const dataConfigs = config.datas || [];
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
      aggregateConfigs[0],
    );
    const tooltip = this.getTooltip(styleConfigs, aggregateConfigs);
    return {
      tooltip,
      series,
    };
  }

  private getTooltip(
    style: ChartStyleConfig[],
    aggConfigs: ChartDataSectionField[],
  ): { formatter: ({ data }: { data: any }) => string } {
    const [prefix, suffix] = getStyles(style, ['gauge'], ['prefix', 'suffix']);
    return {
      formatter: ({ data }) => {
        return `${data.name} : ${prefix}${toFormattedValue(
          data.value,
          aggConfigs[0].format,
        )}${suffix}`;
      },
    };
  }

  private getSeries(
    styleConfigs: ChartStyleConfig[],
    chartDataSet: IChartDataSet<string>,
    aggConfig: ChartDataSectionField,
  ): SeriesConfig {
    const detail = this.getDetail(styleConfigs, aggConfig);
    const title = this.getTitle(styleConfigs);
    const pointer = this.getPointer(styleConfigs);
    const axis = this.getAxis(styleConfigs);
    const splitLine = this.getSplitLine(styleConfigs);
    const progress = this.getProgress(styleConfigs);
    const pointerColor = getStyles(styleConfigs, ['pointer'], ['pointerColor']);
    const dataConfig: DataConfig = {
      name: getColumnRenderName(aggConfig),
      value: chartDataSet?.[0]?.getCell(aggConfig) || 0,
      itemStyle: {
        color: pointerColor,
      },
    };
    if (aggConfig?.color?.start) {
      dataConfig.itemStyle.color = aggConfig.color.start;
    }

    return {
      ...this.getGauge(styleConfigs),
      data: [dataConfig],
      pointer,
      ...axis,
      title,
      splitLine,
      detail,
      progress,
    };
  }

  private getProgress(styleConfigs: ChartStyleConfig[]): GaugeProgressStyle {
    const [show, roundCap] = getStyles(
      styleConfigs,
      ['progress'],
      ['showProgress', 'roundCap'],
    );
    const width = getValue(styleConfigs, ['axis', 'axisLineSize']);
    return {
      show,
      roundCap,
      width,
    };
  }

  private getSplitLine(styleConfigs: ChartStyleConfig[]): GaugeSplitLineStyle {
    const [show, lineStyle, distance, length] = getStyles(
      styleConfigs,
      ['splitLine'],
      ['showSplitLine', 'lineStyle', 'distance', 'splitLineLength'],
    );
    return {
      show,
      length,
      distance,
      lineStyle,
    };
  }

  private getGauge(styleConfigs: ChartStyleConfig[]): GaugeStyle {
    const [max, radius, startAngle, endAngle, splitNumber] = getStyles(
      styleConfigs,
      ['gauge'],
      ['max', 'radius', 'startAngle', 'endAngle', 'splitNumber'],
    );
    return {
      type: 'gauge',
      max,
      splitNumber,
      radius,
      startAngle,
      endAngle,
    };
  }

  private getAxis(styleConfigs: ChartStyleConfig[]): GaugeAxisStyle {
    const [axisWidth, axisLineColor, axisRoundCap] = getStyles(
      styleConfigs,
      ['axis'],
      ['axisLineSize', 'axisLineColor', 'axisRoundCap'],
    );
    const [showAxisTick, lineStyle, distance, splitNumber] = getStyles(
      styleConfigs,
      ['axisTick'],
      ['showAxisTick', 'lineStyle', 'distance', 'splitNumber'],
    );
    const [showAxisLabel, font, axisLabelDistance] = getStyles(
      styleConfigs,
      ['axisLabel'],
      ['showAxisLabel', 'font', 'distance'],
    );
    return {
      axisLine: {
        lineStyle: {
          width: axisWidth,
          color: [[1, axisLineColor]],
        },
        roundCap: axisRoundCap,
      },
      axisTick: {
        show: showAxisTick,
        splitNumber,
        distance,
        lineStyle,
      },
      axisLabel: {
        show: showAxisLabel,
        distance: axisLabelDistance,
        ...font,
      },
    };
  }

  private getPointer(styleConfigs: ChartStyleConfig[]): GaugePointerStyle {
    const [
      show,
      pointerLength,
      pointerWidth,
      customPointerColor,
      pointerColor,
      { type, color, width },
    ] = getStyles(
      styleConfigs,
      ['pointer'],
      [
        'showPointer',
        'pointerLength',
        'pointerWidth',
        'customPointerColor',
        'pointerColor',
        'lineStyle',
      ],
    );

    return {
      show,
      length: pointerLength ? pointerLength : 0,
      width: pointerWidth ? `${pointerWidth}px` : 0,
      itemStyle: {
        color: customPointerColor ? pointerColor : 'auto',
        borderType: type,
        borderWidth: width,
        borderColor: color,
      },
    };
  }

  private getDetail(
    styleConfigs: ChartStyleConfig[],
    aggConfig: ChartDataSectionField,
  ): GaugeDetailStyle {
    const [show, font, detailOffsetLeft, detailOffsetTop] = getStyles(
      styleConfigs,
      ['data'],
      ['showData', 'font', 'detailOffsetLeft', 'detailOffsetTop'],
    );
    const [suffix, prefix] = getStyles(
      styleConfigs,
      ['gauge'],
      ['suffix', 'prefix'],
    );

    return {
      show,
      ...font,
      offsetCenter: [
        detailOffsetLeft ? detailOffsetLeft : 0,
        detailOffsetTop ? detailOffsetTop : 0,
      ],
      formatter: value =>
        `${prefix}${toFormattedValue(value || 0, aggConfig.format)}${suffix}`,
    };
  }

  private getTitle(styleConfigs: ChartStyleConfig[]): GaugeTitleStyle {
    const [show, font, detailOffsetLeft, detailOffsetTop] = getStyles(
      styleConfigs,
      ['label'],
      ['showLabel', 'font', 'detailOffsetLeft', 'detailOffsetTop'],
    );
    return {
      show,
      ...font,
      offsetCenter: [
        detailOffsetLeft ? detailOffsetLeft : 0,
        detailOffsetTop ? detailOffsetTop : 0,
      ],
    };
  }
}

export default BasicGaugeChart;
