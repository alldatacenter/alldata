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
import i18n from '@/i18n';
import EditableTable from '@/ui/components/EditableTable';
import { sourceFields } from '../common/sourceFields';
import { SinkInfo } from '../common/SinkInfo';
import NodeSelect from '@/ui/components/NodeSelect';

const { I18n } = DataWithBackend;
const { FieldDecorator, SyncField } = RenderRow;
const { ColumnDecorator } = RenderList;

const redisTargetTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'timestamptz',
  'binary',
].map(item => ({
  label: item,
  value: item,
}));

const schemaMappingModeStrategies = dataType => {
  const data = [
    {
      label: 'STATIC_PREFIX_MATCH',
      value: 'STATIC_PREFIX_MATCH',
      enable: !['BITMAP'].includes(dataType),
    },
    {
      label: 'STATIC_KV_PAIR',
      value: 'STATIC_KV_PAIR',
      enable: !['BITMAP', 'PLAIN'].includes(dataType),
    },
    {
      label: 'DYNAMIC',
      value: 'DYNAMIC',
      enable: !['PLAIN'].includes(dataType),
    },
  ];
  return data.filter(item => item.enable);
};

export default class RedisSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: NodeSelect,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      nodeType: 'Redis',
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.DataNodeName')
  dataNodeName: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: '',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'PLAIN',
          value: 'PLAIN',
        },
        {
          label: 'HASH',
          value: 'HASH',
        },
        {
          label: 'BITMAP',
          value: 'BITMAP',
        },
      ],
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @I18n('meta.Sinks.Redis.DataType')
  dataType: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: schemaMappingModeStrategies(values!.dataType),
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @I18n('meta.Sinks.Redis.SchemaMapMode')
  schemaMapMode: string;

  @FieldDecorator({
    type: 'select',
    initialValue: 'CSV',
    visible: values => values!.schemaMapMode == 'STATIC_PREFIX_MATCH',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'CSV',
          value: 'CSV',
        },
        {
          label: 'KV',
          value: 'KV',
        },
        {
          label: 'AVRO',
          value: 'AVRO',
        },
        {
          label: 'JSON',
          value: 'JSON',
        },
      ],
    }),
    rules: [{ required: true }],
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.FormatDataType')
  formatDataType: string;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: true,
    visible: values => values!.schemaMapMode == 'STATIC_PREFIX_MATCH',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
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
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.FormatIgnoreParseError')
  formatIgnoreParseError: boolean;

  @FieldDecorator({
    type: 'radio',
    initialValue: 'UTF-8',
    visible: values => values!.schemaMapMode == 'STATIC_PREFIX_MATCH',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'UTF-8',
          value: 'UTF-8',
        },
        {
          label: 'GBK',
          value: 'GBK',
        },
      ],
    }),
    rules: [{ required: true }],
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.FormatDataEncoding')
  formatDataEncoding: string;

  @FieldDecorator({
    type: 'select',
    initialValue: '124',
    visible: values => values!.schemaMapMode == 'STATIC_PREFIX_MATCH',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      dropdownMatchSelectWidth: false,
      options: [
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.Space'),
          value: '32',
        },
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.VerticalLine'),
          value: '124',
        },
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.Comma'),
          value: '44',
        },
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.Semicolon'),
          value: '59',
        },
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.Asterisk'),
          value: '42',
        },
        {
          label: i18n.t('meta.Sinks.Redis.DataSeparator.DoubleQuotes'),
          value: '34',
        },
      ],
      useInput: true,
      useInputProps: {
        placeholder: 'ASCII',
      },
      style: { width: 100 },
    }),
    rules: [
      {
        required: true,
        type: 'number',
        transform: val => +val,
        min: 0,
        max: 127,
      },
    ],
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.FormatDataSeparator')
  formatDataSeparator: string;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      min: 0,
      placholder: '0',
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @I18n('meta.Sinks.Redis.Database')
  database: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 0,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      min: 0,
    }),
    rules: [{ required: false }],
    suffix: i18n.t('meta.Sinks.Redis.TtlUnit'),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.Ttl')
  ttl: number;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      editing: ![110, 130].includes(values?.status),
      columns: getFieldListColumns(values),
      canBatchAdd: true,
      upsertByFieldKey: true,
    }),
  })
  sinkFieldList: Record<string, unknown>[];

  @FieldDecorator({
    type: EditableTable,
    rules: [{ required: false }],
    initialValue: [],
    props: values => ({
      size: 'small',
      columns: [
        {
          title: 'Key',
          dataIndex: 'keyName',
          props: {
            disabled: [110, 130].includes(values?.status),
          },
        },
        {
          title: 'Value',
          dataIndex: 'keyValue',
          props: {
            disabled: [110, 130].includes(values?.status),
          },
        },
      ],
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.ExtList')
  properties: string;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    initialValue: 2000,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
    suffix: i18n.t('meta.Sinks.Redis.TimeoutUnit'),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.Timeout')
  timeout: number;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    initialValue: 2000,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
    suffix: i18n.t('meta.Sinks.Redis.SoTimeoutUnit'),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.SoTimeout')
  soTimeout: number;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    initialValue: 2,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.MaxTotal')
  maxTotal: number;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    initialValue: 8,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.MaxIdle')
  maxIdle: number;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    initialValue: 1,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.MinIdle')
  minIdle: number;

  @FieldDecorator({
    type: 'inputnumber',
    isPro: true,
    rules: [{ required: true }],
    initialValue: 1,
    props: values => ({
      disabled: values?.status === 101,
      min: 1,
    }),
  })
  @SyncField()
  @I18n('meta.Sinks.Redis.MaxRetries')
  maxRetries: number;
}

const getFieldListColumns = sinkValues => {
  return [
    ...sourceFields,
    {
      title: `Redis${i18n.t('meta.Sinks.Redis.FieldName')}`,
      dataIndex: 'fieldName',
      initialValue: '',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
          message: i18n.t('meta.Sinks.Redis.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `Redis${i18n.t('meta.Sinks.Redis.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: redisTargetTypes[0].value,
      type: 'select',
      props: (text, record, idx, isNew) => ({
        options: redisTargetTypes,
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
      rules: [{ required: true, message: `${i18n.t('meta.Sinks.FieldTypeMessage')}` }],
    },
    {
      title: i18n.t('meta.Sinks.Redis.FieldFormat'),
      dataIndex: 'fieldFormat',
      initialValue: 0,
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        options: ['MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'SQL', 'ISO_8601'].map(item => ({
          label: item,
          value: item,
        })),
      }),
      visible: (text, record) => ['BIGINT', 'DATE'].includes(record.fieldType as string),
    },
    {
      title: i18n.t('meta.Sinks.Redis.FieldDescription'),
      dataIndex: 'fieldComment',
      initialValue: '',
    },
  ];
};
