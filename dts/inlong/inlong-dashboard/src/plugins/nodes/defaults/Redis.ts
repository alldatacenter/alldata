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

import { DataWithBackend } from '@/plugins/DataWithBackend';
import { RenderRow } from '@/plugins/RenderRow';
import { RenderList } from '@/plugins/RenderList';
import { NodeInfo } from '../common/NodeInfo';
import i18n from '@/i18n';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class RedisNode extends NodeInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    tooltip: i18n.t('meta.Nodes.Redis.ClusterModeHelper'),
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'cluster',
          value: 'cluster',
        },
        {
          label: 'sentinel',
          value: 'sentinel',
        },
        {
          label: 'standalone',
          value: 'standalone',
        },
      ],
      placeholder: i18n.t('meta.Nodes.Redis.ClusterModeHelper'),
    }),
  })
  @I18n('meta.Nodes.Redis.ClusterMode')
  clusterMode: string;

  @FieldDecorator({
    type: 'input',
    initialValue: '127.0.0.1',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: '127.0.0.1',
    }),
    visible: values => values!.clusterMode === 'standalone',
  })
  @I18n('meta.Nodes.Redis.Host')
  host: string;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: false }],
    initialValue: 6379,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      min: 1,
      max: 65535,
      placeholder: i18n.t('meta.Nodes.Redis.PortHelper'),
    }),
    visible: values => values!.clusterMode === 'standalone',
  })
  @I18n('meta.Nodes.Redis.Port')
  port: number;

  @FieldDecorator({
    type: 'input',
    initialValue: '',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
    visible: values => values!.clusterMode === 'sentinel',
  })
  @I18n('meta.Nodes.Redis.MasterName')
  masterName: string;

  @FieldDecorator({
    type: 'input',
    initialValue: '',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: '127.0.0.1:6379,127.0.0.1:6378',
    }),
    visible: values => values!.clusterMode === 'sentinel',
  })
  @I18n('meta.Nodes.Redis.SentinelsInfo')
  sentinelsInfo: string;

  @FieldDecorator({
    type: 'input',
    initialValue: '',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: '127.0.0.1:6379,127.0.0.1:6378',
    }),
    visible: values => values!.clusterMode === 'cluster',
  })
  @I18n('meta.Nodes.Redis.ClusterNodes')
  clusterNodes: string;
}
