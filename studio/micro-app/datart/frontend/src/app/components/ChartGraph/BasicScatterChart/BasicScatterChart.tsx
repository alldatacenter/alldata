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
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import { ChartSelectionManager } from 'app/models/ChartSelectionManager';
import {
  ChartConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  LabelStyle,
  LegendStyle,
  SelectedItem,
  YAxis,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getColumnRenderName,
  getDataColumnMaxAndMin2,
  getDrillableRows,
  getExtraSeriesRowData,
  getGridStyle,
  getReference2,
  getScatterSymbolSizeFn,
  getSelectedItemStyles,
  getSeriesTooltips4Polar2,
  getStyles,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import Config from './config';
import { ScatterMetricAndSizeSerie } from './types';

class BasicScatterChart extends Chart {
  dependency = [];
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  constructor() {
    super('scatter', 'viz.palette.graph.names.scatterChart', 'sandiantu');
    this.meta.requirements = [
      {
        group: [0, 999],
        aggregate: 2,
      },
    ];
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (
      options.containerId === undefined ||
      !context.document ||
      !context.window
    ) {
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
      options.drillOption,
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
    this.chart?.resize(options, context);
  }

  private getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    drillOption?: ChartDrillOption,
    selectedItems?: SelectedItem[],
  ) {
    const styleConfigs = config.styles || [];
    const dataConfigs = config.datas || [];
    const settingConfigs = config.settings || [];
    const groupConfigs: ChartDataSectionField[] = getDrillableRows(
      dataConfigs,
      drillOption,
    );
    const aggregateConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Aggregate)
      .flatMap(config => config.rows || []);
    const sizeConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Size)
      .flatMap(config => config.rows || []);
    const infoConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);
    const colorConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Color)
      .flatMap(config => config.rows || []);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );

    const axisColumns = aggregateConfigs.map(config => {
      return {
        type: 'value',
        name: getColumnRenderName(config),
      };
    });

    const series = this.getSeriesGroupByColorConfig(
      chartDataSet,
      groupConfigs,
      aggregateConfigs,
      sizeConfigs,
      colorConfigs,
      infoConfigs,
      styleConfigs,
      settingConfigs,
      selectedItems,
    );

    return {
      tooltip: {
        formatter: this.getTooltipFormmaterFunc(
          groupConfigs,
          aggregateConfigs,
          colorConfigs,
          sizeConfigs,
          infoConfigs,
          chartDataSet,
        ),
      },
      legend: this.getLegendStyle(
        styleConfigs,
        series?.map(s => s.name),
      ),
      grid: getGridStyle(styleConfigs),
      xAxis: this.getAxis(styleConfigs, axisColumns[0], 'xAxis'),
      yAxis: this.getAxis(styleConfigs, axisColumns[1], 'yAxis'),
      series,
    };
  }

  protected getSeriesGroupByColorConfig(
    chartDataSetRows: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    colorConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
    styleConfigs: ChartStyleConfig[],
    settingConfigs: ChartStyleConfig[],
    selectedItems?: SelectedItem[],
  ): ScatterMetricAndSizeSerie[] {
    const { min, max } = getDataColumnMaxAndMin2(
      chartDataSetRows,
      sizeConfigs[0],
    );
    if (!colorConfigs?.length) {
      return [
        this.getMetricAndSizeSerie(
          {
            max,
            min,
          },
          chartDataSetRows,
          groupConfigs,
          aggregateConfigs,
          sizeConfigs,
          infoConfigs,
          styleConfigs,
          settingConfigs,
          selectedItems,
        ),
      ];
    }

    const colors: Array<{ key; value }> =
      colorConfigs?.[0]?.color?.colors || [];

    // TODO(Stephen): should be refactor by ChartDataSet groupBy function
    const groupedObjDataColumns: {
      [key: string]: { color: string; datas: IChartDataSet<string> };
    } = chartDataSetRows?.reduce((acc, cur) => {
      const key = cur.getCell(colorConfigs?.[0]);
      if (acc?.[key]) {
        acc[key].datas.push(cur);
      } else {
        acc[key] = {
          color: colors?.find(c => c.key === key)?.value,
          datas: [cur],
        };
      }
      return acc;
    }, {});

    return Object.keys(groupedObjDataColumns).map((k, gcIndex) => {
      return this.getMetricAndSizeSerie(
        {
          max,
          min,
        },
        groupedObjDataColumns?.[k]?.datas,
        groupConfigs,
        aggregateConfigs,
        sizeConfigs,
        infoConfigs,
        styleConfigs,
        settingConfigs,
        selectedItems,
        k,
        groupedObjDataColumns?.[k]?.color,
        gcIndex,
      );
    });
  }

  protected getMetricAndSizeSerie(
    { max, min }: { max: number; min: number },
    dataSetRows: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
    styleConfigs: ChartStyleConfig[],
    settingConfigs: ChartStyleConfig[],
    selectedItems?: SelectedItem[],
    colorSeriesName?: string,
    color?: string,
    comIndex: number = 0,
  ): ScatterMetricAndSizeSerie {
    const [cycleRatio] = getStyles(styleConfigs, ['scatter'], ['cycleRatio']);
    const seriesName = groupConfigs
      ?.map(gc => getColumnRenderName(gc))
      .join('-');
    const defaultSizeValue = (max - min) / 2;
    const seriesDatas = dataSetRows?.map((row, dcIndex) => {
      const sizeValue = sizeConfigs?.length
        ? row.getCell(sizeConfigs?.[0]) || min
        : defaultSizeValue;
      return {
        ...getExtraSeriesRowData(row),
        ...getSelectedItemStyles(comIndex, dcIndex, selectedItems || []),
        name: groupConfigs?.map(row.getCell, row).join('-'),
        value: aggregateConfigs
          .map(row.getCell, row)
          .concat(infoConfigs?.map(row.getCell, row))
          .concat([sizeValue, colorSeriesName] as any),
      };
    });

    const sizeValueIndex = ([] as ChartDataSectionField[])
      .concat(aggregateConfigs)
      .concat(infoConfigs)?.length;

    return {
      name: colorSeriesName || seriesName,
      type: 'scatter',
      data: seriesDatas,
      symbolSize: getScatterSymbolSizeFn(sizeValueIndex, max, min, cycleRatio),
      itemStyle: {
        color,
      },
      ...this.getLabelStyle(styleConfigs),
      ...getReference2(
        settingConfigs,
        dataSetRows,
        aggregateConfigs?.[1],
        true,
      ),
    };
  }

  private getAxis(
    styles: ChartStyleConfig[],
    xAxisColumn: { type: string; name: string },
    axisKey: string,
  ): YAxis {
    const [
      showAxis,
      inverse,
      lineStyle,
      showLabel,
      font,
      unitFont,
      showTitleAndUnit,
      nameLocation,
      nameGap,
      nameRotate,
      min,
      max,
    ] = getStyles(
      styles,
      [axisKey],
      [
        'showAxis',
        'inverseAxis',
        'lineStyle',
        'showLabel',
        'font',
        'unitFont',
        'showTitleAndUnit',
        'nameLocation',
        'nameGap',
        'nameRotate',
        'min',
        'max',
      ],
    );
    const name = showTitleAndUnit
      ? [xAxisColumn].map(c => c.name).join(' / ')
      : null;
    const splitLineProps =
      axisKey === 'xAxis'
        ? ['showHorizonLine', 'horizonLineStyle']
        : ['showVerticalLine', 'verticalLineStyle'];
    const [showSplitLine, splitLineStyle] = getStyles(
      styles,
      ['splitLine'],
      [splitLineProps[0], splitLineProps[1]],
    );

    const [format] = getStyles(
      styles,
      [axisKey, 'modal'],
      ['YAxisNumberFormat'],
    );

    return {
      type: 'value',
      inverse,
      name,
      nameLocation,
      nameGap,
      nameRotate,
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
        show: showSplitLine,
        lineStyle: splitLineStyle,
      },
    };
  }

  private getLegendStyle(
    styles: ChartStyleConfig[],
    seriesNames: string[],
  ): LegendStyle {
    const [show, type, font, legendPos, selectAll, height] = getStyles(
      styles,
      ['legend'],
      ['showLegend', 'type', 'font', 'position', 'selectAll', 'height'],
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
    const selected = seriesNames.reduce(
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
      height: height || null,
      orient,
      selected,
      data: seriesNames,
      textStyle: font,
    };
  }

  private getLabelStyle(styles: ChartStyleConfig[]): LabelStyle {
    const [show, position, font] = getStyles(
      styles,
      ['label'],
      ['showLabel', 'position', 'font'],
    );
    return {
      label: { show, position, ...font, formatter: '{b}' },
      labelLayout: { hideOverlap: true },
    };
  }

  private getTooltipFormmaterFunc(
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    colorConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
    chartDataSet: IChartDataSet<string>,
  ): (params) => string {
    return seriesParams => {
      return getSeriesTooltips4Polar2(
        chartDataSet,
        seriesParams,
        groupConfigs,
        colorConfigs,
        aggregateConfigs,
        infoConfigs,
        sizeConfigs,
      );
    };
  }
}

export default BasicScatterChart;
