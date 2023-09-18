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
      limit: [0, 1],
      actions: {
        NUMERIC: ['alias', 'colorize', 'sortable'],
        STRING: ['alias', 'colorize', 'sortable'],
        DATE: ['alias', 'colorize', 'sortable'],
      },
      drillable: true,
    },
    {
      label: 'metrics',
      key: 'metrics',
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
      label: 'funnel.title',
      key: 'funnel',
      comType: 'group',
      rows: [
        {
          label: 'funnel.sort',
          key: 'sort',
          comType: 'select',
          default: 'descending',
          options: {
            translateItemLabel: true,
            items: [
              {
                label: '@global@.funnel.sortType.descending',
                value: 'descending',
              },
              {
                label: '@global@.funnel.sortType.ascending',
                value: 'ascending',
              },
              { label: '@global@.funnel.sortType.none', value: 'none' },
            ],
          },
        },
        {
          label: 'funnel.align',
          key: 'align',
          default: 'center',
          comType: 'select',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.funnel.alignType.center', value: 'center' },
              { label: '@global@.funnel.alignType.left', value: 'left' },
              { label: '@global@.funnel.alignType.right', value: 'right' },
            ],
          },
        },
        {
          label: 'funnel.gap',
          key: 'gap',
          default: 1,
          comType: 'inputNumber',
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
          comType: 'select',
          default: 'inside',
          options: {
            translateItemLabel: true,
            items: [
              { label: 'viz.palette.style.position.left', value: 'left' },
              { label: 'viz.palette.style.position.right', value: 'right' },
              { label: 'viz.palette.style.position.inside', value: 'inside' },
            ],
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
        {
          label: 'label.metric',
          key: 'metric',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'label.conversion',
          key: 'conversion',
          comType: 'checkbox',
        },
        {
          label: 'label.arrival',
          key: 'arrival',
          comType: 'checkbox',
        },
        {
          label: 'label.percentage',
          key: 'percentage',
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
          default: false,
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
        section: {
          legend: '图例',
          detail: '详细信息',
          info: '提示信息',
        },
        label: {
          title: '标签',
          showLabel: '显示标签',
          position: '位置',
          metric: '指标',
          dimension: '维度',
          conversion: '转化率',
          arrival: '到达率',
          percentage: '百分比',
        },
        legend: {
          title: '图例',
          showLegend: '显示图例',
          type: '图例类型',
          selectAll: '图例全选',
          position: '图例位置',
          height: '图例高度',
        },
        funnel: {
          title: '漏斗图',
          sort: '排序',
          align: '对齐',
          gap: '间距',
          sortType: {
            descending: '降序',
            ascending: '升序',
            none: '无',
          },
          alignType: {
            center: '居中',
            left: '居左',
            right: '居右',
          },
        },
        data: {
          color: '颜色',
          colorize: '配色',
        },
      },
    },
    {
      lang: 'en-US',
      translation: {
        section: {
          legend: 'Legend',
          detail: 'Detail',
          info: 'Info',
        },
        label: {
          title: 'Title',
          showLabel: 'Show Label',
          position: 'Position',
          metric: 'Metric',
          dimension: 'Dimension',
          conversion: 'Conversion',
          arrival: 'Arrival',
          percentage: 'Percentage',
        },
        legend: {
          title: 'Legend',
          showLegend: 'Show Legend',
          type: 'Type',
          selectAll: 'Select All',
          position: 'Position',
          height: 'Height',
        },
        funnel: {
          title: 'Funnel',
          sort: 'Sort',
          align: 'Alignment',
          gap: 'Gap',
          sortType: {
            descending: 'Descending',
            ascending: 'Ascending',
            none: 'None',
          },
          alignType: {
            center: 'Center',
            left: 'Left',
            right: 'Right',
          },
        },
        data: {
          color: 'Color',
          colorize: 'Colorize',
        },
      },
    },
  ],
};

export default config;
