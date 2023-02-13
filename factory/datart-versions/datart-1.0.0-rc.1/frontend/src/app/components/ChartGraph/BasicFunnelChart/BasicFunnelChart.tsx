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
} from 'app/types/ChartConfig';
import ChartDataSetDTO, {
  IChartDataSet,
  IChartDataSetRow,
} from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getAutoFunnelTopPosition,
  getColumnRenderName,
  getDrillableRows,
  getExtraSeriesDataFormat,
  getExtraSeriesRowData,
  getGridStyle,
  getSelectedItemStyles,
  getSeriesTooltips4Scatter,
  getStyles,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import isEmpty from 'lodash/isEmpty';
import Config from './config';
import { Series, SeriesData } from './types';

class BasicFunnelChart extends Chart {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  constructor() {
    super(
      'funnel-chart',
      'viz.palette.graph.names.funnelChart',
      'fsux_tubiao_loudoutu',
    );
    this.meta.requirements = [
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
  }

  onUpdated(options: BrokerOption, context: BrokerContext) {
    if (!options.dataset || !options.dataset.columns || !options.config) {
      return;
    }

    this.chart?.clear();
    if (!this.isMatchRequirement(options.config)) {
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

  onResize(options: BrokerOption, context: BrokerContext): void {
    this.chart?.resize({ width: context?.width, height: context?.height });
  }

  private getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    drillOption?: ChartDrillOption,
    selectedItems?: SelectedItem[],
  ) {
    const styleConfigs = config.styles || [];
    const dataConfigs = config.datas || [];
    const groupConfigs: ChartDataSectionField[] = getDrillableRows(
      dataConfigs,
      drillOption,
    );
    const aggregateConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Aggregate)
      .flatMap(config => config.rows || []);
    const infoConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );
    const dataList = !groupConfigs.length
      ? chartDataSet
      : chartDataSet?.sort(
          (a, b) =>
            (b?.getCell(aggregateConfigs[0]) as any) -
            (a?.getCell(aggregateConfigs[0]) as any),
        );
    const aggregateList = !groupConfigs.length
      ? aggregateConfigs?.sort(
          (a, b) =>
            (chartDataSet?.[0]?.getCell(b) as any) -
            (chartDataSet?.[0]?.getCell(a) as any),
        )
      : aggregateConfigs;

    const series = this.getSeries(
      styleConfigs,
      aggregateList,
      groupConfigs,
      dataList,
      infoConfigs,
      selectedItems,
    );

    return {
      tooltip: this.getFunnelChartTooltip(
        groupConfigs,
        aggregateList,
        infoConfigs,
      ),
      legend: this.getLegendStyle(styleConfigs, series.sort),
      animation: false,
      series,
    };
  }

  private getDataItemStyle(
    config: ChartDataSectionField,
    colorConfigs: ChartDataSectionField[],
    chartDataSetRow: IChartDataSetRow<string>,
  ): { color: string | undefined } | undefined {
    const colorColConfig = colorConfigs?.[0];
    const columnColor = config?.color?.start;
    if (colorColConfig) {
      const colorKey = chartDataSetRow.getCell(colorColConfig);
      const itemStyleColor = colorConfigs[0]?.color?.colors?.find(
        c => c.key === colorKey,
      );

      return {
        color: itemStyleColor?.value,
      };
    } else if (columnColor) {
      return {
        color: columnColor,
      };
    }
  }

  private getLabelStyle(styles: ChartStyleConfig[]): LabelStyle {
    const [show, position, font, metric, conversion, arrival, percentage] =
      getStyles(
        styles,
        ['label'],
        [
          'showLabel',
          'position',
          'font',
          'metric',
          'conversion',
          'arrival',
          'percentage',
        ],
      );

    return {
      show,
      position,
      ...font,
      formatter: params => {
        const { name, value, percent, data } = params;
        const formattedValue = toFormattedValue(value?.[0], data.format);
        const labels: string[] = [];
        if (metric) {
          labels.push(`${name}: ${formattedValue}`);
        }
        if (conversion && !isEmpty(data.conversion)) {
          labels.push(`转化率: ${data.conversion}%`);
        }
        if (arrival && !isEmpty(data.arrival)) {
          labels.push(`到达率: ${data.arrival}%`);
        }
        if (percentage) {
          labels.push(`百分比: ${percent}%`);
        }

        return labels.join('\n');
      },
    };
  }

  private getLegendStyle(styles: ChartStyleConfig[], sort): LegendStyle {
    const [show, type, font, legendPos, height] = getStyles(
      styles,
      ['legend'],
      ['showLegend', 'type', 'font', 'position', 'height'],
    );
    let positions = {};
    let orient = '';

    const top = getAutoFunnelTopPosition({
      chart: this.chart,
      sort,
      legendPos,
      height,
    });

    switch (legendPos) {
      case 'top':
        orient = 'horizontal';
        positions = { top, left: 8, right: 8, height: 32 };
        break;
      case 'bottom':
        orient = 'horizontal';
        positions = { bottom: 8, left: 8, right: 8, height: 32 };
        break;
      case 'left':
        orient = 'vertical';
        positions = { left: 8, top, bottom: 24, width: 96 };
        break;
      default:
        orient = 'vertical';
        positions = { right: 8, top, bottom: 24, width: 96 };
        break;
    }

    return {
      ...positions,
      height: height || null,
      show,
      type,
      orient,
      textStyle: font,
      itemStyle: {
        opacity: 1,
      },
    };
  }

  private getSeries(
    styles: ChartStyleConfig[],
    aggregateConfigs: ChartDataSectionField[],
    groupConfigs: ChartDataSectionField[],
    dataList: IChartDataSet<string>,
    infoConfigs: ChartDataSectionField[],
    selectedItems?: SelectedItem[],
  ): Series {
    const [selectAll] = getStyles(styles, ['legend'], ['selectAll']);
    const [sort, funnelAlign, gap] = getStyles(
      styles,
      ['funnel'],
      ['sort', 'align', 'gap'],
    );

    if (!groupConfigs.length) {
      const dc = dataList?.[0];
      const datas: SeriesData[] = aggregateConfigs.map((aggConfig, acIndex) => {
        const dataItemStyle = this.getDataItemStyle(
          aggConfig,
          groupConfigs,
          dc,
        );
        return {
          ...aggConfig,
          select: selectAll,
          value: [aggConfig]
            .concat(infoConfigs)
            .map(config => dc?.getCell(config)),
          name: getColumnRenderName(aggConfig),
          ...getSelectedItemStyles(
            0,
            acIndex,
            selectedItems || [],
            dataItemStyle,
          ),
          ...getExtraSeriesRowData(dc),
          ...getExtraSeriesDataFormat(aggConfig?.format),
        };
      });
      return {
        ...getGridStyle(styles),
        type: 'funnel',
        funnelAlign,
        sort,
        gap,
        labelLine: {
          length: 10,
          lineStyle: {
            width: 1,
            type: 'solid',
          },
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)',
        },
        label: this.getLabelStyle(styles),
        labelLayout: { hideOverlap: true },
        data: this.getFunnelSeriesData(datas),
      };
    }

    const flattenedDatas = aggregateConfigs.flatMap((aggConfig, acIndex) => {
      const ormalizeSerieDatas: SeriesData[] = dataList.map((dc, dcIndex) => {
        const dataItemStyle = this.getDataItemStyle(
          aggConfig,
          groupConfigs,
          dc,
        );
        return {
          ...aggConfig,
          select: selectAll,
          value: aggregateConfigs
            .concat(infoConfigs)
            .map(config => dc?.getCell(config)),
          name: groupConfigs.map(config => dc.getCell(config)).join('-'),
          ...getSelectedItemStyles(
            0,
            acIndex * dataList.length + dcIndex,
            selectedItems || [],
            dataItemStyle,
          ),
          ...getExtraSeriesRowData(dc),
          ...getExtraSeriesDataFormat(aggConfig?.format),
        };
      });
      return ormalizeSerieDatas;
    });

    const series = {
      ...getGridStyle(styles),
      type: 'funnel',
      funnelAlign,
      sort,
      gap,
      labelLine: {
        length: 10,
        lineStyle: {
          width: 1,
          type: 'solid',
        },
      },
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)',
      },
      label: this.getLabelStyle(styles),
      labelLayout: { hideOverlap: true },
      data: this.getFunnelSeriesData(flattenedDatas),
    };
    return series;
  }

  private getFunnelSeriesData(seriesData: SeriesData[]) {
    const _calculateConversionAndArrivalRatio = (data, index) => {
      if (index) {
        data.conversion = this.formatPercent(
          (data.value?.[0] / Number(seriesData[index - 1].value?.[0])) * 100,
        );
        data.arrival = this.formatPercent(
          (data.value?.[0] / Number(seriesData[0].value?.[0])) * 100,
        );
      }
      return data;
    };

    return seriesData.map(_calculateConversionAndArrivalRatio);
  }

  private formatPercent(per: number): string {
    const perStr = per + '';
    return perStr.length - (perStr.indexOf('.') + 1) > 2
      ? per.toFixed(2)
      : perStr;
  }

  private getFunnelChartTooltip(
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
  ): { trigger: string; formatter: (params) => string } {
    return {
      trigger: 'item',
      formatter(params) {
        const { data } = params;
        let tooltips: string[] = !!groupConfigs?.length
          ? [
              `${groupConfigs?.map(gc => getColumnRenderName(gc)).join('-')}: ${
                params?.name
              }`,
            ]
          : [];
        const aggTooltips = !!groupConfigs?.length
          ? getSeriesTooltips4Scatter(
              [params],
              aggregateConfigs.concat(infoConfigs),
            )
          : getSeriesTooltips4Scatter([params], [data].concat(infoConfigs));
        tooltips = tooltips.concat(aggTooltips);
        if (data.conversion) {
          tooltips.push(`转化率: ${data.conversion}%`);
        }
        if (data.arrival) {
          tooltips.push(`到达率: ${data.arrival}%`);
        }
        return tooltips.join('<br/>');
      },
    };
  }
}

export default BasicFunnelChart;
