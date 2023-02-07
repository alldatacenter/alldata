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
import { statusList, genStatusTag } from './status';
import { sources, defaultValue } from '..';

const { I18nMap, I18n } = DataWithBackend;
const { FieldList, FieldDecorator } = RenderRow;
const { ColumnList, ColumnDecorator } = RenderList;

export class SourceDefaultInfo implements DataWithBackend, RenderRow, RenderList {
  static I18nMap = I18nMap;
  static FieldList = FieldList;
  static ColumnList = ColumnList;

  readonly id: number;

  @FieldDecorator({
    // This field is not visible or editable, but form value should exists.
    type: 'text',
    hidden: true,
  })
  @I18n('inlongGroupId')
  readonly inlongGroupId: string;

  @FieldDecorator({
    type: 'text',
    hidden: true,
  })
  @I18n('inlongStreamId')
  readonly inlongStreamId: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: Boolean(values.id),
      maxLength: 128,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Name')
  sourceName: string;

  @FieldDecorator({
    type: sources.length > 3 ? 'select' : 'radio',
    rules: [{ required: true }],
    initialValue: defaultValue,
    props: values => ({
      disabled: Boolean(values.id),
      dropdownMatchSelectWidth: false,
      options: sources
        .filter(item => item.value)
        .map(item => ({
          label: item.label,
          value: item.value,
        })),
    }),
  })
  @ColumnDecorator({
    render: type => sources.find(c => c.value === type)?.label || type,
  })
  @I18n('meta.Sources.Type')
  sourceType: string;

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
  readonly status: string;

  parse(data) {
    return data;
  }

  stringify(data) {
    return data;
  }

  renderRow() {
    const constructor = this.constructor as typeof SourceDefaultInfo;
    return constructor.FieldList;
  }

  renderList() {
    const constructor = this.constructor as typeof SourceDefaultInfo;
    return constructor.ColumnList;
  }
}
