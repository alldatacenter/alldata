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

const esTypes = [
  'text',
  'keyword',
  'date',
  'boolean',
  'long',
  'integer',
  'short',
  'byte',
  'double',
  'float',
  'half_float',
  'scaled_float',
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
      name: 'indexName',
      type: 'input',
      label: i18n.t('meta.Sinks.Es.IndexName'),
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
      label: i18n.t('meta.Sinks.Es.Host'),
      name: 'host',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
    {
      name: 'port',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Es.Port'),
      initialValue: 9200,
      props: {
        min: 1,
        max: 65535,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
    },
    {
      name: 'flushInterval',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Es.FlushInterval'),
      initialValue: 1,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Es.FlushIntervalUnit'),
    },
    {
      name: 'flushRecord',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Es.FlushRecord'),
      initialValue: 1000,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Es.FlushRecordUnit'),
    },
    {
      name: 'retryTime',
      type: 'inputnumber',
      label: i18n.t('meta.Sinks.Es.RetryTimes'),
      initialValue: 3,
      props: {
        min: 1,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
      suffix: i18n.t('meta.Sinks.Es.RetryTimesUnit'),
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
      title: `ES ${i18n.t('meta.Sinks.Es.FieldName')}`,
      dataIndex: 'fieldName',
      rules: [
        { required: true },
        {
          pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
          message: i18n.t('meta.Sinks.Es.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
    {
      title: `ES ${i18n.t('meta.Sinks.Es.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: esTypes[0].value,
      type: 'select',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
        options: esTypes,
      }),
      rules: [{ required: true }],
    },
    {
      title: 'Analyzer',
      dataIndex: 'analyzer',
      type: 'input',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      visible: (text, record) => record.fieldType === 'text',
    },
    {
      title: 'SearchAnalyzer',
      dataIndex: 'searchAnalyzer',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      visible: (text, record) => record.fieldType === 'text',
    },
    {
      title: i18n.t('meta.Sinks.Es.DateFormat'),
      dataIndex: 'format',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      visible: (text, record) => record.fieldType === 'date',
    },
    {
      title: 'ScalingFactor',
      dataIndex: 'scalingFactor',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
      visible: (text, record) => record.fieldType === 'scaled_float',
    },
    {
      title: `ES ${i18n.t('meta.Sinks.Es.FieldDescription')}`,
      dataIndex: 'fieldComment',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(currentValues?.status as number) && !isNew,
      }),
    },
  ] as ColumnsItemProps[];
};

const tableColumns = getForm('col') as ColumnsType;

export const es = {
  getForm,
  getFieldListColumns,
  tableColumns,
};
