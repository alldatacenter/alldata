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

const kuduFieldTypes = [
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
      label: 'Hash',
      value: 'Hash',
      disabled: false,
    },
    {
      label: 'PrimaryKey',
      value: 'PrimaryKey',
      disabled: false,
    },
  ];

  return data.filter(item => !item.disabled);
};

export default class KuduSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: NodeSelect,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      nodeType: 'KUDU',
    }),
  })
  @I18n('meta.Sinks.DataNodeName')
  @SyncField()
  dataNodeName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Kudu.TableName')
  @SyncField()
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
  @SyncField()
  enableCreateResource: number;

  @FieldDecorator({
    type: 'input',
    initialValue: '',
    rules: [{ required: false }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
    visible: values => values!.enableCreateResource === 1,
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Kudu.buckets')
  @SyncField()
  buckets: number;

  @FieldDecorator({
    type: EditableTable,
    rules: [{ required: false }],
    initialValue: [],
    tooltip: i18n.t('meta.Sinks.Kudu.ExtListHelper'),
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
  @SyncField()
  @I18n('meta.Sinks.Kudu.ExtList')
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
  @SyncField()
  @I18n('meta.Sinks.Kudu.DataConsistency')
  dataConsistency: string;

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
}

const getFieldListColumns = sinkValues => {
  return [
    ...sourceFields,
    {
      title: `Kudu ${i18n.t('meta.Sinks.Kudu.FieldName')}`,
      width: 110,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.Kudu.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `Kudu ${i18n.t('meta.Sinks.Kudu.FieldType')}`,
      dataIndex: 'fieldType',
      width: 130,
      initialValue: kuduFieldTypes[0].value,
      type: 'select',
      rules: [{ required: true, message: `${i18n.t('meta.Sinks.FieldTypeMessage')}` }],
      props: (text, record, idx, isNew) => ({
        options: kuduFieldTypes,
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
      title: i18n.t('meta.Sinks.Kudu.PartitionStrategy'),
      dataIndex: 'partitionStrategy',
      type: 'select',
      initialValue: 'None',
      rules: [{ required: true }],
      props: (text, record) => ({
        options: matchPartitionStrategies(record.fieldType),
      }),
    },
    {
      title: i18n.t('meta.Sinks.Kudu.FieldDescription'),
      dataIndex: 'fieldComment',
    },
  ];
};
