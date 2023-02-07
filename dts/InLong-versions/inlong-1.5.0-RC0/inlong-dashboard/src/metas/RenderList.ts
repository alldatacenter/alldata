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

import { ColumnType } from 'antd/es/table';
import merge from 'lodash/merge';

type ColumnItemType = ColumnType<Record<string, any>>;

export abstract class RenderList {
  static ColumnList: ColumnItemType[] = [];

  static ColumnDecorator(config?: ColumnItemType): PropertyDecorator {
    return (target: any, propertyKey: string) => {
      const { I18nMap, ColumnList } = target.constructor;
      const newColumnList = [...ColumnList];
      const oldIndex = ColumnList.findIndex(item => item.name === propertyKey);
      const column = {
        ...(typeof config === 'object' ? config : {}),
        dataIndex: propertyKey,
        title: I18nMap[propertyKey],
      };

      if (oldIndex !== -1) {
        const mergeField = merge(newColumnList[oldIndex], column);
        newColumnList.splice(oldIndex, 1, mergeField);
      } else {
        newColumnList.push(column);
      }

      target.constructor.ColumnList = newColumnList;
    };
  }

  abstract renderList(columns?: ColumnItemType[]): ColumnItemType[];
}
