/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { DataWithBackend } from '@/metas/DataWithBackend';
import { RenderRow } from '@/metas/RenderRow';
import { RenderList } from '@/metas/RenderList';
import { NodeInfo } from '../common/NodeInfo';
import i18n from '@/i18n';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class ElasticsearchNode
  extends NodeInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    tooltip: i18n.t('meta.Nodes.ES.HostHelp'),
  })
  @I18n('meta.Nodes.ES.Host')
  url: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
  })
  @I18n('meta.Nodes.ES.Username')
  username: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
  })
  @I18n('meta.Nodes.ES.Password')
  token: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 4000,
    props: {
      min: 0,
    },
    suffix: i18n.t('meta.Nodes.ES.BulkActionUnit'),
  })
  @I18n('bulkAction')
  bulkAction: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 10,
    props: {
      min: 0,
    },
    suffix: 'MB',
  })
  @I18n('bulkSize')
  bulkSizeMb: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 60,
    props: {
      min: 0,
    },
    suffix: i18n.t('meta.Nodes.ES.FlushIntervalUnit'),
  })
  @I18n('meta.Nodes.ES.FlushInterval')
  flushInterval: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 5,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.ES.ConcurrentRequests')
  concurrentRequests: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 10,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.ES.MaxConnect')
  maxConnect: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 32767,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.ES.KeywordMaxLength')
  keywordMaxLength: number;

  @FieldDecorator({
    type: 'radio',
    initialValue: false,
    props: {
      options: [
        {
          label: i18n.t('basic.Yes'),
          value: true,
        },
        {
          label: i18n.t('basic.No'),
          value: false,
        },
      ],
    },
  })
  @I18n('meta.Nodes.ES.IsUseIndexId')
  isUseIndexId: boolean;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 2,
    props: {
      min: 0,
    },
  })
  @I18n('meta.Nodes.ES.MaxThreads')
  maxThreads: number;

  @FieldDecorator({
    type: 'input',
  })
  @I18n('auditSetName')
  auditSetName: string;
}
