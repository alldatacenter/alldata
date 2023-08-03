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

import { DataWithBackend } from '@/plugins/DataWithBackend';
import { RenderRow } from '@/plugins/RenderRow';
import { RenderList } from '@/plugins/RenderList';
import i18n from '@/i18n';
import { TransformInfo } from '../common/TransformInfo';
import EditableTable from '@/ui/components/EditableTable';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

export default class TubeMqSource
  extends TransformInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    initialValue: '将选中字段转换为指定时间格式',
    props: values => ({
      disabled: true,
    }),
  })
  @ColumnDecorator()
  @I18n('使用场景')
  transformDefinition: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
  })
  @ColumnDecorator()
  @I18n('preNodeNames')
  preNodeNames: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
  })
  @ColumnDecorator()
  @I18n('postNodeNames')
  postNodeNames: string;

  @FieldDecorator({
    type: EditableTable,
    props: values => ({
      size: 'small',
      editing: ![110, 130].includes(values?.status),
      columns: getFieldListColumns(values),
    }),
  })
  fieldList: Record<string, unknown>[];
}

const getFieldListColumns = sinkValues => {
  return [
    {
      title: `字段`,
      dataIndex: 'fieldName',
      initialValue: 'id',
      rules: [
        { required: true },
        {
          pattern: /^[a-z][0-9a-z_]*$/,
        },
      ],
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
    },
    {
      title: `源时间格式`,
      dataIndex: 'fieldType',
      initialValue: 'int',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
    {
      title: '目标时间格式',
      dataIndex: 'fieldValue',
      initialValue: 'test',
      type: 'input',
      props: (text, record, idx, isNew) => ({
        disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
      }),
      rules: [{ required: true }],
    },
    // {
    //   title: '目标时间格式',
    //   dataIndex: '',
    //   type: 'input',
    //   props: (text, record, idx, isNew) => ({
    //     disabled: [110, 130].includes(sinkValues?.status as number) && !isNew,
    //   }),
    //   rules: [{ required: true }],
    // },
  ];
};
