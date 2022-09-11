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
import { FormItemProps } from '@/components/FormGenerator';
import { pickObjectArray } from '@/utils';
import DataSourcesEditor from '../DataSourcesEditor';
import DataStorageEditor from '../DataStorageEditor/Editor';
import EditableTable from '@/components/EditableTable';
import i18n from '@/i18n';
import { fieldTypes as sourceFieldsTypes } from '@/components/MetaData/SourceDataFields';
import { Storages } from '@/components/MetaData';

type RestParams = {
  inlongGroupId?: string;
  // Whether to use true operation for data source management
  useDataSourcesActionRequest?: boolean;
  // Whether to use real operation for data storage management
  useDataStorageActionRequest?: boolean;
  // Whether the fieldList is in edit mode
  fieldListEditing?: boolean;
  readonly?: boolean;
};

export default (
  names: (string | FormItemProps)[],
  currentValues: Record<string, any> = {},
  {
    inlongGroupId,
    useDataSourcesActionRequest = false,
    useDataStorageActionRequest = false,
    fieldListEditing = true,
    readonly = false,
  }: RestParams = {},
): FormItemProps[] => {
  const basicProps = {
    inlongGroupId: inlongGroupId,
    inlongStreamId: currentValues.inlongStreamId,
  };

  const fields: FormItemProps[] = [
    {
      type: 'input',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataStreamID'),
      name: 'inlongStreamId',
      props: {
        maxLength: 32,
      },
      initialValue: currentValues.inlongStreamId,
      rules: [
        { required: true },
        {
          pattern: /^[a-z_\d]+$/,
          message: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataStreamRules'),
        },
      ],
    },
    {
      type: 'input',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataStreamName'),
      name: 'name',
      initialValue: currentValues.name,
    },
    {
      type: 'textarea',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataFlowIntroduction'),
      name: 'description',
      props: {
        showCount: true,
        maxLength: 100,
      },
      initialValue: currentValues.desc,
    },
    {
      type: 'radio',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Source'),
      name: 'dataSourceType',
      initialValue: currentValues.dataSourceType ?? 'AUTO_PUSH',
      props: {
        options: [
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.File'),
            value: 'FILE',
          },
          {
            label: 'BINLOG',
            value: 'BINLOG',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Autonomous'),
            value: 'AUTO_PUSH',
          },
        ],
      },
      rules: [{ required: true }],
    },
    {
      type: currentValues.dataSourceType ? (
        <DataSourcesEditor
          readonly={readonly}
          type={currentValues.dataSourceType}
          useActionRequest={useDataSourcesActionRequest}
          {...basicProps}
        />
      ) : (
        <div />
      ),
      preserve: false,
      name: 'dataSourcesConfig',
      visible: values => values.dataSourceType === 'BINLOG' || values.dataSourceType === 'FILE',
    },
    {
      type: 'radio',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataType'),
      name: 'dataType',
      initialValue: currentValues.dataType ?? 'CSV',
      tooltip: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataTypeCsvHelp'),
      props: {
        options: [
          {
            label: 'CSV',
            value: 'CSV',
          },
          {
            label: 'KEY-VALUE',
            value: 'KEY-VALUE',
          },
          {
            label: 'JSON',
            value: 'JSON',
          },
          {
            label: 'AVRO',
            value: 'AVRO',
          },
        ],
      },
      rules: [{ required: true }],
      visible: values => values.dataSourceType !== 'BINLOG',
    },
    {
      type: 'radio',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataEncoding'),
      name: 'dataEncoding',
      initialValue: currentValues.dataEncoding ?? 'UTF-8',
      props: {
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
      },
      rules: [{ required: true }],
      visible: values => values.dataSourceType === 'FILE' || values.dataSourceType === 'AUTO_PUSH',
    },
    {
      type: 'select',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.fileDelimiter'),
      name: 'dataSeparator',
      initialValue: '124',
      props: {
        dropdownMatchSelectWidth: false,
        options: [
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Space'),
            value: '32',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.VerticalLine'),
            value: '124',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Comma'),
            value: '44',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Semicolon'),
            value: '59',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Asterisk'),
            value: '42',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DoubleQuotes'),
            value: '34',
          },
        ],
        useInput: true,
        useInputProps: {
          placeholder: 'ASCII',
        },
        style: { width: 100 },
      },
      rules: [
        {
          required: true,
          type: 'number',
          transform: val => +val,
          min: 0,
          max: 127,
        },
      ],
      visible: values =>
        (values.dataSourceType === 'FILE' || values.dataSourceType === 'AUTO_PUSH') &&
        values.dataType === 'CSV',
    },
    {
      type: (
        <EditableTable
          size="small"
          editing={fieldListEditing}
          columns={[
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldName'),
              dataIndex: 'fieldName',
              props: () => ({
                disabled: !fieldListEditing,
              }),
              rules: [
                { required: true },
                {
                  pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
                  message: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldNameRule'),
                },
              ],
            },
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldType'),
              dataIndex: 'fieldType',
              type: 'select',
              initialValue: sourceFieldsTypes[0].value,
              props: () => ({
                disabled: !fieldListEditing,
                options: sourceFieldsTypes,
              }),
              rules: [{ required: true }],
            },
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldComment'),
              dataIndex: 'fieldComment',
            },
          ]}
        />
      ),
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.SourceDataField'),
      name: 'rowTypeFields',
      visible: () => !(currentValues.dataType as string[])?.includes('PB'),
    },
    {
      type: 'checkboxgroup',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataFlowDirection'),
      name: 'streamSink',
      props: {
        options: Storages.map(item => {
          return {
            label: item.label,
            value: item.value,
          };
        }).concat({
          label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.AutoConsumption'),
          value: 'AUTO_CONSUMPTION',
        }),
      },
    },
    ...Storages.map(item => item.value).reduce(
      (acc, item) =>
        acc.concat({
          type: (
            <DataStorageEditor
              readonly={readonly}
              defaultRowTypeFields={currentValues.rowTypeFields}
              type={item}
              dataType={currentValues.dataType}
              useActionRequest={useDataStorageActionRequest}
              {...basicProps}
            />
          ),
          name: `streamSink${item}`,
          visible: values => (values.streamSink as string[])?.includes(item),
        }),
      [],
    ),
  ];

  return pickObjectArray(names, fields);
};
