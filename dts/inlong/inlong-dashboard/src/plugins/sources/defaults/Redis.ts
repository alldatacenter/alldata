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
import { SourceInfo } from '../common/SourceInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

export default class RedisSource
  extends SourceInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
      min: 0,
    }),
  })
  @I18n('meta.Sources.Redis.Database')
  database: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Redis.Username')
  username: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Redis.Password')
  password: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: 'get',
    props: values => ({
      disabled: values?.status === 101,
      options: [
        {
          label: 'get',
          value: 'get',
        },
        {
          label: 'hget',
          value: 'hget',
        },
        {
          label: 'zscore',
          value: 'zscore',
        },
        {
          label: 'zrevrank',
          value: 'zrevrank',
        },
      ],
    }),
  })
  @I18n('meta.Sources.Redis.RedisCommand')
  redisCommand: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
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
    }),
  })
  @I18n('meta.Sources.Redis.RedisMode')
  redisMode: string;

  @FieldDecorator({
    type: 'input',
    visible: values => values.redisMode === 'cluster',
    props: values => ({
      disabled: values?.status === 101,
      placeholder: '127.0.0.1:6379,127.0.0.1:6378',
    }),
  })
  @I18n('meta.Sources.Redis.ClusterNodes')
  clusterNodes: string;

  @FieldDecorator({
    type: 'input',
    visible: values => values.redisMode === 'sentinel',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Redis.SentinelsInfo')
  sentinelsInfo: string;

  @FieldDecorator({
    type: 'input',
    visible: values => values.redisMode === 'sentinel',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Redis.AdditionalKey')
  additionalKey: string;

  @FieldDecorator({
    type: 'input',
    visible: values => values.redisMode === 'standalone',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Redis.Hostname')
  host: string;

  @FieldDecorator({
    type: 'inputnumber',
    visible: values => values.redisMode === 'standalone',
    initialValue: 6379,
    props: values => ({
      min: 1,
      max: 65535,
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Redis.Port')
  port: number;

  @FieldDecorator({
    type: 'input',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Redis.PrimaryKey')
  primaryKey: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 1000,
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Redis.Timeout')
  timeout: number;
}
