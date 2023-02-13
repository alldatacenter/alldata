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
import i18n from '@/i18n';
import EditableTable from '@/components/EditableTable';
import { sourceFields } from '../common/sourceFields';
import { SinkInfo } from '../common/SinkInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

const hudiFieldTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'decimal',
  'timestamptz',
  'binary',
  'fixed',
  'uuid',
].map(item => ({
  label: item,
  value: item,
}));

const matchPartitionStrategies = fieldType => {
  const data = [
    {
      label: 'None',
      value: 'None',
      disabled: false,
    },
    {
      label: 'Identity',
      value: 'Identity',
      disabled: false,
    },
    {
      label: 'Year',
      value: 'Year',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Month',
      value: 'Month',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Day',
      value: 'Day',
      disabled: !['timestamp', 'date'].includes(fieldType),
    },
    {
      label: 'Hour',
      value: 'Hour',
      disabled: fieldType !== 'timestamp',
    },
    {
      label: 'Bucket',
      value: 'Bucket',
      disabled: ![
        'string',
        'boolean',
        'short',
        'int',
        'long',
        'float',
        'double',
        'decimal',
      ].includes(fieldType),
    },
    {
      label: 'Truncate',
      value: 'Truncate',
      disabled: !['string', 'int', 'long', 'binary', 'decimal'].includes(fieldType),
    },
  ];

  return data.filter(item => !item.disabled);
};

export default class HudiSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.DbName')
  dbName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.TableName')
  tableName: string;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: 1,
    tooltip: i18n.t('meta.Sinks.EnableCreateResourceHelp'),
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: i18n.t('basic.Yes'),
          value: 1,
        },
        {
          label: i18n.t('basic.No'),
          value: 0,
        },
      ],
    }),
  })
  @I18n('meta.Sinks.EnableCreateResource')
  enableCreateResource: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: 'thrift://127.0.0.1:9083',
    }),
  })
  @ColumnDecorator()
  @I18n('Catalog URI')
  catalogUri: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      placeholder: 'hdfs://127.0.0.1:9000/user/hudi/warehouse',
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.Warehouse')
  warehouse: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: 'Parquet',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'Parquet',
          value: 'Parquet',
        },
        // {
        //   label: 'Orc',
        //   value: 'Orc',
        // },
        // {
        //   label: 'Avro',
        //   value: 'Avro',
        // },
      ],
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.FileFormat')
  fileFormat: string;

  @FieldDecorator({
    type: EditableTable,
    rules: [{ required: false }],
    initialValue: [],
    tooltip: i18n.t('meta.Sinks.Hudi.ExtListHelper'),
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
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.ExtList')
  extList: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    initialValue: 'EXACTLY_ONCE',
    isPro: true,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      options: [
        {
          label: 'EXACTLY_ONCE',
          value: 'EXACTLY_ONCE',
        },
        {
          label: 'AT_LEAST_ONCE',
          value: 'AT_LEAST_ONCE',
        },
      ],
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.DataConsistency')
  dataConsistency: string;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      editing: ![110, 130].includes(values?.status),
      columns: getFieldListColumns(values),
    }),
  })
  sinkFieldList: Record<string, unknown>[];

  @FieldDecorator({
    type: 'input',
    tooltip: i18n.t('meta.Sinks.Hudi.PrimaryKeyHelper'),
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.PrimaryKey')
  primaryKey: string;

  @FieldDecorator({
    type: 'input',
    tooltip: i18n.t('meta.Sinks.Hudi.PartitionKeyHelper'),
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Hudi.PartitionKey')
  partitionKey: string;
}

const getFieldListColumns = sinkValues => {
  return [
    ...sourceFields,
    {
      title: `Hudi ${i18n.t('meta.Sinks.Hudi.FieldName')}`,
      width: 110,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.Hudi.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `Hudi ${i18n.t('meta.Sinks.Hudi.FieldType')}`,
      dataIndex: 'fieldType',
      width: 130,
      initialValue: hudiFieldTypes[0].value,
      type: 'select',
      rules: [{ required: true }],
      props: (text, record, idx, isNew) => ({
        options: hudiFieldTypes,
        onChange: value => {
          const partitionStrategies = matchPartitionStrategies(value);
          if (partitionStrategies.every(item => item.value !== record.partitionStrategy)) {
            return {
              partitionStrategy: partitionStrategies[0].value,
            };
          }
        },
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: 'Length',
      dataIndex: 'fieldLength',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'fixed',
    },
    {
      title: 'Precision',
      dataIndex: 'fieldPrecision',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'decimal',
    },
    {
      title: 'Scale',
      dataIndex: 'fieldScale',
      type: 'inputnumber',
      props: {
        min: 0,
      },
      initialValue: 1,
      rules: [{ type: 'number', required: true }],
      visible: (text, record) => record.fieldType === 'decimal',
    },
    {
      title: i18n.t('meta.Sinks.Hudi.FieldDescription'),
      dataIndex: 'fieldComment',
    },
  ];
};
