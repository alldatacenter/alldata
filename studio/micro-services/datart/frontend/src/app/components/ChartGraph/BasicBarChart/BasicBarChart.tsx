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
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import { ChartSelectionManager } from 'app/models/ChartSelectionManager';
import { IChartLifecycle } from 'app/types/Chart';
import {
  ChartConfig,
  ChartDataConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  LabelStyle,
  LegendStyle,
  SelectedItem,
  XAxis,
  XAxisColumns,
  YAxis,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getColorizeGroupSeriesColumns,
  getColumnRenderName,
  getDrillableRows,
  getExtraSeriesDataFormat,
  getExtraSeriesRowData,
  getGridStyle,
  getReference2,
  getSelectedItemStyles,
  getSeriesTooltips4Rectangular2,
  getStyles,
  hadAxisLabelOverflowConfig,
  setOptionsByAxisLabelOverflow,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { toPrecision } from 'app/utils/number';
import { init } from 'echarts';
import { transparentize } from 'polished';
import { UniqArray } from 'utils/object';
import Chart from '../../../models/Chart';
import { ChartRequirement } from '../../../types/ChartMetadata';
import Config from './config';
import { BarBorderStyle, BarSeriesImpl, Series } from './types';

class BasicBarChart extends Chart implements IChartLifecycle {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  protected isHorizonDisplay = false;
  protected isStackMode = false;
  protected isPercentageYAxis = false;
  protected dataZoomConfig: {
    setConfig: any;
    showConfig: any;
  } = {
    setConfig: null,
    showConfig: null,
  };

  constructor(props?: {
    id: string;
    name: string;
    icon: string;
    requirements?: ChartRequirement[];
  }) {
    super(
      props?.id || 'bar',
      props?.name || 'viz.palette.graph.names.barChart',
      props?.icon || 'chart-bar',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: [0, 1],
        aggregate: [1, 999],
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

    // TODO(TL): refactor to chart data zoom manager model
    this.chart.on('datazoom', ({ end, start }) => {
      this.dataZoomConfig.showConfig = {
        end,
        start,
      };
    });
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
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
    this.chart?.resize({ width: context?.width, height: context?.height });
    hadAxisLabelOverflowConfig(this.chart?.getOption()) &&
      this.onUpdated(options, context);
  }

  getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    drillOption?: ChartDrillOption,
    selectedItems?: SelectedItem[],
  ) {
    const styleConfigs: ChartStyleConfig[] = config.styles || [];
    const dataConfigs: ChartDataConfig[] = config.datas || [];
    const settingConfigs: ChartStyleConfig[] = config.settings || [];

    const groupConfigs: ChartDataSectionField[] = getDrillableRows(
      dataConfigs,
      drillOption,
    );
    const aggregateConfigs: ChartDataSectionField[] = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Aggregate)
      .flatMap(config => config.rows || []);
    const colorConfigs: ChartDataSectionField[] = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Color)
      .flatMap(config => config.rows || []);
    const infoConfigs: ChartDataSectionField[] = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );

    if (this.isHorizonDisplay) {
      chartDataSet.reverse();
    }
    const xAxisColumns: XAxisColumns[] = [
      {
        type: 'category',
        tooltip: { show: true },
        data: UniqArray(
          chartDataSet?.map(row => {
            return groupConfigs.map(g => row.getCell(g)).join('-');
          }),
        ),
      },
    ];

    const yAxisNames: string[] = aggregateConfigs.map(getColumnRenderName);
    const series = this.getSeries(
      settingConfigs,
      styleConfigs,
      colorConfigs,
      chartDataSet,
      groupConfigs,
      aggregateConfigs,
      infoConfigs,
      xAxisColumns,
      selectedItems,
    );

    const axisInfo = {
      xAxis: this.getXAxis(styleConfigs, xAxisColumns),
      yAxis: this.getYAxis(styleConfigs, yAxisNames),
    };

    if (this.isStackMode) {
      this.makeStackSeries(styleConfigs, series);
    }
    if (this.isPercentageYAxis) {
      this.makePercentageSeries(styleConfigs, series);
      this.makePercentageYAxis(axisInfo);
    }
    if (this.isHorizonDisplay) {
      this.makeTransposeAxis(axisInfo);
    }

    // @TM 溢出自动根据bar长度设置标尺
    const option = setOptionsByAxisLabelOverflow({
      chart: this.chart,
      xAxis: axisInfo.xAxis,
      yAxis: axisInfo.yAxis,
      grid: getGridStyle(styleConfigs),
      yAxisNames,
      series,
      horizon: this.isHorizonDisplay,
    });

    return {
      tooltip: {
        trigger: 'item',
        formatter: this.getTooltipFormatterFunc(
          chartDataSet,
          groupConfigs,
          aggregateConfigs,
          colorConfigs,
          infoConfigs,
        ),
      },
      legend: this.getLegendStyle(styleConfigs, series),
      dataZoom: this.getDataZoom(styleConfigs),
      ...option,
    };
  }

  private getDataZoom(styles: ChartStyleConfig[]) {
    const [
      showZoomSlider,
      zoomSliderColor,
      usePercentage,
      start,
      end,
      startValue,
      endValue,
    ] = getStyles(
      styles,
      ['xAxis', 'dataZoomPanel'],
      [
        'showZoomSlider',
        'zoomSliderColor',
        'usePercentage',
        'zoomStartPercentage',
        'zoomEndPercentage',
        'zoomStartIndex',
        'zoomEndIndex',
      ],
    );

    const _getDataZoomStartAndEnd = setConfig => {
      if (
        JSON.stringify(this.dataZoomConfig.setConfig) !==
          JSON.stringify(setConfig) ||
        !this.dataZoomConfig.showConfig
      ) {
        this.dataZoomConfig.setConfig = {
          ...setConfig,
        };
        return setConfig.usePercentage
          ? {
              start: setConfig.start,
              end: setConfig.end,
            }
          : {
              startValue: setConfig.startValue,
              endValue: setConfig.endValue,
            };
      }
      return this.dataZoomConfig.showConfig;
    };

    return showZoomSlider
      ? [
          {
            type: 'slider',
            handleSize: '100%',
            show: true,
            orient: this.isHorizonDisplay ? 'vertical' : 'horizontal',
            dataBackground: {
              lineStyle: {
                color: transparentize(0.7, zoomSliderColor),
              },
              areaStyle: {
                color: transparentize(0.7, zoomSliderColor),
              },
            },
            selectedDataBackground: {
              lineStyle: {
                color: zoomSliderColor,
              },
              areaStyle: {
                color: zoomSliderColor,
              },
            },
            brushStyle: {
              color: transparentize(0.85, zoomSliderColor),
            },
            emphasis: {
              handleStyle: {
                color: zoomSliderColor,
              },
              moveHandleStyle: {
                color: zoomSliderColor,
              },
            },
            fillerColor: transparentize(0.85, zoomSliderColor),
            ..._getDataZoomStartAndEnd({
              usePercentage,
              start,
              end,
              startValue,
              endValue,
            }),
          },
        ]
      : [];
  }

  private makePercentageYAxis(axisInfo: { xAxis: XAxis; yAxis: YAxis }) {
    if (axisInfo.yAxis) {
      axisInfo.yAxis.min = 0;
      axisInfo.yAxis.max = 100;
    }
  }

  private makeTransposeAxis(info) {
    const temp = info.xAxis;
    info.xAxis = info.yAxis;
    info.yAxis = temp;
  }

  private getSeries(
    settingConfigs: ChartStyleConfig[],
    styleConfigs: ChartStyleConfig[],
    colorConfigs: ChartDataSectionField[],
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
    xAxisColumns: XAxisColumns[],
    selectedItems?: SelectedItem[],
  ): Series[] {
    if (!colorConfigs.length) {
      return aggregateConfigs.map((aggConfig, sIndex) => {
        return {
          ...this.getBarSeriesImpl(
            styleConfigs,
            settingConfigs,
            chartDataSet,
            aggConfig,
          ),
          name: getColumnRenderName(aggConfig),
          data: chartDataSet?.map((dc, dIndex) => {
            return {
              ...getExtraSeriesRowData(dc),
              ...getExtraSeriesDataFormat(aggConfig?.format),
              ...getSelectedItemStyles(sIndex, dIndex, selectedItems || []),
              name: getColumnRenderName(aggConfig),
              value: dc.getCell(aggConfig),
            };
          }),
        };
      });
    }

    const secondGroupInfos = getColorizeGroupSeriesColumns(
      chartDataSet,
      colorConfigs[0],
    );

    const colorizeGroupedSeries = aggregateConfigs.flatMap(
      (aggConfig, acIndex) => {
        return secondGroupInfos.map((sgCol, sgIndex) => {
          const k = Object.keys(sgCol)[0];
          const dataSet = sgCol[k];

          const itemStyleColor = colorConfigs?.[0]?.color?.colors?.find(
            c => c.key === k,
          );

          return {
            ...this.getBarSeriesImpl(
              styleConfigs,
              settingConfigs,
              chartDataSet,
              aggConfig,
            ),
            name: k,
            data: xAxisColumns?.[0]?.data?.map((d, dIndex) => {
              const row = dataSet.find(
                r =>
                  groupConfigs?.map(gc => String(r.getCell(gc))).join('-') ===
                  d,
              )!;
              return {
                ...getExtraSeriesRowData(row),
                ...getExtraSeriesDataFormat(aggConfig?.format),
                name: getColumnRenderName(aggConfig),
                value: row?.getCell(aggConfig),
                ...getSelectedItemStyles(
                  acIndex * secondGroupInfos.length + sgIndex,
                  dIndex,
                  selectedItems || [],
                ),
              };
            }),
            itemStyle: this.getSeriesItemStyle(styleConfigs, {
              color: itemStyleColor?.value,
            }),
          };
        });
      },
    );
    return colorizeGroupedSeries;
  }

  private getBarSeriesImpl(
    styleConfigs: ChartStyleConfig[],
    settingConfigs: ChartStyleConfig[],
    chartDataSet: IChartDataSet<string>,
    dataConfig: ChartDataSectionField,
  ): BarSeriesImpl {
    return {
      type: 'bar',
      sampling: 'average',
      barGap: this.getSeriesBarGap(styleConfigs),
      barWidth: this.getSeriesBarWidth(styleConfigs),
      itemStyle: this.getSeriesItemStyle(styleConfigs, {
        color: dataConfig?.color?.start,
      }),
      ...this.getLabelStyle(styleConfigs),
      ...this.getSeriesStyle(styleConfigs),
      ...getReference2(
        settingConfigs,
        chartDataSet,
        dataConfig,
        this.isHorizonDisplay,
      ),
    };
  }

  private makeStackSeries(_: ChartStyleConfig[], series: Series[]): Series[] {
    (series || []).forEach(s => {
      s['stack'] = this.isStackMode ? this.getStackName(1) : undefined;
    });
    return series;
  }

  private makePercentageSeries(
    styles: ChartStyleConfig[],
    series: Series[],
  ): Series[] {
    const _getAbsValue = data => {
      if (typeof data === 'object' && data !== null && 'value' in data) {
        return Math.abs(data.value || 0);
      }
      return data;
    };

    const _convertToPercentage = (data, totalArray: number[]) => {
      return (data || []).map((d, dataIndex) => {
        const sum = totalArray[dataIndex];
        const percentageValue = toPrecision((_getAbsValue(d) / sum) * 100, 2);
        return {
          ...d,
          value: percentageValue,
          total: sum,
        };
      });
    };

    const _seriesTotalArrayByDataIndex = (series?.[0]?.data || []).map(
      (_, index) => {
        const sum = series.reduce((acc, cur) => {
          const value = +_getAbsValue(cur.data?.[index] || 0);
          acc = acc + value;
          return acc;
        }, 0);
        return sum;
      },
    );
    (series || []).forEach(s => {
      s.data = _convertToPercentage(s.data, _seriesTotalArrayByDataIndex);
    });
    return series;
  }

  private getSeriesItemStyle(
    styles: ChartStyleConfig[],
    itemStyle?: { color: string | undefined },
  ): BarBorderStyle {
    const [borderStyle, borderRadius] = getStyles(
      styles,
      ['bar'],
      ['borderStyle', 'radius'],
    );

    return {
      ...itemStyle,
      borderRadius,
      borderType: borderStyle?.type,
      borderWidth: borderStyle?.width,
      borderColor: borderStyle?.color,
    };
  }

  private getSeriesBarGap(styles: ChartStyleConfig[]): number {
    const [gap] = getStyles(styles, ['bar'], ['gap']);
    return gap;
  }

  private getSeriesBarWidth(styles: ChartStyleConfig[]): number {
    const [width] = getStyles(styles, ['bar'], ['width']);
    return width;
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

  private getXAxis(
    styles: ChartStyleConfig[],
    xAxisColumns: XAxisColumns[],
  ): XAxis {
    const axisColumnInfo = xAxisColumns[0];
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
      ...axisColumnInfo,
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

  private getLegendStyle(styles, series): LegendStyle {
    const seriesNames: string[] = (series || []).map((col: any) => col?.name);
    const [show, type, font, legendPos, selectAll, height] = getStyles(
      styles,
      ['legend'],
      ['showLegend', 'type', 'font', 'position', 'selectAll', 'height'],
    );
    let positions: {
      width?: number;
      height?: number;
      top?: number;
      left?: number;
      right?: number;
      bottom?: number;
    } = {};
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
      height: height || null,
      orient,
      selected,
      data: seriesNames,
      textStyle: font,
    };
  }

  private getLabelStyle(styles): LabelStyle {
    const [show, position, font] = getStyles(
      styles,
      ['label'],
      ['showLabel', 'position', 'font'],
    );

    return {
      label: {
        show,
        position,
        ...font,
        formatter: params => {
          const { value, data } = params;
          if ((!value || !Number(value)) && value !== 0) {
            return '';
          }
          if (this.isPercentageYAxis) {
            return value;
          }
          return toFormattedValue(value, data.format);
        },
      },
      labelLayout: { hideOverlap: true },
    };
  }

  private getSeriesStyle(styles): { smooth: boolean; step: boolean } {
    const [smooth, step] = getStyles(styles, ['graph'], ['smooth', 'step']);
    return { smooth, step };
  }

  private getStackName(index): string {
    return `total`;
  }

  private getTooltipFormatterFunc(
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    colorConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
  ): (params) => string {
    return seriesParams => {
      const params = Array.isArray(seriesParams)
        ? seriesParams
        : [seriesParams];
      return getSeriesTooltips4Rectangular2(
        chartDataSet,
        params[0],
        groupConfigs,
        colorConfigs,
        aggregateConfigs,
        infoConfigs,
      );
    };
  }
}

export default BasicBarChart;
