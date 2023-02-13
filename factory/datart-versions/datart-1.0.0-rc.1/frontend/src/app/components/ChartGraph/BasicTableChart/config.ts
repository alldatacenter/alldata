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
      label: 'mixed',
      key: 'mixed',
      required: true,
      type: 'mixed',
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
      label: 'header.title',
      key: 'header',
      comType: 'group',
      rows: [
        {
          label: 'header.open',
          key: 'modal',
          comType: 'group',
          options: { type: 'modal', modalSize: 'middle' },
          rows: [
            {
              label: 'header.styleAndGroup',
              key: 'tableHeaders',
              comType: 'tableHeader',
            },
          ],
        },
      ],
    },
    {
      label: 'column.conditionalStyle',
      key: 'column',
      comType: 'group',
      rows: [
        {
          label: 'column.open',
          key: 'modal',
          comType: 'group',
          options: { type: 'modal', modalSize: 'middle' },
          rows: [
            {
              label: 'column.list',
              key: 'list',
              comType: 'listTemplate',
              rows: [],
              options: {
                getItems: cols => {
                  const columns = (cols || [])
                    .filter(col =>
                      ['aggregate', 'group', 'mixed'].includes(col.type),
                    )
                    .reduce((acc, cur) => acc.concat(cur.rows || []), [])
                    .map(c => ({
                      key: c.uid,
                      value: c.uid,
                      type: c.type,
                      label:
                        c.label || c.aggregate
                          ? `${c.aggregate}(${c.colName})`
                          : c.colName,
                    }));
                  return columns;
                },
              },
              template: {
                label: 'column.listItem',
                key: 'listItem',
                comType: 'group',
                rows: [
                  {
                    label: 'column.conditionalStyle',
                    key: 'conditionalStyle',
                    comType: 'group',
                    options: { expand: true },
                    rows: [
                      {
                        label: 'column.conditionalStylePanel',
                        key: 'conditionalStylePanel',
                        comType: 'conditionalStylePanel',
                      },
                    ],
                  },
                ],
              },
            },
          ],
        },
      ],
    },
    {
      label: 'style.title',
      key: 'style',
      comType: 'group',
      rows: [
        {
          label: 'style.enableFixedHeader',
          key: 'enableFixedHeader',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'style.enableBorder',
          key: 'enableBorder',
          default: true,
          comType: 'checkbox',
        },
        {
          label: 'style.leftFixedColumns',
          key: 'leftFixedColumns',
          comType: 'select',
          options: {
            mode: 'multiple',
            getItems: cols => {
              const columns = (cols || [])
                .filter(col =>
                  ['aggregate', 'group', 'mixed'].includes(col.type),
                )
                .reduce((acc, cur) => acc.concat(cur.rows || []), [])
                .map(c => ({
                  key: c.uid,
                  value: c.uid,
                  label:
                    c.label || c.aggregate
                      ? `${c.aggregate}(${c.colName})`
                      : c.colName,
                }));
              return columns;
            },
          },
        },
        {
          label: 'style.rightFixedColumns',
          key: 'rightFixedColumns',
          comType: 'select',
          options: {
            mode: 'multiple',
            getItems: cols => {
              const columns = (cols || [])
                .filter(col =>
                  ['aggregate', 'group', 'mixed'].includes(col.type),
                )
                .reduce((acc, cur) => acc.concat(cur.rows || []), [])
                .map(c => ({
                  key: c.uid,
                  value: c.uid,
                  label:
                    c.label || c.aggregate
                      ? `${c.aggregate}(${c.colName})`
                      : c.colName,
                }));
              return columns;
            },
          },
        },
      ],
    },
    {
      label: 'data.title',
      key: 'data',
      comType: 'group',
      rows: [
        {
          label: 'data.tableSize',
          key: 'tableSize',
          default: 'default',
          comType: 'select',
          options: {
            translateItemLabel: true,
            items: [
              { label: '@global@.tableSize.default', value: 'default' },
              { label: '@global@.tableSize.middle', value: 'middle' },
              { label: '@global@.tableSize.small', value: 'small' },
            ],
          },
        },
      ],
    },
  ],
  settings: [
    {
      label: 'paging.title',
      key: 'paging',
      comType: 'group',
      rows: [
        {
          label: 'paging.enablePaging',
          key: 'enablePaging',
          default: true,
          comType: 'checkbox',
          options: {
            needRefresh: true,
          },
        },
        {
          label: 'paging.pageSize',
          key: 'pageSize',
          default: 10,
          comType: 'select',
          options: {
            needRefresh: true,
            items: [
              { label: '5', value: 5 },
              { label: '10', value: 10 },
              { label: '20', value: 20 },
              { label: '30', value: 30 },
              { label: '40', value: 40 },
              { label: '50', value: 50 },
              { label: '100', value: 100 },
            ],
          },
          watcher: {
            deps: ['enablePaging'],
            action: props => {
              return {
                disabled: !props.enablePaging,
              };
            },
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
        header: {
          title: '表头分组',
          open: '打开',
          styleAndGroup: '表头分组',
        },
        column: {
          open: '打开列设置',
          list: '字段列表',
          sortAndFilter: '排序与过滤',
          enableSort: '开启列排序',
          basicStyle: '基础样式',
          conditionalStyle: '条件样式',
          conditionalStylePanel: '条件样式配置器',
          backgroundColor: '背景颜色',
          align: '对齐方式',
          enableFixedCol: '开启固定列宽',
          fixedColWidth: '固定列宽度设置',
          font: '字体与样式',
        },
        style: {
          title: '表格样式',
          enableFixedHeader: '固定表头',
          enableBorder: '显示边框',
          leftFixedColumns: '左侧固定列',
          rightFixedColumns: '右侧固定列',
        },
        data: {
          title: '表格数据控制',
          tableSize: '表格大小',
          autoMerge: '自动合并相同内容',
          enableRaw: '使用原始数据',
        },
        tableSize: {
          default: '默认',
          middle: '中',
          small: '小',
        },
        paging: {
          title: '分页设置',
          enablePaging: '启用分页',
          pageSize: '每页行数',
        },
      },
    },
    {
      lang: 'en-US',
      translation: {
        header: {
          title: 'Table Header Group',
          open: 'Open',
          styleAndGroup: 'Header Group',
        },
        column: {
          title: 'Table Data Column',
          open: 'Open Column Setting',
          list: 'Field List',
          sortAndFilter: 'Sort and Filter',
          enableSort: 'Enable Sort',
          basicStyle: 'Baisc Style',
          conditionalStyle: 'Column Conditional Style',
          conditionalStylePanel: 'Conditional Style Panel',
          backgroundColor: 'Background Color',
          align: 'Align',
          enableFixedCol: 'Enable Fixed Column',
          fixedColWidth: 'Fixed Column Width',
          font: 'Font and Style',
        },
        style: {
          title: 'Table Style',
          enableFixedHeader: 'Enable Fixed Header',
          enableBorder: 'Show Border',
          leftFixedColumns: 'Left Fixed Columns',
          rightFixedColumns: 'Right Fixed Columns',
        },
        tableSize: {
          default: 'Default',
          middle: 'Middle',
          small: 'Small',
        },
        data: {
          title: 'Table Data Setting',
          tableSize: 'Table Size',
          autoMerge: 'Auto Merge',
          enableRaw: 'Enable Raw Data',
        },
        paging: {
          title: 'Paging',
          enablePaging: 'Enable Paging',
          pageSize: 'Page Size',
        },
      },
    },
  ],
};

export default config;
