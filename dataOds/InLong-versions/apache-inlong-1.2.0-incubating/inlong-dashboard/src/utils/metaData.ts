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

import { FormItemProps } from '@/components/FormGenerator';
import { ColumnsType } from 'antd/es/table';
import { ColumnsItemProps } from '@/components/EditableTable';

export type GetStorageColumnsType = (
  dataType: string,
  currentValues?: Record<string, unknown>,
) => ColumnsItemProps[];

export type GetStorageFormFieldsType = (
  type: 'form' | 'col',
  {
    currentValues,
    inlongGroupId,
    isEdit,
    dataType,
    form,
  }?: {
    currentValues?: Record<string, any>;
    inlongGroupId?: string;
    isEdit?: boolean;
    dataType?: string;
    form?: any;
  },
) => FormItemProps[] | ColumnsType;

interface FieldConfigItem extends FormItemProps {
  _inTable?: boolean | Record<string, unknown>;
}

export const getColsFromFields = (fieldsConfig: FieldConfigItem[]): ColumnsType => {
  return fieldsConfig
    .filter(item => item._inTable)
    .map(item => {
      let output = {
        title: item.label,
        dataIndex: item.name,
      };
      if (typeof item._inTable === 'object') {
        output = {
          ...output,
          ...item._inTable,
        };
      }
      return output;
    });
};
