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

const fieldTypesConf = {
  TINYINT: (m, d) => (1 <= m && m <= 4 ? '' : '1<=M<=4'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1<=M<=6'),
  MEDIUMINT: (m, d) => (1 <= m && m <= 9 ? '' : '1<=M<=9'),
  INT: (m, d) => (1 <= m && m <= 11 ? '' : '1<=M<=11'),
  FLOAT: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1<=M<=20'),
  DOUBLE: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  NUMERIC: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DECIMAL: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BOOLEAN: () => '',
  DATE: () => '',
  TIME: () => '',
  DATETIME: () => '',
  CHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  VARCHAR: (m, d) => (1 <= m && m <= 16383 ? '' : '1<=M<=16383'),
  TEXT: () => '',
  BINARY: (m, d) => (1 <= m && m <= 64 ? '' : '1<=M<=64'),
  VARBINARY: (m, d) => (1 <= m && m <= 64 ? '' : '1<=M<=64'),
  BLOB: () => '',
};

const fieldTypes = Object.keys(fieldTypesConf).reduce(
  (acc, key) =>
    acc.concat({
      label: key,
      value: key,
    }),
  [],
);

export default class HiveSink extends SinkInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.MySQL.DatabaseName')
  @SyncField()
  databaseName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.MySQL.TableName')
  @SyncField()
  tableName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.MySQL.PrimaryKey')
  @SyncField()
  primaryKey: string;

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
    type: NodeSelect,
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
      nodeType: 'MYSQL',
    }),
  })
  @I18n('meta.Sinks.DataNodeName')
  dataNodeName: string;

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
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `MYSQL${i18n.t('meta.Sinks.MySQL.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: fieldTypes[0].value,
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        options: fieldTypes,
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
        allowClear: true,
      }),
      rules: [
        { required: true, message: `${i18n.t('meta.Sinks.FieldTypeMessage')}` },
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
  ];
};
