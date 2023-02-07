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
import { SinkInfo } from '../common/SinkInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

export default class KafkaSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    initialValue: '127.0.0.1:9092',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Kafka.Server')
  bootstrapServers: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('Topic')
  topicName: string;

  @FieldDecorator({
    type: 'radio',
    initialValue: 'JSON',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'JSON',
          value: 'JSON',
        },
        {
          label: 'CANAL',
          value: 'CANAL',
        },
        {
          label: 'AVRO',
          value: 'AVRO',
        },
      ],
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Kafka.SerializationType')
  serializationType: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 3,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      min: 1,
      max: 30,
    }),
    rules: [{ required: true }],
  })
  @I18n('meta.Sinks.Kafka.PartitionNum')
  partitionNum: number;

  @FieldDecorator({
    type: 'radio',
    initialValue: 'earliest',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'earliest',
          value: 'earliest',
        },
        {
          label: 'latest',
          value: 'latest',
        },
        {
          label: 'none',
          value: 'none',
        },
      ],
    }),
  })
  @I18n('meta.Sinks.Kafka.AutoOffsetReset')
  autoOffsetReset: string;
}
