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
import { fieldTypes as sourceFieldsTypes } from '@/metas/sinks/common/sourceFields';
import { statusList, genStatusTag } from './status';

const { I18nMap, I18n } = DataWithBackend;
const { FieldList, FieldDecorator } = RenderRow;
const { ColumnList, ColumnDecorator } = RenderList;

export class StreamDefaultInfo implements DataWithBackend, RenderRow, RenderList {
  static I18nMap = I18nMap;
  static FieldList = FieldList;
  static ColumnList = ColumnList;

  readonly id: number;

  @FieldDecorator({
    type: 'input',
    props: values => ({
      disabled: Boolean(values?.status),
      maxLength: 64,
    }),
    rules: [
      { required: true },
      {
        pattern: /^[0-9a-z_-]+$/,
        message: i18n.t('meta.Stream.StreamIdRules'),
      },
    ],
  })
  @ColumnDecorator()
  @I18n('meta.Stream.StreamId')
  inlongStreamId: string;

  @FieldDecorator({
    type: 'input',
  })
  @ColumnDecorator()
  @I18n('meta.Stream.StreamName')
  name: string;

  @FieldDecorator({
    type: 'textarea',
    props: {
      showCount: true,
      maxLength: 256,
    },
  })
  @I18n('meta.Stream.Description')
  description: string;

  @ColumnDecorator()
  @I18n('basic.Creator')
  readonly creator: string;

  @ColumnDecorator()
  @I18n('basic.CreateTime')
  readonly createTime: string;

  @FieldDecorator({
    type: 'select',
    props: {
      allowClear: true,
      options: statusList,
      dropdownMatchSelectWidth: false,
    },
    visible: false,
  })
  @ColumnDecorator({
    render: text => genStatusTag(text),
  })
  @I18n('basic.Status')
  status: string;

  @FieldDecorator({
    type: 'radio',
    initialValue: 'CSV',
    tooltip: i18n.t('meta.Stream.DataTypeHelp'),
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
  @I18n('meta.Stream.DataType')
  dataType: string;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: true,
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
  @I18n('meta.Stream.IgnoreParseError')
  ignoreParseError: boolean;

  @FieldDecorator({
    type: 'radio',
    initialValue: 'UTF-8',
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
  @I18n('meta.Stream.DataEncoding')
  dataEncoding: string;

  @FieldDecorator({
    type: 'select',
    initialValue: '124',
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      dropdownMatchSelectWidth: false,
      options: [
        {
          label: i18n.t('meta.Stream.DataSeparator.Space'),
          value: '32',
        },
        {
          label: i18n.t('meta.Stream.DataSeparator.VerticalLine'),
          value: '124',
        },
        {
          label: i18n.t('meta.Stream.DataSeparator.Comma'),
          value: '44',
        },
        {
          label: i18n.t('meta.Stream.DataSeparator.Semicolon'),
          value: '59',
        },
        {
          label: i18n.t('meta.Stream.DataSeparator.Asterisk'),
          value: '42',
        },
        {
          label: i18n.t('meta.Stream.DataSeparator.DoubleQuotes'),
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
  @I18n('meta.Stream.DataSeparator')
  dataSeparator: string;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      canDelete: record => !(record.id && [110, 130].includes(values?.status)),
      columns: [
        {
          title: i18n.t('meta.Stream.FieldName'),
          dataIndex: 'fieldName',
          rules: [
            { required: true },
            {
              pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
              message: i18n.t('meta.Stream.FieldNameRule'),
            },
          ],
          props: (text, record) => ({
            disabled: record.id && [110, 130].includes(values?.status),
          }),
        },
        {
          title: i18n.t('meta.Stream.FieldType'),
          dataIndex: 'fieldType',
          type: 'select',
          initialValue: sourceFieldsTypes[0].value,
          props: (text, record) => ({
            disabled: record.id && [110, 130].includes(values?.status),
            options: sourceFieldsTypes,
          }),
          rules: [{ required: true }],
        },
        {
          title: i18n.t('meta.Stream.FieldComment'),
          dataIndex: 'fieldComment',
        },
      ],
    }),
  })
  @I18n('meta.Stream.SourceDataField')
  rowTypeFields: Record<string, string>[];

  @FieldDecorator({
    type: 'radio',
    isPro: true,
    rules: [{ required: true }],
    initialValue: true,
    tooltip: i18n.t('meta.Stream.WrapWithInlongMsgHelp'),
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
  @I18n('meta.Stream.WrapWithInlongMsg')
  wrapWithInlongMsg: boolean;

  parse(data) {
    return data;
  }

  stringify(data) {
    return data;
  }

  renderRow() {
    const constructor = this.constructor as typeof StreamDefaultInfo;
    return constructor.FieldList;
  }

  renderList() {
    const constructor = this.constructor as typeof StreamDefaultInfo;
    return constructor.ColumnList;
  }
}
