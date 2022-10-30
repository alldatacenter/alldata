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

// mysqlFieldTypes
const mysqlFieldTypes = [
  'TINYINT',
  'SMALLINT',
  'MEDIUMINT',
  'INT',
  'FLOAT',
  'BIGINT',
  'DOUBLE',
  'NUMERIC',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'TIME',
  'DATETIME',
  'CHAR',
  'VARCHAR',
  'TEXT',
  'BINARY',
  'VARBINARY',
  'BLOB',
  // 'interval',
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
      label: 'JDBC URL',
      name: 'jdbcUrl',
      rules: [{ required: true }],
      props: {
        placeholder: 'jdbc:mysql://127.0.0.1:3306/write',
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: { width: 500 },
      },
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.MySQL.TableName'),
      name: 'tableName',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'input',
      label: i18n.t('meta.Sinks.Greenplum.PrimaryKey'),
      name: 'primaryKey',
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
      label: i18n.t('meta.Sinks.Username'),
      name: 'username',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      type: 'password',
      label: i18n.t('meta.Sinks.Password'),
      name: 'password',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
        style: {
          maxWidth: 500,
        },
      },
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
      title: `MYSQL${i18n.t('meta.Sinks.MySQL.FieldName')}`,
      dataIndex: 'fieldName',
      initialValue: '',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
          message: i18n.t('meta.Sinks.MySQL.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `MYSQL${i18n.t('meta.Sinks.MySQL.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: mysqlFieldTypes[0].value,
      type: 'select',
      props: (text, record, idx, isNew) => ({
        options: mysqlFieldTypes,
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
    {
      title: i18n.t('meta.Sinks.MySQL.IsMetaField'),
      dataIndex: 'isMetaField',
      initialValue: 0,
      type: 'select',
      props: (text, record, idx, isNew) => ({
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
    },
    {
      title: i18n.t('meta.Sinks.MySQL.FieldFormat'),
      dataIndex: 'fieldFormat',
      initialValue: 0,
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        options: ['MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'SQL', 'ISO_8601'].map(item => ({
          label: item,
          value: item,
        })),
      }),
      visible: (text, record) =>
        ['BIGINT', 'DATE', 'TIMESTAMP'].includes(record.fieldType as string),
    },
    {
      title: i18n.t('meta.Sinks.MySQL.FieldDescription'),
      dataIndex: 'fieldComment',
      initialValue: '',
    },
  ] as ColumnsItemProps[];
};

const tableColumns = getForm('col') as ColumnsType;

export const mysql = {
  getForm,
  getFieldListColumns,
  tableColumns,
};
