/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import {
  getColsFromFields,
  GetStorageColumnsType,
  GetStorageFormFieldsType,
} from '@/utils/metaData';
import { ColumnsType } from 'antd/es/table';
import EditableTable, { ColumnsItemProps } from '@/components/EditableTable';
import i18n from '@/i18n';
import { excludeObject } from '@/utils';
import { sourceFields } from './common/sourceFields';

// hbaseFieldTypes
const hbaseFieldTypes = [
  'int',
  'short',
  'long',
  'float',
  'double',
  'byte',
  'bytes',
  'string',
  'boolean',
].map(item => ({
  label: item,
  value: item,
}));

const getForm: GetStorageFormFieldsType = (
  type,
  { currentValues, inlongGroupId, isEdit, dataType, form } = {} as any,
) => {
  const fileds = [
    {
      type: 'input',
      label: i18n.t('meta.Sinks.HBase.Namespace'),
      name: 'namespace',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: { width: 500 },
      },
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.HBase.TableName'),
      name: 'tableName',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.HBase.RowKey'),
      name: 'rowKey',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.HBase.ZkQuorum'),
      name: 'zkQuorum',
      rules: [{ required: true }],
      props: {
        placeholder: '127.0.0.1:2181,127.0.0.2:2181',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.HBase.ZkNodeParent'),
      name: 'zkNodeParent',
      rules: [{ required: true }],
      props: {
        placeholder: '/hbase',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.HBase.BufferFlushMaxSize'),
      name: 'bufferFlushMaxSize',
      initialValue: 2,
      rules: [{ required: true }],
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      suffix: 'mb',
      _inTable: true,
    },
    {
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.HBase.BufferFlushMaxRows'),
      name: 'bufferFlushMaxRows',
      initialValue: 1000,
      rules: [{ required: true }],
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.HBase.BufferFlushInterval'),
      name: 'bufferFlushInterval',
      initialValue: 1,
      rules: [{ required: true }],
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      suffix: i18n.t('meta.Sinks.HBase.FlushIntervalUnit'),
      _inTable: true,
    },
    {
      type: (
        <EditableTable
          size="small"
          columns={getFieldListColumns(dataType, currentValues)}
          canDelete={(record, idx, isNew) => !isEdit || isNew}
        />
      ),
      name: 'sinkFieldList',
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
      title: `HBASE${i18n.t('meta.Sinks.HBase.FieldName')}`,
      dataIndex: 'fieldName',
      initialValue: '',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
          message: i18n.t('meta.Sinks.HBase.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `HBASE${i18n.t('meta.Sinks.HBase.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: hbaseFieldTypes[0].value,
      type: 'select',
      props: (text, record, idx, isNew) => ({
        options: hbaseFieldTypes,
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
    {
      title: 'cfName',
      dataIndex: 'cfName',
      type: 'input',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
    {
      title: 'ttl',
      dataIndex: 'ttl',
      type: 'inputnumber',
      props: (text, record, idx, isNew) => ({
        min: 1,
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
  ] as ColumnsItemProps[];
};

const tableColumns = getForm('col') as ColumnsType;

export const hbase = {
  getForm,
  getFieldListColumns,
  tableColumns,
};
