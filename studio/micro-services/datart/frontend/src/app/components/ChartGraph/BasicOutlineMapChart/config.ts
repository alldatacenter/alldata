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
      label: 'metrics',
      key: 'metrics',
      required: true,
      type: 'aggregate',
      actions: {
        NUMERIC: ['aggregate', 'alias', 'format', 'colorRange'],
        STRING: ['aggregate', 'alias', 'format', 'colorRange'],
      },
      limit: 1,
    },
    {
      label: 'filter',
      key: 'filter',
      type: 'filter',
      allowSameField: true,
    },
    {
      label: 'size',
      key: 'size',
      type: 'size',
    },
  ],
  styles: [
    {
      label: 'map.title',
      key: 'map',
      comType: 'group',
      rows: [
        {
          label: 'map.level',
          key: 'level',
          comType: 'select',
          default: 'china',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.levelType.china', value: 'china' },
              { label: '@global@.levelType.chinaCity', value: 'china-city' },
            ],
          },
        },
        {
          label: 'map.areaColor',
          key: 'areaColor',
          default: '#e9ecef',
          comType: 'fontColor',
        },
        {
          label: 'map.focusArea',
          key: 'focusArea',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'map.areaEmphasisColor',
          key: 'areaEmphasisColor',
          default: '#dee2e6',
          comType: 'fontColor',
        },
        {
          label: 'map.borderStyle',
          key: 'borderStyle',
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
          label: 'viz.palette.style.position.title',
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
      label: 'viz.palette.style.visualMap.title',
      key: 'visualMap',
      comType: 'group',
      rows: [
        {
          label: 'viz.palette.style.visualMap.show',
          key: 'show',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'viz.palette.style.visualMap.orient',
          key: 'orient',
          comType: 'select',
          default: 'vertical',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.orientType.vertical', value: 'vertical' },
              { label: '@global@.orientType.horizontal', value: 'horizontal' },
            ],
          },
        },
        {
          label: 'viz.palette.style.visualMap.align',
          key: 'align',
          comType: 'select',
          default: 'auto',
          options: {
            translateItemLabel: true,
            items: [
              { label: 'viz.palette.style.position.auto', value: 'auto' },
              { label: 'viz.palette.style.position.right', value: 'right' },
              { label: 'viz.palette.style.position.top', value: 'top' },
              { label: 'viz.palette.style.position.bottom', value: 'bottom' },
              { label: 'viz.palette.style.position.left', value: 'left' },
            ],
          },
        },
        {
          label: 'viz.palette.style.position.title',
          key: 'position',
          comType: 'select',
          default: 'right,bottom',
          options: {
            translateItemLabel: true,
            items: [
              {
                label: 'viz.palette.style.position.leftTop',
                value: 'left,top',
              },
              {
                label: 'viz.palette.style.position.rightTop',
                value: 'right,top',
              },
              {
                label: 'viz.palette.style.position.rightBottom',
                value: 'right,bottom',
              },
            ],
          },
        },
        {
          label: 'viz.palette.style.visualMap.itemWidth',
          key: 'itemWidth',
          default: 20,
          comType: 'inputNumber',
        },
        {
          label: 'viz.palette.style.visualMap.itemHeight',
          key: 'itemHeight',
          default: 140,
          comType: 'inputNumber',
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
          unitFont: '刻度字体',
          rotate: '旋转角度',
          showInterval: '显示刻度',
          interval: '刻度间隔',
          showTitleAndUnit: '显示标题和刻度',
          nameLocation: '标题位置',
          nameRotate: '标题旋转',
          nameGap: '标题与轴线距离',
          min: '最小值',
          max: '最大值',
          reset: '重置',
          zoomOut: '缩小',
          zoomIn: '放大',
        },
        metricsAndColor: '指标(颜色)',
        label: {
          title: '标签',
          showLabel: '显示标签',
        },
        levelType: {
          china: '中国-省级地图',
          chinaCity: '中国-地市级地图',
        },
        orientType: {
          horizontal: '水平',
          vertical: '竖直',
        },
        map: {
          title: '地图设置',
          level: '默认地图等级',
          backgroundColor: '底图背景色',
          borderStyle: '轮廓样式',
          focusArea: '聚焦选中区域',
          areaColor: '区域颜色',
          areaEmphasisColor: '选中区域高亮颜色',
        },
        background: { title: '背景设置' },
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
          showInterval: 'Show Interval',
          interval: 'Interval',
          showTitleAndUnit: 'Show Title and Unit',
          nameLocation: 'Name Location',
          nameRotate: 'Name Rotate',
          nameGap: 'Name Gap',
          min: 'Min',
          max: 'Max',
          reset: 'Reset',
          zoomOut: 'Zoom Out',
          zoomIn: 'Zoom In',
        },
        metricsAndColor: 'Metrics and Color',
        label: {
          title: 'Label',
          showLabel: 'Show Label',
          height: 'Height',
        },
        levelType: {
          china: 'China',
          chinaCity: 'China City',
        },
        orientType: {
          horizontal: 'Horizontal',
          vertical: 'Vertical',
        },
        map: {
          title: 'Map',
          level: 'Level',
          backgroundColor: 'Background Color',
          borderStyle: 'Border Style',
          focusArea: 'Focus Area',
          areaColor: 'Area Color',
          areaEmphasisColor: 'Area Emphasis Color',
        },
        background: { title: 'Background' },
      },
    },
  ],
};

export default config;
