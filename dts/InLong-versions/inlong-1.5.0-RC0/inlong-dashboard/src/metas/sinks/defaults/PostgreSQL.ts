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
import { SinkInfo } from '../common/SinkInfo';
import { sourceFields } from '../common/sourceFields';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

const fieldTypesConf = {
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  INT2: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  SMALLSERIAL: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  SERIAL2: () => '',
  INTEGER: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  SERIAL: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  BIGSERIAL: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  REAL: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  FLOAT4: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  FLOAT8: (m, d) => (24 < m && m <= 53 ? '' : '24 < M <= 53'),
  DOUBLE: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  NUMERIC: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  DECIMAL: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  BOOLEAN: () => '',
  DATE: () => '',
  TIME: () => '',
  TIMESTAMP: () => '',
  CHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  CHARACTER: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  VARCHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  TEXT: () => '',
  BYTEA: () => '',
};

const postgreSqlFieldTypes = Object.keys(fieldTypesConf).reduce(
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
      placeholder: 'jdbc:postgresql://127.0.0.1:5432/db_name',
    }),
  })
  @ColumnDecorator()
  @I18n('JDBC URL')
  jdbcUrl: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.PostgreSQL.DbName')
  dbName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.PostgreSQL.TableName')
  tableName: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.PostgreSQL.PrimaryKey')
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
  enableCreateResource: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Username')
  username: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
    props: values => ({
      disabled: [110, 130].includes(values?.status),
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sinks.Password')
  password: string;

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
      title: `POSTGRESQL${i18n.t('meta.Sinks.PostgreSQL.FieldName')}`,
      dataIndex: 'fieldName',
      initialValue: '',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
          message: i18n.t('meta.Sinks.PostgreSQL.FieldNameRule'),
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `POSTGRESQL${i18n.t('meta.Sinks.PostgreSQL.FieldType')}`,
      dataIndex: 'fieldType',
      initialValue: postgreSqlFieldTypes[0].value,
      type: 'autocomplete',
      props: (text, record, idx, isNew) => ({
        options: postgreSqlFieldTypes,
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
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
      title: i18n.t('meta.Sinks.PostgreSQL.IsMetaField'),
      initialValue: 0,
      dataIndex: 'isMetaField',
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
      title: i18n.t('meta.Sinks.PostgreSQL.FieldFormat'),
      dataIndex: 'fieldFormat',
      initialValue: '',
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
      title: i18n.t('meta.Sinks.PostgreSQL.FieldDescription'),
      dataIndex: 'fieldComment',
      initialValue: '',
    },
  ];
};
