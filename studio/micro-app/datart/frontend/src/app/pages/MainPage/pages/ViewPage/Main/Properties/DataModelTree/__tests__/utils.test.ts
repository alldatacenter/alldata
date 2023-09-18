/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { DataViewFieldType } from 'app/constants';
import { ColumnCategories } from '../../../../constants';
import { ColumnRole } from '../../../../slice/types';
import { toModel } from '../utils';

describe('DataModelTree Util Tests', () => {
  describe('toModel Tests', () => {
    test('should get empty tree model when columns is empty', () => {
      const columns = [];
      const model = toModel(columns);
      expect(model).toEqual({});
    });

    test('should get tree model when no hierarchy', () => {
      const columns = [
        {
          name: 'age',
          type: DataViewFieldType.NUMERIC,
          category: ColumnCategories.UnCategorized,
          index: 0,
        },
        {
          name: 'address',
          type: DataViewFieldType.STRING,
          category: ColumnCategories.UnCategorized,
          index: 0,
        },
      ];
      const model = toModel(columns);
      expect(model).toEqual({
        age: {
          name: 'age',
          type: 'NUMERIC',
          category: 'UNCATEGORIZED',
          index: 0,
          children: undefined,
        },
        address: {
          name: 'address',
          type: 'STRING',
          category: 'UNCATEGORIZED',
          index: 1,
          children: undefined,
        },
      });
    });

    test('should get tree model when role is hierarchy with valid children', () => {
      const columns = [
        {
          name: 'name',
          role: ColumnRole.Role,
          type: DataViewFieldType.STRING,
          category: ColumnCategories.UnCategorized,
          index: 0,
          children: [
            {
              name: 'first-name',
              role: ColumnRole.Role,
              type: DataViewFieldType.STRING,
              category: ColumnCategories.UnCategorized,
              index: 0,
            },
          ],
        },
        {
          name: 'age',
          role: ColumnRole.Hierarchy,
          type: DataViewFieldType.NUMERIC,
          category: ColumnCategories.UnCategorized,
          index: 0,
          children: [],
        },
        {
          name: 'address',
          role: ColumnRole.Hierarchy,
          type: DataViewFieldType.STRING,
          category: ColumnCategories.UnCategorized,
          index: 0,
          children: [
            {
              name: 'address1',
              role: ColumnRole.Role,
              type: DataViewFieldType.STRING,
              category: ColumnCategories.UnCategorized,
              index: 0,
            },
            {
              name: 'address2',
              role: ColumnRole.Role,
              type: DataViewFieldType.STRING,
              category: ColumnCategories.UnCategorized,
              index: 0,
            },
          ],
        },
      ];
      const model = toModel(columns) as any;

      expect(model.name.name).toEqual('name');
      expect(model.name.role).toEqual('role');
      expect(model.name.type).toEqual('STRING');
      expect(model.name.category).toEqual('UNCATEGORIZED');
      expect(model.name.index).toEqual(0);
      expect(model.name.children).toEqual(undefined);

      expect(model.address.name).toEqual('address');
      expect(model.address.role).toEqual('hierachy');
      expect(model.address.type).toEqual('STRING');
      expect(model.address.category).toEqual('UNCATEGORIZED');
      expect(model.address.index).toEqual(1);
      expect(model.address.children).toEqual([
        {
          name: 'address1',
          role: 'role',
          type: 'STRING',
          category: 'UNCATEGORIZED',
          index: 0,
        },
        {
          name: 'address2',
          role: 'role',
          type: 'STRING',
          category: 'UNCATEGORIZED',
          index: 1,
        },
      ]);
    });

    test('should get tree model with additional columns', () => {
      const columns = [
        {
          name: 'age',
          type: DataViewFieldType.NUMERIC,
          category: ColumnCategories.UnCategorized,
          index: 0,
        },
      ];
      const additionalColumns = [
        {
          name: 'address',
          type: DataViewFieldType.STRING,
          category: ColumnCategories.UnCategorized,
          index: 0,
        },
      ];
      const model = toModel(columns, ...additionalColumns);
      expect(model).toEqual({
        age: {
          name: 'age',
          type: 'NUMERIC',
          category: 'UNCATEGORIZED',
          index: 0,
          children: undefined,
        },
        address: {
          name: 'address',
          type: 'STRING',
          category: 'UNCATEGORIZED',
          index: 1,
          children: undefined,
        },
      });
    });
  });
});
