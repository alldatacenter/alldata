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
      required: true,
      type: 'group',
      limit: 1,
    },
    {
      label: 'axis.y.left',
      key: 'metricsL',
      required: true,
      type: 'aggregate',
      limit: [1, 999],
    },
    {
      label: 'axis.y.right',
      key: 'metricsR',
      required: true,
      type: 'aggregate',
      limit: [1, 999],
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
      label: 'graph.title',
      key: 'graph',
      comType: 'group',
      rows: [
        {
          label: 'graph.smooth',
          key: 'smooth',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'graph.step',
          key: 'step',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'graph.symbol',
          key: 'symbol',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'graph.label',
          key: 'label',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'graph.stack',
          key: 'stack',
          default: false,
          comType: 'checkbox',
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
      label: 'label.leftYTitle',
      key: 'leftYLabel',
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
      label: 'label.rightYTitle',
      key: 'rightYLabel',
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
      label: 'leftY.title',
      key: 'leftY',
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
          comType: 'checkbox',
        },
        {
          label: 'leftY.title',
          key: 'graphType',
          default: 'bar',
          comType: 'select',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.graphType.line', value: 'line' },
              { label: '@global@.graphType.bar', value: 'bar' },
            ],
          },
        },
        {
          label: 'common.graphStyle',
          key: 'graphStyle',
          comType: 'line',
          default: {
            type: 'solid',
            width: 1,
            color: null,
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
      label: 'rightY.title',
      key: 'rightY',
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
          comType: 'checkbox',
        },
        {
          label: 'rightY.title',
          key: 'graphType',
          default: 'line',
          comType: 'select',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.graphType.line', value: 'line' },
              { label: '@global@.graphType.bar', value: 'bar' },
            ],
          },
        },
        {
          label: 'common.graphStyle',
          key: 'graphStyle',
          comType: 'line',
          default: {
            type: 'solid',
            width: 1,
            color: null,
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
          comType: 'checkbox',
        },
        {
          label: 'common.lineStyle',
          key: 'lineStyle',
          comType: 'line',
          default: {
            type: 'dashed',
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
          label: 'common.rotate',
          key: 'rotate',
          default: 0,
          comType: 'inputNumber',
        },
        {
          label: 'common.showInterval',
          key: 'showInterval',
          default: false,
          comType: 'checkbox',
        },
        {
          label: 'common.overflow',
          key: 'overflow',
          comType: 'select',
          default: 'break',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.common.overflowType.none', value: 'none' },
              {
                label: '@global@.common.overflowType.truncate',
                value: 'truncate',
              },
              { label: '@global@.common.overflowType.break', value: 'break' },
              {
                label: '@global@.common.overflowType.breakAll',
                value: 'breakAll',
              },
            ],
          },
        },
        {
          label: 'common.interval',
          key: 'interval',
          default: 0,
          comType: 'inputNumber',
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
        chartName: '双Y轴图',
        common: {
          showAxis: '显示坐标轴',
          inverseAxis: '反转坐标轴',
          graphStyle: '图形样式',
          lineStyle: '线条样式',
          borderType: '边框线条类型',
          borderWidth: '边框线条宽度',
          borderColor: '边框线条颜色',
          backgroundColor: '背景颜色',
          showLabel: '显示标签',
          fontFamily: '字体',
          fontSize: '字号',
          fontColor: '字体颜色',
          rotate: '旋转角度',
          position: '位置',
          showInterval: '显示刻度',
          interval: '刻度间隔',
          overflow: '文本溢出',
          showTitleAndUnit: '显示标题和刻度',
          nameLocation: '标题位置',
          nameRotate: '标题旋转',
          nameGap: '标题与轴线距离',
          min: '最小值',
          max: '最大值',
          overflowType: {
            none: '溢出',
            truncate: '截断',
            break: '换行',
            breakAll: '强制换行',
          },
        },
        graph: {
          title: '图形设置',
          smooth: '平滑',
          step: '阶梯',
          symbol: '标记',
          label: '数值',
          stack: '堆叠',
        },
        label: {
          leftYTitle: '左轴标签',
          rightYTitle: '右轴标签',
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
        graphType: {
          line: '折线图',
          bar: '柱状图',
        },
        leftY: {
          graph: '显示图形',
          title: '左Y轴',
        },
        rightY: {
          graph: '显示图形',
          title: '右Y轴',
        },
        yAxis: {
          numberFormat: '数据格式设置',
          open: '打开',
        },
        xAxis: {
          title: 'X轴',
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
      },
    },
    {
      lang: 'en-US',
      translation: {
        chartName: 'Double Y Chart',
        common: {
          showAxis: 'Show Axis',
          inverseAxis: 'Inverse Axis',
          graphStyle: 'Graph Style',
          lineStyle: 'Line Style',
          borderType: 'Border Style',
          borderWidth: 'Border Width',
          borderColor: 'Border Color',
          backgroundColor: 'Background Color',
          showLabel: 'Show Label',
          fontFamily: 'Font Family',
          fontSize: 'Font Size',
          fontColor: 'Font Color',
          rotate: 'Rotate',
          position: 'Position',
          showInterval: 'Show Interval',
          interval: 'Interval',
          overflow: 'Overflow',
          showTitleAndUnit: 'Show Title and Unit',
          nameLocation: 'Name Location',
          nameRotate: 'Name Rotate',
          nameGap: 'Name Gap',
          min: 'Min',
          max: 'Max',
          overflowType: {
            none: 'None',
            truncate: 'Truncate',
            break: 'Break',
            breakAll: 'BreakAll',
          },
        },
        graph: {
          title: 'Graph Setting',
          smooth: 'Smooth',
          step: 'Step',
          symbol: 'Symbol',
          label: 'Label',
          stack: 'Stack',
        },
        label: {
          leftYTitle: 'Left Y Axis Label',
          rightYTitle: 'Right Y Axis Label',
          showLabel: 'Show Label',
          position: 'Position',
        },
        legend: {
          title: 'Legend',
          showLegend: 'Show Legend',
          type: 'Type',
          selectAll: 'Select All',
          position: 'Position',
          height: 'Height',
        },
        leftY: {
          graph: 'Show Graph',
          title: 'Left Y Axis',
        },
        rightY: {
          graph: 'Show Graph',
          title: 'Right Y Axis',
        },
        yAxis: {
          numberFormat: 'Number Format',
          open: 'Open',
        },
        graphType: {
          line: 'Line',
          bar: 'Bar',
        },
        xAxis: {
          title: 'X Axis',
        },
        splitLine: {
          title: 'SplitLine',
          showHorizonLine: 'Show Horizon Line',
          showVerticalLine: 'Show Vertical Line',
        },
        reference: {
          title: 'Reference Line',
          open: 'Open',
        },
      },
    },
  ],
};

export default config;
