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
    },
    {
      label: 'metrics',
      key: 'metrics',
      type: 'aggregate',
      actions: {
        NUMERIC: ['alias', 'sortable', 'format', 'aggregate'],
        STRING: ['alias', 'sortable', 'format', 'aggregate'],
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
      label: 'common.title',
      key: 'richTextMarkdown',
      comType: 'group',
      rows: [
        {
          label: 'richTextMarkdown.openQuillMarkdown',
          key: 'openQuillMarkdown',
          default: false,
          comType: 'checkbox',
        },
      ],
    },
    {
      label: 'common.title',
      hidden: true,
      key: 'delta',
      comType: 'group',
      rows: [
        {
          label: 'delta.richText',
          key: 'richText',
          default: '',
          comType: 'input',
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
  i18ns: [
    {
      lang: 'zh-CN',
      translation: {
        common: {
          title: '富文本',
          noData: '暂无可用字段',
          richTextPreview: '富文本预览',
          preview: '预览',
          referenceFields: '引用字段',
        },
        delta: {
          text: '内容',
        },
        richTextMarkdown: {
          openQuillMarkdown: '开启Markdown',
        },
      },
    },
    {
      lang: 'en-US',
      translation: {
        common: {
          title: 'Rich Text',
          noData: 'No Data',
          richTextPreview: 'Rich Text Preview',
          preview: 'Preview',
          referenceFields: 'Reference Fields',
        },
        delta: {
          text: 'Text',
        },
        richTextMarkdown: {
          openQuillMarkdown: 'Open Markdown',
        },
      },
    },
  ],
};

export default config;
