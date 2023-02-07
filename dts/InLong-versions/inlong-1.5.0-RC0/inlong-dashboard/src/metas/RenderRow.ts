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

import type { FormItemProps as FieldItemType } from '@/components/FormGenerator';
import merge from 'lodash/merge';

export abstract class RenderRow {
  static FieldList: FieldItemType[] = [];

  static FieldDecorator(config: FieldItemType): PropertyDecorator {
    return (target: any, propertyKey: string) => {
      const { I18nMap, FieldList } = target.constructor;
      const newFieldList = [...FieldList];
      const existIndex = newFieldList.findIndex(item => item.name === propertyKey);
      const field = {
        ...config,
        name: propertyKey,
        label: I18nMap[propertyKey],
      };

      if (existIndex !== -1) {
        const mergeField = merge(newFieldList[existIndex], field);
        newFieldList.splice(existIndex, 1, mergeField);
      } else {
        newFieldList.push(field);
      }

      // const sortedFieldList = sortListPosition(newFieldList);
      target.constructor.FieldList = newFieldList;
    };
  }

  abstract renderRow(fields?: FieldItemType[]): FieldItemType[];
}

interface PositionObjectType extends Record<string, unknown> {
  position: ['before' | 'after', string];
}

function sortListPosition(list: PositionObjectType[], primaryPositionKey = 'name') {
  const output: Record<string, unknown>[] = [];
  const notFoundPosMap: Record<string, PositionObjectType> = {};
  const _list = [...list];
  let loopCount = 0;
  while (_list.length) {
    loopCount++;
    if (loopCount > 500) {
      console.error(
        '[Apache InLong Error] The number of loops of the sorting algorithm array has reached the maximum limit, please check or adjust the configuration.',
      );
      break;
    }
    const listItem = _list.shift();
    if (!listItem) continue;
    if (listItem.position) {
      const [positionType, positionName] = listItem.position;
      const index = output.findIndex(item => item[primaryPositionKey] === positionName);
      if (index !== -1) {
        output.splice(positionType === 'before' ? index : index + 1, 0, listItem);
      } else {
        notFoundPosMap[positionName] = listItem;
      }
    } else {
      output.push(listItem);
    }
    const currentItemName = listItem[primaryPositionKey] as string;
    if (notFoundPosMap[currentItemName]) {
      _list.push(notFoundPosMap[currentItemName]);
      delete notFoundPosMap[currentItemName];
    }
  }

  const notFoundPosList = Object.keys(notFoundPosMap).map(name => notFoundPosMap[name]);
  return output.concat(notFoundPosList);
}
