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
  getDefaultThemeColor,
  getExtraSeriesRowData,
  getSelectedItemStyles,
  getStyles,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { init } from 'echarts';
import 'echarts-wordcloud';
import Config from './config';
import { WordCloudConfig, WordCloudLabelConfig } from './types';

// NOTE: wordcloud chart is echarts extension, more detail please check https://github.com/ecomfe/echarts-wordcloud
class WordCloudChart extends Chart {
  chart: any = null;
  config = Config;
  dependency = [];
  selectionManager?: ChartSelectionManager;

  constructor(props?) {
    super(
      props?.id || 'word-cloud',
      props?.name || 'viz.palette.graph.names.wordCloudChart',
      props?.icon || 'fsux_tubiao_ciyun',
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
    this.chart?.clear();
    this.chart?.resize(context);
    const newOptions = this.getOptions(
      options.dataset!,
      options.config!,
      options.selectedItems,
    );
    this.chart?.setOption(Object.assign({}, newOptions), true);
  }

  getOptions(
    dataset: ChartDataSetDTO,
    config: ChartConfig,
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
    const wordCloud = this.getWordCloud(styleConfigs);
    const label = this.getLabel(
      styleConfigs,
      chartDataSet,
      groupConfigs,
      aggregateConfigs,
      selectedItems,
    );

    return {
      series: [
        {
          type: 'wordCloud',
          layoutAnimation: false,
          ...wordCloud,
          ...label,
        },
      ],
    };
  }

  getWordCloud(style: ChartStyleConfig[]): WordCloudConfig {
    const [drawOutOfBound, shape, width, height] = getStyles(
      style,
      ['wordCloud'],
      ['drawOutOfBound', 'shape', 'width', 'height'],
    );
    const [left, top] = getStyles(
      style,
      ['margin'],
      ['marginLeft', 'marginTop'],
    );
    return {
      drawOutOfBound: !drawOutOfBound,
      shape,
      width,
      height,
      left,
      top,
      right: 'auto',
      bottom: 'auto',
    };
  }

  getLabel(
    style: ChartStyleConfig[],
    chartDataSet: IChartDataSet<string>,
    groupConfigs: ChartDataSectionField[],
    aggregateConfigs: ChartDataSectionField[],
    selectedItems?: SelectedItem[],
  ): WordCloudLabelConfig {
    const [
      fontFamily,
      fontWeight,
      maxFontSize,
      minFontSize,
      rotationRangeStart,
      rotationRangeEnd,
      rotationStep,
      gridSize,
      focus,
      textShadowBlur,
      textShadowColor,
    ] = getStyles(
      style,
      ['label'],
      [
        'fontFamily',
        'fontWeight',
        'maxFontSize',
        'minFontSize',
        'rotationRangeStart',
        'rotationRangeEnd',
        'rotationStep',
        'gridSize',
        'focus',
        'textShadowBlur',
        'textShadowColor',
      ],
    );
    return {
      sizeRange: [minFontSize, maxFontSize],
      rotationRange: [rotationRangeStart, rotationRangeEnd],
      rotationStep,
      gridSize,
      emphasis: {
        focus: focus ? 'self' : 'none',
        textStyle: {
          textShadowBlur,
          textShadowColor,
        },
      },
      data: chartDataSet?.map((dc, dIndex) => {
        return {
          name: dc.getCell(groupConfigs[0]),
          value: dc.getCell(aggregateConfigs[0]),
          ...getExtraSeriesRowData(dc),
          textStyle: getSelectedItemStyles(0, dIndex, selectedItems || [], {
            fontFamily,
            fontWeight,
            color:
              getDefaultThemeColor()[
                dIndex % (getDefaultThemeColor().length - 1)
              ],
          }).itemStyle,
        };
      }),
    };
  }
}

export default WordCloudChart;
