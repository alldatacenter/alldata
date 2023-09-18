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
import { Column, ColumnRole } from '../slice/types';
import {
  addPathToHierarchyStructureAndChangeName,
  dataModelColumnSorter,
  diffMergeHierarchyModel,
} from '../utils';

describe('dataModelColumnSorter test', () => {
  test('should sort by alphabet with the STRING column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.STRING },
      { name: 'a', type: DataViewFieldType.STRING },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by alphabet with the Numeric column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.NUMERIC },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.NUMERIC },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by alphabet with string and date column type', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.DATE },
      { name: 'a', type: DataViewFieldType.DATE },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('c');
  });

  test('should sort by column type when column type with STRING, Numeric, DATE', () => {
    const columns: Column[] = [
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.DATE },
      { name: 'd', type: DataViewFieldType.DATE },
      { name: 'e', type: DataViewFieldType.NUMERIC },
      { name: 'f', type: DataViewFieldType.STRING },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('c');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('d');
    expect(columns.sort(dataModelColumnSorter)[3].name).toEqual('f');
    expect(columns.sort(dataModelColumnSorter)[4].name).toEqual('b');
    expect(columns.sort(dataModelColumnSorter)[5].name).toEqual('e');
  });

  test('should sort by column type with multiple column types and hierarchy columns', () => {
    const columns: Column[] = [
      {
        name: 'e',
        type: DataViewFieldType.STRING,
        role: ColumnRole.Hierarchy,
      },
      { name: 'c', type: DataViewFieldType.STRING },
      { name: 'b', type: DataViewFieldType.NUMERIC },
      { name: 'a', type: DataViewFieldType.DATE },
      {
        name: 'f',
        type: DataViewFieldType.DATE,
        role: ColumnRole.Hierarchy,
      },
    ];
    expect(columns.sort(dataModelColumnSorter)[0].name).toEqual('e');
    expect(columns.sort(dataModelColumnSorter)[1].name).toEqual('f');
    expect(columns.sort(dataModelColumnSorter)[2].name).toEqual('a');
    expect(columns.sort(dataModelColumnSorter)[3].name).toEqual('c');
    expect(columns.sort(dataModelColumnSorter)[4].name).toEqual('b');
  });
});

describe('diffMergeHierarchyModel test', () => {
  test('should append all new column to hierarchy without children', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {},
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
    });
  });

  test('should append new column to hierarchy without children', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
      },
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: model.columns,
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
    });
  });

  test('should remove column in hierarchy which not exist in columns', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
      },
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: model.columns,
      hierarchy: {
        id: { name: 'id', type: 'STRING' },
      },
    });
  });

  test('should remove child column in hierarchy', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
      },
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [
            { name: 'id', type: 'STRING' },
            { name: 'age', type: 'NUMBER' },
            { name: 'address', type: 'STRING' },
          ],
        },
      },
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: model.columns,
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [
            { name: 'id', type: 'STRING' },
            { name: 'age', type: 'NUMBER' },
          ],
        },
      },
    });
  });

  test('should delete branch node in hierarchy when child is not in columns', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
      },
      hierarchy: {
        dealers: {
          name: 'dealers',
          children: [{ name: 'unkown', type: 'STRING' }],
        },
      },
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: model.columns,
      hierarchy: {},
    });
  });

  test('should delete and add new to hierarchy model', () => {
    const model = {
      columns: {
        id: { name: 'id', type: 'STRING' },
        age: { name: 'age', type: 'NUMBER' },
        address: { name: 'address', type: 'STRING' },
        newId: { name: 'newId', type: 'STRING' },
      },
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
        dealers: {
          name: 'dealers',
          children: [
            { name: 'address', type: 'STRING' },
            { name: 'post', type: 'STRING' },
          ],
        },
      },
    };
    expect(diffMergeHierarchyModel(model as any, 'SQL')).toMatchObject({
      columns: model.columns,
      hierarchy: {
        age: { name: 'age', type: 'NUMBER' },
        newId: { name: 'newId', type: 'STRING' },
        dealers: {
          name: 'dealers',
          children: [{ name: 'address', type: 'STRING' }],
        },
      },
    });
  });
});

describe('addPathToHierarchyStructureAndChangeName test', () => {
  test('test if hierarchy is empty', () => {
    const hierarchy: any = null;
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual(null);
  });

  test('test view type is SQL and have name', () => {
    const hierarchy: any = {
      QD_id: {
        name: 'QD_id',
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and dont name', () => {
    const hierarchy: any = {
      QD_id: {},
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and have path', () => {
    const hierarchy: any = {
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and path in undefined', () => {
    const hierarchy: any = {
      QD_id: {
        name: 'QD_id',
        path: undefined,
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL and name is array', () => {
    const hierarchy: any = {
      QD_id: {
        name: ['QD_id'],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      QD_id: {
        name: 'QD_id',
        path: ['QD_id'],
      },
    });
  });

  test('test view type is SQL have children', () => {
    const hierarchy: any = {
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: 'QD_id',
          },
        ],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: 'QD_id',
            path: ['QD_id'],
          },
        ],
      },
    });
  });

  test('test view type is SQL have children name is Array', () => {
    const hierarchy: any = {
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: ['QD_id'],
          },
        ],
      },
    };
    const result = addPathToHierarchyStructureAndChangeName(hierarchy, 'SQL');
    expect(result).toEqual({
      文件夹1: {
        name: '文件夹1',
        children: [
          {
            name: ['QD_id'],
            path: ['QD_id'],
          },
        ],
      },
    });
  });

  test('test view type is STRUCT view - name is string array', () => {
    const hierarchy: any = {
      'dad.num': {
        name: '["dad","num"]',
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      hierarchy,
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: ['dad', 'num'],
      },
    });
  });

  test('test view type is STRUCT view - name is array', () => {
    const hierarchy: any = {
      'dad.num': {
        name: ['dad', 'num'],
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      hierarchy,
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: ['dad', 'num'],
      },
    });
  });

  test('test view type is STRUCT view is not have name', () => {
    const hierarchy: any = {
      'dad.num': {},
    };

    const result = addPathToHierarchyStructureAndChangeName(
      hierarchy,
      'STRUCT',
    );
    expect(result).toEqual({
      'dad.num': {
        name: 'dad.num',
        path: undefined,
      },
    });
  });

  test('test view type is STRUCT view - have children', () => {
    const hierarchy: any = {
      file: {
        name: 'file1',
        children: [
          {
            name: '["dad", "num"]',
          },
        ],
      },
    };

    const result = addPathToHierarchyStructureAndChangeName(
      hierarchy,
      'STRUCT',
    );
    expect(result).toEqual({
      file: {
        name: 'file1',
        children: [
          {
            name: 'dad.num',
            path: ['dad', 'num'],
          },
        ],
      },
    });
  });
});
