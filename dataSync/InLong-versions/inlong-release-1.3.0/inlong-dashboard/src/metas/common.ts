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

import type { FormItemProps } from '@/components/FormGenerator';
import { ColumnType } from 'antd/es/table';
import { excludeObject } from '@/utils';

export interface FieldItemType extends FormItemProps {
  position?: string[];
  _renderTable?: boolean | ColumnType<Record<string, any>>;
}

export const genFields = (
  fieldsDefault: FieldItemType[],
  fieldsExtends?: FieldItemType[],
): FieldItemType[] => {
  const output: FieldItemType[] = [];
  const fields = fieldsDefault.concat(fieldsExtends);
  while (fields.length) {
    const fieldItem = fields.shift();
    if (!fieldItem) continue;
    if (fieldItem.position) {
      const [positionType, positionName] = fieldItem.position;
      const index = output.findIndex(item => item.name === positionName);
      if (index !== -1) {
        output.splice(positionType === 'before' ? index : index + 1, 0, fieldItem);
      } else {
        fields.push(fieldItem);
      }
    } else {
      output.push(fieldItem);
    }
  }

  return output;
};

export const genForm = (fields: Omit<FieldItemType, 'position'>[]): FormItemProps[] => {
  return fields.map(item => excludeObject(['_renderTable'], item));
};

export const genTable = (
  fields: Omit<FieldItemType, 'position'>[],
): ColumnType<Record<string, any>>[] => {
  return fields
    .filter(item => Boolean(item._renderTable))
    .map(item => {
      let output: ColumnType<Record<string, any>> = {
        title: item.label,
        dataIndex: item.name,
      };
      if (typeof item._renderTable === 'object') {
        output = {
          ...output,
          ...item._renderTable,
        };
      }
      return output;
    });
};
