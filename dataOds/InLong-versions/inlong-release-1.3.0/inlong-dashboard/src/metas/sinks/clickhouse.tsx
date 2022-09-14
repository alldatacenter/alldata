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

import {
  getColsFromFields,
  GetStorageColumnsType,
  GetStorageFormFieldsType,
} from '@/utils/metaData';
import i18n from '@/i18n';
import { ColumnsType } from 'antd/es/table';
import EditableTable, { ColumnsItemProps } from '@/components/EditableTable';
import { excludeObject } from '@/utils';
import { sourceFields } from './common/sourceFields';

// ClickHouse targetType
const clickhouseTargetTypes = [
  'String',
  'Int8',
  'Int16',
  'Int32',
  'Int64',
  'Float32',
  'Float64',
  'DateTime',
  'Date',
].map(item => ({
  label: item,
  value: item,
}));

const getForm: GetStorageFormFieldsType = (
  type: 'form' | 'col' = 'form',
  { currentValues, inlongGroupId, isEdit, dataType } = {} as any,
) => {
  const fileds = [
    {
      name: 'dbName',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.DbName'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'tableName',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.TableName'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'enableCreateResource',
      type: 'radio',
      label: i18n.t('meta.Sinks.EnableCreateResource'),
      rules: [{ required: true }],
      initialValue: 1,
      tooltip: i18n.t('meta.Sinks.EnableCreateResourceHelp'),
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
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
      },
    },
    {
      name: 'username',
      type: 'input',
      label: i18n.t('meta.Sinks.Username'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'password',
      type: 'password',
      label: i18n.t('meta.Sinks.Password'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: {
          maxWidth: 500,
        },
      },
    },
    {
      type: 'input',
      label: 'JDBC URL',
      name: 'jdbcUrl',
      rules: [{ required: true }],
      props: {
        placeholder: 'jdbc:clickhouse://127.0.0.1:8123',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: { width: 500 },
      },
    },
    {
      name: 'flushInterval',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Clickhouse.FlushInterval'),
      initialValue: 1,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Clickhouse.FlushIntervalUnit'),
    },
    {
      name: 'flushRecord',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Clickhouse.FlushRecord'),
      initialValue: 1000,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Clickhouse.FlushRecordUnit'),
    },
    {
      name: 'retryTime',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Clickhouse.RetryTimes'),
      initialValue: 3,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Clickhouse.RetryTimesUnit'),
    },
    {
      name: 'isDistributed',
      type: 'radio',
      label: i18n.t('meta.Sinks.Clickhouse.IsDistributed'),
      initialValue: 0,
      props: {
        options: [
          {
            label: i18n.t('meta.Sinks.Clickhouse.Yes'),
            value: 1,
          },
          {
            label: i18n.t('meta.Sinks.Clickhouse.No'),
            value: 0,
          },
        ],
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
    },
    {
      name: 'partitionStrategy',
      type: 'select',
      label: i18n.t('meta.Sinks.Clickhouse.PartitionStrategy'),
      initialValue: 'BALANCE',
      rules: [{ required: true }],
      props: {
        options: [
          {
            label: 'BALANCE',
            value: 'BALANCE',
          },
          {
            label: 'RANDOM',
            value: 'RANDOM',
          },
          {
            label: 'HASH',
            value: 'HASH',
          },
        ],
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      visible: values => values.isDistributed,
    },
    {
      name: 'partitionFields',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.PartitionFields'),
      rules: [{ required: true }],
      visible: values => values.isDistributed && values.partitionStrategy === 'HASH',
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'engine',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.Engine'),
      initialValue: 'Log',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'orderBy',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.OrderBy'),
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'partitionBy',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.PartitionBy'),
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'primaryKey',
      type: 'input',
      label: i18n.t('meta.Sinks.Clickhouse.PrimaryKey'),
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'sinkFieldList',
      type: EditableTable,
      props: {
        size: 'small',
        editing: ![110, 130].includes(currentValues?.status),
        columns: getFieldListColumns(dataType, currentValues),
      },
    },
  ];

  return type === 'col'
    ? getColsFromFields(fileds)
    : fileds.map(item => excludeObject(['_inTable'], item));
};

const getFieldListColumns: GetStorageColumnsType = (dataType, currentValues) => {
  return [
    ...sourceFields,
    {
      title: `ClickHouse${i18n.t('meta.Sinks.Clickhouse.FieldName')}`,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.Clickhouse.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `ClickHouse${i18n.t('meta.Sinks.Clickhouse.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: clickhouseTargetTypes[0].value,
      type: 'select',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
        options: clickhouseTargetTypes,
      }),
      rules: [{ required: true }],
    },
    {
      title: 'DefaultType',
      dataIndex: 'defaultType',
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
        options: ['DEFAULT', 'EPHEMERAL', 'MATERIALIZED', 'ALIAS'].map(item => ({
          label: item,
          value: item,
        })),
      }),
    },
    {
      title: 'DefaultExpr',
      dataIndex: 'defaultExpr',
      type: 'input',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      visible: (text, record) =>
        ['DEFAULT', 'EPHEMERAL', 'MATERIALIZED', 'ALIAS'].includes(record.defaultType as string),
    },
    {
      title: i18n.t('meta.Sinks.Clickhouse.CompressionCode'),
      dataIndex: 'compressionCode',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: i18n.t('meta.Sinks.Clickhouse.TtlExpr'),
      dataIndex: 'ttlExpr',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `ClickHouse${i18n.t('meta.Sinks.Clickhouse.FieldDescription')}`,
      dataIndex: 'fieldComment',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
  ] as ColumnsItemProps[];
};

const tableColumns = getForm('col') as ColumnsType;

export const clickhouse = {
  getForm,
  getFieldListColumns,
  tableColumns,
};
