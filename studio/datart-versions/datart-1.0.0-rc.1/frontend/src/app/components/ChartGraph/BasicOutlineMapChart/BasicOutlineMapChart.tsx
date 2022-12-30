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
  SelectedItem,
} from 'app/types/ChartConfig';
import ChartDataSetDTO, { IChartDataSet } from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  getDataColumnMaxAndMin2,
  getExtraSeriesRowData,
  getScatterSymbolSizeFn,
  getSelectedItemStyles,
  getSeriesTooltips4Polar2,
  getStyles,
  hadAxisLabelOverflowConfig,
  toFormattedValue,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { ECharts, init, registerMap } from 'echarts';
import { CloneValueDeep } from 'utils/object';
import Config from './config';
import geoChinaCity from './geo-china-city.map.json';
import geoChina from './geo-china.map.json';
import {
  GeoInfo,
  GeoSeries,
  GeoVisualMapStyle,
  MapOption,
  MetricAndSizeSeriesStyle,
} from './types';

// NOTE: source from: http://datav.aliyun.com/tools/atlas/index.html#&lat=31.39115752282472&lng=103.7548828125&zoom=4
registerMap('china', geoChina as any);
registerMap('china-city', geoChinaCity as any);

class BasicOutlineMapChart extends Chart {
  config = Config;
  chart: any = null;
  selectionManager?: ChartSelectionManager;

  protected isNormalGeoMap = false;
  private geoMap;
  private container: HTMLElement | null = null;
  private geoConfig: GeoInfo = {
    center: undefined,
    zoom: 1,
  };

  constructor(props?) {
    super(
      props?.id || 'outline-map',
      props?.name || 'viz.palette.graph.names.outlineMap',
      props?.icon || 'china',
    );
    this.meta.requirements = props?.requirements || [
      {
        group: 1,
        aggregate: 1,
      },
    ];
  }

  getOptionsConfig(geoConfig: GeoInfo, chart?: ECharts) {
    const newOption: any = CloneValueDeep(chart?.getOption());
    geoConfig.center = newOption?.geo?.[0].center;
    geoConfig.zoom = newOption?.geo?.[0].zoom;
  }

  onMount(options: BrokerOption, context: BrokerContext) {
    if (
      options.containerId === undefined ||
      !context.document ||
      !context.window
    ) {
      return;
    }
    this.container = context.document.getElementById(options.containerId);
    this.chart = init(this.container!, 'default');
    this.selectionManager = new ChartSelectionManager(this.mouseEvents);
    this.selectionManager.attachWindowListeners(context.window);
    this.selectionManager.attachZRenderListeners(this.chart);
    this.selectionManager.attachEChartsListeners(this.chart);
    this.container?.addEventListener('mouseup', () =>
      this.getOptionsConfig(this.geoConfig, this.chart),
    );
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
      options.selectedItems,
      context,
    );
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  onResize(options: BrokerOption, context: BrokerContext) {
    this.chart?.resize({ width: context?.width, height: context?.height });
    hadAxisLabelOverflowConfig(this.chart?.getOption()) &&
      this.onUpdated(options, context);
  }

  onUnMount(options: BrokerOption, context: BrokerContext) {
    this.container?.removeEventListener('mouseup', () =>
      this.getOptionsConfig(this.geoConfig, this.chart),
    );
    this.selectionManager?.removeWindowListeners(context.window);
    this.selectionManager?.removeZRenderListeners(this.chart);
    this.chart?.dispose();
  }

  private getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
    selectedItems?: SelectedItem[],
    context?: BrokerContext,
  ): MapOption {
    const styleConfigs = config.styles || [];
    const dataConfigs = config.datas || [];
    const groupConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Group)
      .flatMap(config => config.rows || []);
    const aggregateConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Aggregate)
      .flatMap(config => config.rows || []);
    const sizeConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Size)
      .flatMap(config => config.rows || []);
    const infoConfigs = dataConfigs
      .filter(c => c.type === ChartDataSectionType.Info)
      .flatMap(config => config.rows || []);

    this.registerGeoMap(styleConfigs);

    const chartDataSet = transformToDataSet(
      dataset.rows,
      dataset.columns,
      dataConfigs,
    );

    return {
      geo: this.getGeoInfo(
        styleConfigs,
        chartDataSet,
        groupConfigs,
        selectedItems,
      ),
      visualMap: this.getVisualMap(
        chartDataSet,
        groupConfigs,
        aggregateConfigs,
        sizeConfigs,
        styleConfigs,
      ),
      series: this.getGeoSeries(
        chartDataSet,
        groupConfigs,
        aggregateConfigs,
        sizeConfigs,
        styleConfigs,
      ).concat(
        this.getMetricAndSizeSeries(
          chartDataSet,
          groupConfigs,
          aggregateConfigs,
          sizeConfigs,
          styleConfigs,
          selectedItems,
        ) as any,
      ),
      tooltip: this.getTooltip(
        chartDataSet,
        groupConfigs,
        aggregateConfigs,
        sizeConfigs,
        infoConfigs,
      ),
      toolbox: this.getToolbox(styleConfigs, context),
    };
  }

  private getToolbox(styles: ChartStyleConfig[], context?: BrokerContext) {
    return {
      show: true,
      orient: 'vertical',
      left: 'left',
      top: 'bottom',
      feature: {
        myResetZoom: {
          show: true,
          title: context?.translator?.('common.reset'),
          onclick: () => {
            this.geoConfig = {
              center: undefined,
              zoom: 1,
            };
            this.chart?.setOption({
              geo: {
                ...this.geoConfig,
              },
            });
          },
          icon: 'path://M943.8 484.1c-17.5-13.7-42.8-10.7-56.6 6.8-5.7 7.3-8.5 15.8-8.6 24.4h-0.4c-0.6 78.3-26.1 157-78 223.3-124.9 159.2-356 187.1-515.2 62.3-31.7-24.9-58.2-54-79.3-85.9h77.1c22.4 0 40.7-18.3 40.7-40.7v-3c0-22.4-18.3-40.7-40.7-40.7H105.5c-22.4 0-40.7 18.3-40.7 40.7v177.3c0 22.4 18.3 40.7 40.7 40.7h3c22.4 0 40.7-18.3 40.7-40.7v-73.1c24.2 33.3 53 63.1 86 89 47.6 37.3 101 64.2 158.9 79.9 55.9 15.2 113.5 19.3 171.2 12.3 57.7-7 112.7-24.7 163.3-52.8 52.5-29 98-67.9 135.3-115.4 37.3-47.6 64.2-101 79.9-158.9 10.2-37.6 15.4-76 15.6-114.6h-0.1c-0.3-11.6-5.5-23.1-15.5-30.9zM918.7 135.2h-3c-22.4 0-40.7 18.3-40.7 40.7V249c-24.2-33.3-53-63.1-86-89-47.6-37.3-101-64.2-158.9-79.9-55.9-15.2-113.5-19.3-171.2-12.3-57.7 7-112.7 24.7-163.3 52.8-52.5 29-98 67.9-135.3 115.4-37.3 47.5-64.2 101-79.9 158.8-10.2 37.6-15.4 76-15.6 114.6h0.1c0.2 11.7 5.5 23.2 15.4 30.9 17.5 13.7 42.8 10.7 56.6-6.8 5.7-7.3 8.5-15.8 8.6-24.4h0.4c0.6-78.3 26.1-157 78-223.3 124.9-159.2 356-187.1 515.2-62.3 31.7 24.9 58.2 54 79.3 85.9h-77.1c-22.4 0-40.7 18.3-40.7 40.7v3c0 22.4 18.3 40.7 40.7 40.7h177.3c22.4 0 40.7-18.3 40.7-40.7V175.8c0.1-22.3-18.2-40.6-40.6-40.6z',
        },
        myZoomIn: {
          show: true,
          title: context?.translator?.('common.zoomIn'),
          onclick: () => {
            this.geoConfig = {
              center: this.geoConfig.center,
              zoom: this.geoConfig.zoom! * 1.25,
            };
            this.chart?.setOption({
              geo: {
                ...this.geoConfig,
              },
            });
          },
          icon: 'path//M796 751.008l124.992 124.992q8.992 10.016 8.992 22.496t-9.504 22.016-22.016 9.504-22.496-8.992l-124.992-124.992q-132.992 108.992-295.008 99.488t-280.992-132.512q-114.016-128.992-111.008-291.008t122.016-286.016q124-119.008 286.016-122.016t291.008 111.008q123.008 119.008 132.512 280.992t-99.488 295.008zM480 832q150.016-4 248.992-103.008T832 480q-4-150.016-103.008-248.992T480 128q-150.016 4-248.992 103.008T128 480q4 150.016 103.008 248.992T480 832z m-32-384v-96q0-14.016 8.992-23.008T480 320t23.008 8.992T512 352v96h96q14.016 0 23.008 8.992T640 480t-8.992 23.008T608 512h-96v96q0 14.016-8.992 23.008T480 640t-23.008-8.992T448 608v-96h-96q-14.016 0-23.008-8.992T320 480t8.992-23.008T352 448h96z',
        },
        myZoomOut: {
          show: true,
          title: context?.translator?.('common.zoomOut'),
          onclick: () => {
            this.geoConfig = {
              center: this.geoConfig.center,
              zoom: this.geoConfig.zoom! / 1.25,
            };
            this.chart?.setOption({
              geo: {
                ...this.geoConfig,
              },
            });
          },
          icon: 'M818.214912 772.134912l128 128c6.144 6.827008 9.216 14.507008 9.216 23.04 0 8.534016-3.241984 16.043008-9.728 22.528-6.484992 6.486016-13.993984 9.728-22.528 9.728-8.532992 0-16.212992-3.072-23.04-9.216l-128-128c-90.793984 74.411008-191.488 108.374016-302.08 101.888-110.592-6.484992-206.505984-51.712-287.744-135.68-77.824-88.064-115.712-187.392-113.664-297.984s43.691008-208.212992 124.928-292.864c84.651008-81.236992 182.272-122.88 292.864-124.928s209.92 35.84 297.984 113.664c83.968 81.238016 129.195008 177.152 135.68 287.744 6.486016 110.592-27.476992 211.286016-101.888 302.08z m-323.584 82.944c102.4-2.729984 187.392-37.888 254.976-105.472s102.742016-152.576 105.472-254.976c-2.729984-102.4-37.888-187.392-105.472-254.976s-152.576-102.740992-254.976-105.472c-102.4 2.731008-187.392 37.888-254.976 105.472s-102.740992 152.576-105.472 254.976c2.731008 102.4 37.888 187.392 105.472 254.976s152.576 102.742016 254.976 105.472z m-131.072-393.216h262.144c9.558016 0 17.408 3.072 23.552 9.216s9.216 13.995008 9.216 23.552c0 9.558016-3.072 17.408-9.216 23.552s-13.993984 9.216-23.552 9.216h-262.144c-9.556992 0-17.408-3.072-23.552-9.216s-9.216-13.993984-9.216-23.552c0-9.556992 3.072-17.408 9.216-23.552s13.995008-9.216 23.552-9.216z',
        },
      },
    };
  }

  private registerGeoMap(styleConfigs: ChartStyleConfig[]) {
    const [mapLevelName] = getStyles(styleConfigs, ['map'], ['level']);
    this.geoMap = mapLevelName === 'china' ? geoChina : geoChinaCity;
  }

  private getGeoInfo(
    styleConfigs: ChartStyleConfig[],
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    selectedItems?: SelectedItem[],
  ): GeoInfo {
    const [show, position, font] = getStyles(
      styleConfigs,
      ['label'],
      ['showLabel', 'position', 'font'],
    );
    const [
      mapLevelName,
      areaColor,
      areaEmphasisColor,
      enableFocus,
      borderStyle,
    ] = getStyles(
      styleConfigs,
      ['map'],
      ['level', 'areaColor', 'areaEmphasisColor', 'focusArea', 'borderStyle'],
    );
    return {
      map: mapLevelName,
      roam: 'move',
      emphasis: {
        focus: enableFocus ? 'self' : 'none',
        itemStyle: {
          areaColor: areaEmphasisColor,
        },
      },
      ...this.geoConfig,
      itemStyle: {
        areaColor: areaColor,
        borderType: borderStyle?.type,
        borderWidth: borderStyle?.width,
        borderColor: borderStyle?.color,
      },
      label: {
        show,
        position,
        ...font,
      },
      labelLayout: { hideOverlap: true },
      regions: chartDataSet?.map((row, dcIndex) => {
        return Object.assign(
          {
            name: this.mappingGeoName(
              row.getCell(groupConfigs[0]),
              styleConfigs,
            ),
          },
          this.isNormalGeoMap
            ? getSelectedItemStyles(0, dcIndex, selectedItems || [])
            : {},
        );
      }),
    };
  }

  protected getGeoSeries(
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    styleConfigs: ChartStyleConfig[],
  ): GeoSeries[] {
    const [show] = getStyles(styleConfigs, ['visualMap'], ['show']);
    const [mapLevelName] = getStyles(styleConfigs, ['map'], ['level']);
    return [
      {
        type: 'map',
        map: mapLevelName,
        geoIndex: 0,
        emphasis: {
          label: {
            show: true,
          },
        },
        select: {
          disabled: true,
        },
        data: chartDataSet
          ?.map(row => {
            return {
              ...getExtraSeriesRowData(row),
              name: this.mappingGeoName(
                row.getCell(groupConfigs[0]),
                styleConfigs,
              ),
              value: row.getCell(aggregateConfigs[0]),
              visualMap: show,
            };
          })
          ?.filter(d => !!d.name && d.value !== undefined),
      },
    ];
  }

  protected getMetricAndSizeSeries(
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    styleConfigs: ChartStyleConfig[],
    selectedItems?: SelectedItem[],
  ): MetricAndSizeSeriesStyle[] {
    if (this.isNormalGeoMap) {
      return [];
    }

    const [showLabel] = getStyles(styleConfigs, ['label'], ['showLabel']);
    const [cycleRatio] = getStyles(styleConfigs, ['map'], ['cycleRatio']);
    const { min, max } = getDataColumnMaxAndMin2(chartDataSet, sizeConfigs[0]);
    const defaultSizeValue = (max - min) / 2;
    const defaultColorValue = 1;

    return [
      {
        type: 'scatter',
        zlevel: 2,
        coordinateSystem: 'geo',
        symbol: 'circle',
        data: chartDataSet
          ?.map((row, dcIndex) => {
            return {
              ...getExtraSeriesRowData(row),
              name: this.mappingGeoName(
                row.getCell(groupConfigs[0]),
                styleConfigs,
              ),
              value: this.mappingGeoCoordination(
                styleConfigs,
                row.getCell(groupConfigs[0]),
                row.getCell(aggregateConfigs[0]) || defaultColorValue,
                row.getCell(sizeConfigs[0]) || defaultSizeValue,
              ),
              ...getSelectedItemStyles(0, dcIndex, selectedItems || []),
            };
          })
          ?.filter(d => !!d.name && d.value !== undefined),
        symbolSize: getScatterSymbolSizeFn(3, max, min, cycleRatio),
        label: {
          formatter: '{b}',
          position: 'right',
          show: showLabel,
        },
        emphasis: {
          label: {
            show: showLabel,
          },
        },
      },
    ];
  }

  protected getTooltip(
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    infoConfigs: ChartDataSectionField[],
  ): {
    trigger: string;
    formatter: (params) => string;
  } {
    return {
      trigger: 'item',
      formatter: function (seriesParams) {
        if (seriesParams.componentType !== 'series') {
          return seriesParams.name;
        }
        return getSeriesTooltips4Polar2(
          chartDataSet,
          seriesParams,
          groupConfigs,
          aggregateConfigs,
          infoConfigs,
          sizeConfigs,
        );
      },
    };
  }

  protected mappingGeoName(sourceName: string, styleConfigs): string {
    const [mapLevelName] = getStyles(styleConfigs, ['map'], ['level']);
    const isProvinceLevel = mapLevelName === 'china';
    const targetName = this.geoMap.features.find(f =>
      isProvinceLevel
        ? f.properties.name?.includes(sourceName)
        : f.properties.name === sourceName,
    )?.properties.name;
    return targetName;
  }

  protected mappingGeoCoordination(
    styleConfigs,
    sourceName: string,
    ...values: Array<number | string>
  ): Array<number[] | number | string> {
    const [mapLevelName] = getStyles(styleConfigs, ['map'], ['level']);
    const isProvinceLevel = mapLevelName === 'china';
    const properties = this.geoMap.features.find(f =>
      isProvinceLevel
        ? f.properties.name?.includes(sourceName)
        : f.properties.name === sourceName,
    )?.properties;

    return (properties?.cp || properties?.center)?.concat(values) || [];
  }

  protected getVisualMap(
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    sizeConfigs: ChartDataSectionField[],
    styleConfigs: ChartStyleConfig[],
  ): GeoVisualMapStyle[] {
    const [show, orient, align, itemWidth, itemHeight, font, position] =
      getStyles(
        styleConfigs,
        ['visualMap'],
        [
          'show',
          'orient',
          'align',
          'itemWidth',
          'itemHeight',
          'font',
          'position',
        ],
      );
    if (!aggregateConfigs?.length) {
      return [];
    }

    const { min, max } = getDataColumnMaxAndMin2(
      chartDataSet,
      aggregateConfigs?.[0],
    );
    const format = aggregateConfigs?.[0]?.format;
    const inRange = {
      color: [
        aggregateConfigs?.[0]?.color?.start || '#1B9AEE',
        aggregateConfigs?.[0]?.color?.end || '#FA8C15',
      ],
    };
    const positionConfig = position?.split(',');
    return [
      {
        type: 'continuous',
        seriesIndex: 0,
        dimension: this.isNormalGeoMap ? undefined : 2,
        show,
        orient,
        align,

        //处理 visualMap position  旧数据中没有 position 数据  beta.2版本之后是 string 类型 后续版本稳定之后 可以移除兼容逻辑
        // TODO migration start
        left: positionConfig?.[0] || 'right',
        top: positionConfig?.[1] || 'bottom',
        // TODO migration end --tl

        itemWidth,
        itemHeight,
        inRange,
        // NOTE 映射最大值和最小值如果一致，会导致map所有区域全部映射成中间颜色，这里做兼容处理
        text: [
          toFormattedValue(max, format),
          toFormattedValue(min !== max ? min : min - 1, format),
        ],
        min: min !== max ? min : min - 1,
        max,
        textStyle: {
          ...font,
        },
        formatter: value => toFormattedValue(value, format),
      },
    ];
  }
}

export default BasicOutlineMapChart;
