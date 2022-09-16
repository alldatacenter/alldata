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

import React from 'react';
import i18n from '@/i18n';
import {
  getColsFromFields,
  GetStorageColumnsType,
  GetStorageFormFieldsType,
} from '@/utils/metaData';
import { ColumnsType } from 'antd/es/table';
import EditableTable from '@/components/EditableTable';
import { excludeObject } from '@/utils';
import TextSwitch from '@/components/TextSwitch';
import { sourceFields } from './common/sourceFields';
// import { Button, message } from 'antd';
// import request from '@/utils/request';

const icebergFieldTypes = [
  'string',
  'boolean',
  'int',
  'long',
  'float',
  'double',
  'decimal',
  'date',
  'time',
  'timestamp',
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

const getForm: GetStorageFormFieldsType = (
  type: 'form' | 'col' = 'form',
  { currentValues, isEdit, dataType, form } = {} as any,
) => {
  const fileds = [
    {
      name: 'dbName',
      type: 'input',
      label: i18n.t('meta.Sinks.Iceberg.DbName'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'tableName',
      type: 'input',
      label: i18n.t('meta.Sinks.Iceberg.TableName'),
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'radio',
      label: i18n.t('meta.Sinks.EnableCreateResource'),
      name: 'enableCreateResource',
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
      type: 'input',
      label: 'Catalog URI',
      name: 'catalogUri',
      rules: [{ required: true }],
      props: {
        placeholder: 'thrift://127.0.0.1:9083',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: { width: 500 },
      },
      /*suffix: (
        <Button
          onClick={async () => {
            const values = await form.validateFields(['username', 'password', 'jdbcUrl']);
            const res = await request({
              url: '/sink/query/testConnection',
              method: 'POST',
              data: values,
            });
            res
              ? message.success(
                  i18n.t('meta.Sinks.Hive.ConnectionSucceeded'),
                )
              : message.error(
                  i18n.t('meta.Sinks.Hive.ConnectionFailed'),
                );
          }}
        >
          {i18n.t('meta.Sinks.Hive.ConnectionTest')}
        </Button>
      ),*/
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.Iceberg.Warehouse'),
      name: 'warehouse',
      rules: [{ required: true }],
      props: {
        placeholder: 'hdfs://127.0.0.1:9000/user/iceberg/warehouse',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'fileFormat',
      type: 'radio',
      label: i18n.t('meta.Sinks.Iceberg.FileFormat'),
      initialValue: 'Parquet',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        options: [
          {
            label: 'Parquet',
            value: 'Parquet',
          },
          {
            label: 'Orc',
            value: 'Orc',
          },
          {
            label: 'Avro',
            value: 'Avro',
          },
        ],
      },
    },
    {
      name: 'extList',
      label: i18n.t('meta.Sinks.Iceberg.ExtList'),
      type: EditableTable,
      props: {
        size: 'small',
        editing: !isEdit,
        columns: [
          {
            title: 'Key',
            dataIndex: 'keyName',
            props: (text, record, idx, isNew) => ({
              disabled: [110, 130].includes(currentValues?.status) && !isNew,
            }),
          },
          {
            title: 'Value',
            dataIndex: 'keyValue',
            props: (text, record, idx, isNew) => ({
              disabled: [110, 130].includes(currentValues?.status) && !isNew,
            }),
          },
        ],
      },
      initialValue: [],
    },
    { name: '_showHigher', type: <TextSwitch />, initialValue: false },
    {
      name: 'dataConsistency',
      type: 'select',
      label: i18n.t('meta.Sinks.Iceberg.DataConsistency'),
      initialValue: 'EXACTLY_ONCE',
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
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
      },
      hidden: !currentValues?._showHigher,
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
      title: `Iceberg ${i18n.t('meta.Sinks.Iceberg.FieldName')}`,
      width: 110,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.Iceberg.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `Iceberg ${i18n.t('meta.Sinks.Iceberg.FieldType')}`,
      dataIndex: 'fieldType',
      width: 130,
      initialValue: icebergFieldTypes[0].value,
      type: 'select',
      rules: [{ required: true }],
      props: (text, record, idx, isNew) => ({
        options: icebergFieldTypes,
        onChange: value => {
          const partitionStrategies = matchPartitionStrategies(value);
          if (partitionStrategies.every(item => item.value !== record.partitionStrategy)) {
            return {
              partitionStrategy: partitionStrategies[0].value,
            };
          }
        },
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
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
      title: i18n.t('meta.Sinks.Iceberg.PartitionStrategy'),
      dataIndex: 'partitionStrategy',
      type: 'select',
      initialValue: 'None',
      rules: [{ required: true }],
      props: (text, record) => ({
        options: matchPartitionStrategies(record.fieldType),
      }),
    },
    {
      title: i18n.t('meta.Sinks.Iceberg.FieldDescription'),
      dataIndex: 'fieldComment',
    },
  ];
};

const tableColumns = getForm('col') as ColumnsType;

export const iceberg = {
  getForm,
  getFieldListColumns,
  tableColumns,
};
