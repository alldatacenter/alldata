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
import {
  ChartConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  LabelStyle,
  LegendStyle,
  SelectedItem,
  SeriesStyle,
  XAxis,
  XAxisColumns,
  YAxis,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getAxisLabel,
  getAxisLine,
  getAxisTick,
  getColorizeGroupSeriesColumns,
  getColumnRenderName,
  getDrillableRows,
  getExtraSeriesDataFormat,
  getExtraSeriesRowData,
  getGridStyle,
  getNameTextStyle,
  getReference2,
  getSeriesTooltips4Rectangular2,
  getSplitLine,
  getStyles,
  hadAxisLabelOverflowConfig,
  setOptionsByAxisLabelOverflow,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import { transparentize } from 'polished';
import { UniqArray } from 'utils/object';
import Chart from '../../../models/Chart';
import Config from './config';
import { Series } from './types';

class BasicLineChart extends Chart {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  protected isArea = false;
  protected isStack = false;
  protected dataZoomConfig: {
    setConfig: any;
    showConfig: any;
  } = {
    setConfig: null,
    showConfig: null,
  };

  constructor(props?) {
    super(
      props?.id || 'line',
      props?.name || 'viz.palette.graph.names.lineChart',
      props?.icon || 'chart-line',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: 1,
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

  onResize(options: BrokerOption, context: BrokerContext) {
    this.chart?.resize({ width: context?.width, height: context?.height });
    hadAxisLabelOverflowConfig(this.chart?.getOption()) &&
      this.onUpdated(options, context);
  }

  onUnMount(options: BrokerOption, context: BrokerContext) {
    this.selectionManager?.removeWindowListeners(context.window);
    this.selectionManager?.removeZRenderListeners(this.chart);
    this.chart?.dispose();
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
    const colorConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Color)
      .flatMap(config => config.rows || []);
    const infoConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );

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
    const yAxisNames: string[] = aggregateConfigs.map(getColumnRenderName);

    // @TM 溢出自动根据bar长度设置标尺
    const option = setOptionsByAxisLabelOverflow({
      chart: this.chart,
      grid: getGridStyle(styleConfigs),
      xAxis: this.getXAxis(styleConfigs, xAxisColumns),
      yAxis: this.getYAxis(styleConfigs, yAxisNames),
      series,
      yAxisNames,
    });

    return {
      tooltip: {
        trigger: 'item',
        formatter: this.getTooltipFormmaterFunc(
          chartDataSet,
          groupConfigs,
          aggregateConfigs,
          colorConfigs,
          infoConfigs,
        ),
      },
      legend: this.getLegendStyle(
        styleConfigs,
        series?.map(s => s.name),
      ),
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
    if (!colorConfigs?.length) {
      return aggregateConfigs.map((aggConfig, acIndex) => {
        const color = aggConfig?.color?.start;
        return {
          name: getColumnRenderName(aggConfig),
          type: 'line',
          sampling: 'average',
          areaStyle: this.isArea
            ? {
                color,
                opacity: selectedItems?.length ? 0.4 : undefined,
              }
            : undefined,
          stack: this.isStack ? 'total' : undefined,
          data: chartDataSet.map((dc, dcIndex) => ({
            ...getExtraSeriesRowData(dc),
            ...getExtraSeriesDataFormat(aggConfig?.format),
            ...this.getLineSelectItemStyle(
              acIndex,
              dcIndex,
              selectedItems || [],
            ),
            value: dc.getCell(aggConfig),
          })),
          itemStyle: {
            color,
          },
          lineStyle: {
            opacity: selectedItems?.length ? 0.5 : 1,
          },
          ...this.getLabelStyle(styleConfigs),
          ...this.getSeriesStyle(styleConfigs),
          ...getReference2(settingConfigs, chartDataSet, aggConfig, false),
        };
      });
    }

    const secondGroupInfos = getColorizeGroupSeriesColumns(
      chartDataSet,
      colorConfigs[0],
    );
    return aggregateConfigs.flatMap((aggConfig, acIndex) => {
      return secondGroupInfos.map((sgCol, sgcIndex) => {
        const k = Object.keys(sgCol)[0];
        const dataSet = sgCol[k];

        const itemStyleColor = colorConfigs?.[0]?.color?.colors?.find(
          c => c.key === k,
        );
        return {
          name: k,
          type: 'line',
          sampling: 'average',
          areaStyle: this.isArea ? {} : undefined,
          stack: this.isStack ? 'total' : undefined,
          itemStyle: {
            normal: { color: itemStyleColor?.value },
          },
          data: xAxisColumns[0].data.map((d, dcIndex) => {
            const row = dataSet.find(
              r =>
                groupConfigs?.map(gc => String(r.getCell(gc))).join('-') === d,
            )!;
            return {
              ...getExtraSeriesRowData(row),
              ...getExtraSeriesDataFormat(aggConfig?.format),
              ...this.getLineSelectItemStyle(
                sgcIndex,
                acIndex * secondGroupInfos.length + dcIndex,
                selectedItems || [],
              ),
              name: getColumnRenderName(aggConfig),
              value: row?.getCell(aggConfig),
            };
          }),
          ...this.getLabelStyle(styleConfigs),
          ...this.getSeriesStyle(styleConfigs),
        };
      });
    });
  }

  getLineSelectItemStyle(
    comIndex: string | number,
    dcIndex: string | number,
    selectList: SelectedItem[],
  ) {
    const findIndex = selectList.findIndex(
      v => v.index === comIndex + ',' + dcIndex,
    );
    return findIndex >= 0 ? { symbol: 'circle', symbolSize: 10 } : {};
  }

  private getYAxis(styles, yAxisNames): YAxis {
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
    const [showHorizonLine, horizonLineStyle] = getStyles(
      styles,
      ['splitLine'],
      ['showHorizonLine', 'horizonLineStyle'],
    );
    const [format] = getStyles(
      styles,
      ['yAxis', 'modal'],
      ['YAxisNumberFormat'],
    );
    const name = showTitleAndUnit ? yAxisNames.join(' / ') : null;

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
      axisLine: getAxisLine(showAxis, lineStyle),
      axisTick: getAxisTick(showLabel, lineStyle),
      nameTextStyle: getNameTextStyle(
        unitFont?.fontFamily,
        unitFont?.fontSize,
        unitFont?.color,
      ),
      splitLine: getSplitLine(showHorizonLine, horizonLineStyle),
    };
  }

  private getXAxis(styles, xAxisColumns): XAxis {
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
    };
  }

  private getLegendStyle(styles, seriesNames): LegendStyle {
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
      orient,
      height: height || null,
      selected,
      data: seriesNames,
      textStyle: font,
      itemStyle: {
        opacity: 1,
      },
      lineStyle: {
        opacity: 1,
      },
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
          const formattedValue = toFormattedValue(value, data.format);
          const labels: string[] = [];
          labels.push(formattedValue);
          return labels.join('\n');
        },
      },
      labelLayout: { hideOverlap: true },
    };
  }

  private getSeriesStyle(styles): SeriesStyle {
    const [smooth, step, connectNulls] = getStyles(
      styles,
      ['graph'],
      ['smooth', 'step', 'connectNulls'],
    );
    return { smooth, step, connectNulls };
  }

  private getTooltipFormmaterFunc(
    chartDataSet,
    groupConfigs,
    aggregateConfigs,
    colorConfigs,
    infoConfigs,
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

export default BasicLineChart;
