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

import { ChartConfig } from 'app/types/ChartConfig';

const config: ChartConfig = {
  datas: [
    {
      label: 'dimension',
      key: 'dimension',
      type: 'group',
      required: true,
      drillable: true,
    },
    {
      label: 'metrics',
      key: 'metrics',
      type: 'aggregate',
      required: true,
      actions: {
        NUMERIC: ['aggregate', 'alias', 'format'],
        STRING: ['aggregateLimit', 'alias', 'format'],
      },
      limit: 2,
    },
    {
      label: 'colorize',
      key: 'color',
      type: 'color',
    },
    {
      label: 'size',
      key: 'size',
      type: 'size',
    },
    {
      label: 'filter',
      key: 'filter',
      type: 'filter',
    },
    {
      label: 'info',
      key: 'info',
      type: 'info',
    },
  ],
  styles: [
    {
      label: 'scatter.title',
      key: 'scatter',
      comType: 'group',
      rows: [
        {
          label: 'scatter.cycleRatio',
          key: 'cycleRatio',
          comType: 'slider',
          default: 1,
        },
      ],
    },
    {
      label: 'label.title',
      key: 'label',
      comType: 'group',
      rows: [
        {
          label: 'label.showLabel',
          key: 'showLabel',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'label.position',
          key: 'position',
          comType: 'labelPosition',
          default: 'top',
        },
        {
          label: 'viz.palette.style.font',
          key: 'font',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
      ],
    },
    {
      label: 'legend.title',
      key: 'legend',
      comType: 'group',
      rows: [
        {
          label: 'legend.showLegend',
          key: 'showLegend',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'legend.type',
          key: 'type',
          comType: 'legendType',
          default: 'scroll',
        },
        {
          label: 'legend.selectAll',
          key: 'selectAll',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'legend.position',
          key: 'position',
          comType: 'legendPosition',
          default: 'right',
        },
        {
          label: 'legend.height',
          key: 'height',
          default: 0,
          comType: 'inputNumber',
          options: {
            step: 40,
            min: 0,
          },
        },
        {
          label: 'viz.palette.style.font',
          key: 'font',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
      ],
    },
    {
      label: 'xAxis.title',
      key: 'xAxis',
      comType: 'group',
      rows: [
        {
          label: 'common.showAxis',
          key: 'showAxis',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'common.inverseAxis',
          key: 'inverseAxis',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'common.lineStyle',
          key: 'lineStyle',
          comType: 'line',
          default: {
            type: 'solid',
            width: 1,
            color: '#ced4da',
          },
        },
        {
          label: 'common.showLabel',
          key: 'showLabel',
          default: true,
          comType: 'checkbox',
          options: [],
        },
        {
          label: 'viz.palette.style.font',
          key: 'font',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
        {
          label: 'common.showTitleAndUnit',
          key: 'showTitleAndUnit',
          default: true,
          comType: 'checkbox',
          options: [],
        },
        {
          label: 'common.unitFont',
          key: 'unitFont',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
        {
          label: 'common.nameLocation',
          key: 'nameLocation',
          default: 'center',
          comType: 'nameLocation',
        },
        {
          label: 'common.nameRotate',
          key: 'nameRotate',
          default: 0,
          comType: 'inputNumber',
        },
        {
          label: 'common.nameGap',
          key: 'nameGap',
          default: 20,
          comType: 'inputNumber',
        },
        {
          label: 'common.min',
          key: 'min',
          comType: 'inputNumber',
        },
        {
          label: 'common.max',
          key: 'max',
          comType: 'inputNumber',
        },
        {
          label: 'yAxis.open',
          key: 'modal',
          comType: 'group',
          options: {
            type: 'modal',
            modalSize: 'middle',
            flatten: true,
            title: 'yAxis.numberFormat',
          },
          rows: [
            {
              label: 'yAxis.open',
              key: 'YAxisNumberFormat',
              comType: 'YAxisNumberFormatPanel',
            },
          ],
        },
      ],
    },
    {
      label: 'yAxis.title',
      key: 'yAxis',
      comType: 'group',
      rows: [
        {
          label: 'common.showAxis',
          key: 'showAxis',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'common.inverseAxis',
          key: 'inverseAxis',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'common.lineStyle',
          key: 'lineStyle',
          comType: 'line',
          default: {
            type: 'solid',
            width: 1,
            color: '#ced4da',
          },
        },
        {
          label: 'common.showLabel',
          key: 'showLabel',
          default: true,
          comType: 'checkbox',
          options: [],
        },
        {
          label: 'viz.palette.style.font',
          key: 'font',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
        {
          label: 'common.showTitleAndUnit',
          key: 'showTitleAndUnit',
          default: true,
          comType: 'checkbox',
          options: [],
        },
        {
          label: 'common.unitFont',
          key: 'unitFont',
          comType: 'font',
          default: {
            fontFamily: 'PingFang SC',
            fontSize: '12',
            fontWeight: 'normal',
            fontStyle: 'normal',
            color: '#495057',
          },
        },
        {
          label: 'common.nameLocation',
          key: 'nameLocation',
          default: 'center',
          comType: 'nameLocation',
        },
        {
          label: 'common.nameRotate',
          key: 'nameRotate',
          default: 90,
          comType: 'inputNumber',
        },
        {
          label: 'common.nameGap',
          key: 'nameGap',
          default: 20,
          comType: 'inputNumber',
        },
        {
          label: 'common.min',
          key: 'min',
          comType: 'inputNumber',
        },
        {
          label: 'common.max',
          key: 'max',
          comType: 'inputNumber',
        },
        {
          label: 'yAxis.open',
          key: 'modal',
          comType: 'group',
          options: {
            type: 'modal',
            modalSize: 'middle',
            flatten: true,
            title: 'yAxis.numberFormat',
          },
          rows: [
            {
              label: 'yAxis.open',
              key: 'YAxisNumberFormat',
              comType: 'YAxisNumberFormatPanel',
            },
          ],
        },
      ],
    },
    {
      label: 'splitLine.title',
      key: 'splitLine',
      comType: 'group',
      rows: [
        {
          label: 'splitLine.showHorizonLine',
          key: 'showHorizonLine',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'common.lineStyle',
          key: 'horizonLineStyle',
          comType: 'line',
          default: {
            type: 'dashed',
            width: 1,
            color: '#ced4da',
          },
        },
        {
          label: 'splitLine.showVerticalLine',
          key: 'showVerticalLine',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'common.lineStyle',
          key: 'verticalLineStyle',
          comType: 'line',
          default: {
            type: 'dashed',
            width: 1,
            color: '#ced4da',
          },
        },
      ],
    },
    {
      label: 'viz.palette.style.margin.title',
      key: 'margin',
      comType: 'group',
      rows: [
        {
          label: 'viz.palette.style.margin.containLabel',
          key: 'containLabel',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'viz.palette.style.margin.left',
          key: 'marginLeft',
          default: '5%',
          comType: 'marginWidth',
        },
        {
          label: 'viz.palette.style.margin.right',
          key: 'marginRight',
          default: '5%',
          comType: 'marginWidth',
        },
        {
          label: 'viz.palette.style.margin.top',
          key: 'marginTop',
          default: '5%',
          comType: 'marginWidth',
        },
        {
          label: 'viz.palette.style.margin.bottom',
          key: 'marginBottom',
          default: '5%',
          comType: 'marginWidth',
        },
      ],
    },
  ],
  settings: [
    {
      label: 'viz.palette.setting.paging.title',
      key: 'paging',
      comType: 'group',
      rows: [
        {
          label: 'viz.palette.setting.paging.pageSize',
          key: 'pageSize',
          default: 1000,
          comType: 'inputNumber',
          options: {
            needRefresh: true,
            step: 1,
            min: 0,
          },
        },
      ],
    },
    {
      label: 'reference.title',
      key: 'reference',
      comType: 'group',
      rows: [
        {
          label: 'reference.open',
          key: 'panel',
          comType: 'reference',
          options: { type: 'modal' },
        },
      ],
    },
  ],
  interactions: [
    {
      label: 'drillThrough.title',
      key: 'drillThrough',
      comType: 'checkboxModal',
      default: false,
      options: { modalSize: 'middle' },
      rows: [
        {
          label: 'drillThrough.title',
          key: 'setting',
          comType: 'interaction.drillThrough',
        },
      ],
    },
    {
      label: 'viewDetail.title',
      key: 'viewDetail',
      comType: 'checkboxModal',
      default: false,
      options: { modalSize: 'middle' },
      rows: [
        {
          label: 'viewDetail.title',
          key: 'setting',
          comType: 'interaction.viewDetail',
        },
      ],
    },
  ],
  i18ns: [
    {
      lang: 'zh-CN',
      translation: {
        common: {
          showAxis: '显示坐标轴',
          inverseAxis: '反转坐标轴',
          lineStyle: '线条样式',
          borderType: '边框线条类型',
          borderWidth: '边框线条宽度',
          borderColor: '边框线条颜色',
          backgroundColor: '背景颜色',
          showLabel: '显示标签',
          rotate: '旋转角度',
          position: '位置',
          unitFont: '刻度字体',
          showInterval: '显示刻度',
          interval: '刻度间隔',
          showTitleAndUnit: '显示标题和刻度',
          nameLocation: '标题位置',
          nameRotate: '标题旋转',
          nameGap: '标题与轴线距离',
          min: '最小值',
          max: '最大值',
        },
        label: {
          title: '标签',
          showLabel: '显示标签',
          position: '位置',
        },
        legend: {
          title: '图例',
          showLegend: '显示图例',
          type: '图例类型',
          selectAll: '图例全选',
          position: '图例位置',
          height: '图例高度',
        },
        data: {
          color: '颜色',
          colorize: '配色',
        },
        xAxis: {
          title: 'X轴',
        },
        yAxis: {
          title: 'Y轴',
          numberFormat: '数据格式设置',
          open: '打开',
        },
        splitLine: {
          title: '分割线',
          showHorizonLine: '显示横向分割线',
          showVerticalLine: '显示纵向分割线',
        },
        reference: {
          title: '参考线',
          open: '点击参考线配置',
        },
        scatter: {
          title: '散点图配置',
          cycleRatio: '气泡大像素比',
        },
      },
    },
    {
      lang: 'en-US',
      translation: {
        common: {
          showAxis: 'Show Axis',
          inverseAxis: 'Inverse Axis',
          lineStyle: 'Line Style',
          borderType: 'Border Type',
          borderWidth: 'Border Width',
          borderColor: 'Border Color',
          backgroundColor: 'Background Color',
          showLabel: 'Show Label',
          unitFont: 'Unit Font',
          rotate: 'Rotate',
          position: 'Position',
          showInterval: 'Show Interval',
          interval: 'Interval',
          showTitleAndUnit: 'Show Title and Unit',
          nameLocation: 'Name Location',
          nameRotate: 'Name Rotate',
          nameGap: 'Name Gap',
          min: 'Min',
          max: 'Max',
        },
        label: {
          title: 'Label',
          showLabel: 'Show Label',
          position: 'Position',
          height: 'Height',
        },
        legend: {
          title: 'Legend',
          showLegend: 'Show Legend',
          type: 'Type',
          selectAll: 'Select All',
          position: 'Position',
          height: 'Height',
        },
        data: {
          color: 'Color',
          colorize: 'Colorize',
        },
        xAxis: {
          title: 'X Axis',
        },
        yAxis: {
          title: 'Y Axis',
          numberFormat: 'Number Format',
          open: 'Open',
        },
        splitLine: {
          title: 'Split Line',
          showHorizonLine: 'Show Horizontal Line',
          showVerticalLine: 'Show Vertical Line',
        },
        reference: {
          title: 'Reference',
          open: 'Open',
        },
        scatter: {
          title: 'Scatter',
          cycleRatio: 'Cycle Ratio',
        },
      },
    },
  ],
};

export default config;
