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

import { DataWithBackend } from '@/metas/DataWithBackend';
import { RenderRow } from '@/metas/RenderRow';
import { RenderList } from '@/metas/RenderList';
import i18n from '@/i18n';
import EditableTable from '@/components/EditableTable';
import { sourceFields } from '../common/sourceFields';
import { SinkInfo } from '../common/SinkInfo';
import NodeSelect from '@/components/NodeSelect';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

const fieldTypesConf = {
  CHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  VARCHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  DATE: () => '',
  TINYINT: (m, d) => (1 <= m && m <= 4 ? '' : '1<=M<=4'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1<=M<=6'),
  INT: (m, d) => (1 <= m && m <= 11 ? '' : '1<=M<=11'),
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1<=M<=20'),
  LARGEINT: () => '',
  STRING: () => '',
  DATETIME: () => '',
  FLOAT: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DOUBLE: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DECIMAL: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BOOLEAN: () => '',
};

const fieldTypes = Object.keys(fieldTypesConf).reduce(
  (acc, key) =>
    acc.concat({
      label: key,
      value: key,
    }),
  [],
);

export default class StarRocksSink
  extends SinkInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: NodeSelect,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      nodeType: 'STARROCKS',
    }),
  })
  @I18n('meta.Sinks.DataNodeName')
  dataNodeName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.DatabaseName')
  databaseName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.TableName')
  tableName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.PrimaryKey')
  primaryKey: string;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: false,
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
  @I18n('meta.Sinks.StarRocks.SinkMultipleEnable')
  sinkMultipleEnable: boolean;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    visible: record => record.sinkMultipleEnable === true,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.SinkMultipleFormat')
  sinkMultipleFormat: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    visible: record => record.sinkMultipleEnable === true,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.DatabasePattern')
  databasePattern: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    visible: record => record.sinkMultipleEnable === true,
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.StarRocks.TablePattern')
  tablePattern: string;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      editing: ![110, 130].includes(values?.status),
      columns: getFieldListColumns(values),
    }),
  })
  sinkFieldList: Record<string, unknown>[];
}

const getFieldListColumns = sinkValues => {
  return [
    ...sourceFields,
    {
      title: `StarRocks${i18n.t('meta.Sinks.StarRocks.FieldName')}`,
      dataIndex: 'fieldName',
      initialValue: '',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
          message: i18n.t('meta.Sinks.StarRocks.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `StarRocks${i18n.t('meta.Sinks.StarRocks.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: fieldTypes[0].value,
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        options: fieldTypes,
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
        allowClear: true,
      }),
      rules: [
        { required: true },
        () => ({
          validator(_, val) {
            if (val) {
              const [, type = val, typeLength = ''] = val.match(/^(.+)\((.+)\)$/) || [];
              if (fieldTypesConf.hasOwnProperty(type)) {
                const [m = -1, d = -1] = typeLength.split(',');
                const errMsg = fieldTypesConf[type]?.(m, d);
                if (typeLength && errMsg) return Promise.reject(new Error(errMsg));
              } else {
                return Promise.reject(new Error('FieldType error'));
              }
            }
            return Promise.resolve();
          },
        }),
      ],
    },
    {
      title: i18n.t('meta.Sinks.StarRocks.IsMetaField'),
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
      title: i18n.t('meta.Sinks.StarRocks.FieldFormat'),
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
      title: i18n.t('meta.Sinks.StarRocks.FieldDescription'),
      dataIndex: 'fieldComment',
      initialValue: '',
    },
  ];
};
