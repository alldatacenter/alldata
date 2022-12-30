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
import { FONT_FAMILY } from 'styles/StyleConstants';

const config: ChartConfig = {
  datas: [
    {
      label: 'dimension',
      key: 'dimension',
      required: true,
      type: 'group',
      limit: 1,
      actions: {
        NUMERIC: ['sortable'],
        STRING: ['sortable'],
      },
    },
    {
      label: 'metrics',
      key: 'metrics',
      required: true,
      type: 'aggregate',
      limit: 1,
      actions: {
        NUMERIC: ['sortable', 'aggregate'],
        STRING: ['sortable', 'aggregate'],
      },
    },
    {
      label: 'filter',
      key: 'filter',
      type: 'filter',
      allowSameField: true,
    },
  ],
  styles: [
    {
      label: 'wordCloud.title',
      key: 'wordCloud',
      comType: 'group',
      rows: [
        {
          label: 'wordCloud.shape',
          key: 'shape',
          comType: 'select',
          default: 'circle',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.shapeType.circle', value: 'circle' },
              { label: '@global@.shapeType.cardioid', value: 'cardioid' },
              { label: '@global@.shapeType.diamond', value: 'diamond' },
              {
                label: '@global@.shapeType.triangleForward',
                value: 'triangle-forward',
              },
              { label: '@global@.shapeType.triangle', value: 'triangle' },
              { label: '@global@.shapeType.pentagon', value: 'pentagon' },
              { label: '@global@.shapeType.star', value: 'star' },
            ],
          },
        },
        {
          label: 'wordCloud.width',
          key: 'width',
          default: '80%',
          comType: 'marginWidth',
        },
        {
          label: 'wordCloud.height',
          key: 'height',
          default: '80%',
          comType: 'marginWidth',
        },
        {
          label: 'wordCloud.drawOutOfBound',
          key: 'drawOutOfBound',
          default: true,
          comType: 'checkbox',
        },
      ],
    },
    {
      label: 'label.title',
      key: 'label',
      comType: 'group',
      rows: [
        {
          label: 'label.fontFamily',
          key: 'fontFamily',
          comType: 'fontFamily',
          default: FONT_FAMILY,
        },
        {
          label: 'label.fontWeight',
          key: 'fontWeight',
          comType: 'fontWeight',
          default: 'normal',
        },
        {
          label: 'label.maxFontSize',
          key: 'maxFontSize',
          default: 72,
          options: {
            min: 0,
          },
          comType: 'inputNumber',
        },
        {
          label: 'label.minFontSize',
          key: 'minFontSize',
          default: 12,
          options: {
            min: 0,
          },
          comType: 'inputNumber',
        },
        {
          label: 'label.rotationRangeStart',
          key: 'rotationRangeStart',
          default: 0,
          comType: 'inputNumber',
        },
        {
          label: 'label.rotationRangeEnd',
          key: 'rotationRangeEnd',
          default: 0,
          comType: 'inputNumber',
        },
        {
          label: 'label.rotationStep',
          key: 'rotationStep',
          default: 0,
          options: {
            min: 0,
          },
          comType: 'inputNumber',
        },
        {
          label: 'label.gridSize',
          key: 'gridSize',
          default: 8,
          options: {
            min: 0,
          },
          comType: 'inputNumber',
        },
        {
          label: 'label.focus',
          key: 'focus',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'label.textShadowBlur',
          key: 'textShadowBlur',
          default: 10,
          options: {
            min: 0,
          },
          comType: 'inputNumber',
        },
        {
          label: 'label.textShadowColor',
          key: 'textShadowColor',
          default: '#333',
          comType: 'fontColor',
        },
      ],
    },
    {
      label: 'viz.palette.style.margin.title',
      key: 'margin',
      comType: 'group',
      rows: [
        {
          label: 'viz.palette.style.margin.left',
          key: 'marginLeft',
          default: '10%',
          comType: 'marginWidth',
        },
        {
          label: 'viz.palette.style.margin.top',
          key: 'marginTop',
          default: '10%',
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
        wordCloud: {
          title: '词云配置',
          shape: '词云形状',
          drawOutOfBound: '限制边界',
          width: '宽度',
          height: '高度',
        },
        label: {
          title: '标签',
          fontFamily: '字体',
          fontWeight: '字体粗细',
          maxFontSize: '字体最大值',
          minFontSize: '字体最小值',
          rotationRangeStart: '起始旋转角度',
          rotationRangeEnd: '结束旋转角度',
          rotationStep: '旋转步长',
          gridSize: '文字间隔',
          focus: '是否淡出',
          textShadowBlur: '阴影长度',
          textShadowColor: '阴影颜色',
        },
        shapeType: {
          circle: '圆形',
          cardioid: '心形',
          diamond: '菱形',
          triangleForward: '正三角形',
          triangle: '三角形',
          pentagon: '五边形',
          star: '星形',
        },
      },
    },
    {
      lang: 'en-US',
      translation: {
        wordCloud: {
          title: 'Word Cloud',
          shape: 'Shape',
          drawOutOfBound: 'Boundary',
          width: 'Width',
          height: 'Height',
        },
        label: {
          title: 'Label',
          fontFamily: 'Font Family',
          fontWeight: 'Font Weight',
          maxFontSize: 'Max Font Size',
          minFontSize: 'Min Font Size',
          rotationRangeStart: 'Start Rotation Range',
          rotationRangeEnd: 'End Rotation Range',
          rotationStep: 'Rotation Step',
          gridSize: 'Grid Size',
          focus: 'Focus',
          textShadowBlur: 'Text Shadow Blur',
          textShadowColor: 'Text Shadow Color',
        },
        shapeType: {
          circle: 'Circle',
          cardioid: 'Cardioid',
          diamond: 'Diamond',
          triangleForward: 'Triangle Forward',
          triangle: 'Triangle',
          pentagon: 'Pentagon',
          star: 'Star',
        },
      },
    },
  ],
};

export default config;
