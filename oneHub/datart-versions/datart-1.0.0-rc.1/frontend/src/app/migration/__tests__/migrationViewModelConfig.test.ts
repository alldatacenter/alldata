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

import { APP_VERSION_BETA_4 } from '../constants';
import beginViewModelMigration from '../ViewConfig/migrationViewModelConfig';

describe('migrationViewModelConfig Test', () => {
  test('should get latest version after migration', () => {
    const model = JSON.stringify({});
    expect(beginViewModelMigration(model, 'SQL')).toEqual(
      JSON.stringify({
        hierarchy: {},
        columns: {},
        version: APP_VERSION_BETA_4,
      }),
    );
  });

  test('should get latest version even model is empty', () => {
    expect(beginViewModelMigration('', 'SQL')).toEqual('');
  });

  test('should migrate model to nested model object', () => {
    const originalModel = {
      column1: { name: 'column1', role: 'role', type: 'STRING' },
      column2: { name: 'column2', role: 'role', type: 'NUMBER' },
    };
    const migrationResultObj = JSON.parse(
      beginViewModelMigration(JSON.stringify(originalModel), 'SQL'),
    );
    expect(migrationResultObj.columns).toMatchObject(originalModel);
    expect(migrationResultObj.hierarchy).toMatchObject(originalModel);
    expect(migrationResultObj.version).toEqual(APP_VERSION_BETA_4);
  });

  test('should migrate model name to columns', () => {
    const originalModel = {
      column1: { role: 'role', type: 'STRING' },
      column2: { role: 'role', type: 'NUMBER' },
    };
    const migrationResultObj = JSON.parse(
      beginViewModelMigration(JSON.stringify(originalModel), 'SQL'),
    );
    expect(migrationResultObj.columns).toMatchObject({
      column1: { name: 'column1', role: 'role', type: 'STRING' },
      column2: { name: 'column2', role: 'role', type: 'NUMBER' },
    });
    expect(migrationResultObj.hierarchy).toMatchObject({
      column1: { name: 'column1', role: 'role', type: 'STRING' },
      column2: { name: 'column2', role: 'role', type: 'NUMBER' },
    });
    expect(migrationResultObj.version).toEqual(APP_VERSION_BETA_4);
  });

  test('should migrate hierarchy name to path', () => {
    const originalModel = {
      column1: { role: 'role', type: 'STRING' },
      column2: { role: 'role', type: 'NUMBER' },
    };
    const migrationResultObj = JSON.parse(
      beginViewModelMigration(JSON.stringify(originalModel), 'SQL'),
    );
    expect(migrationResultObj.hierarchy).toEqual({
      column1: {
        name: 'column1',
        path: ['column1'],
        role: 'role',
        type: 'STRING',
      },
      column2: {
        name: 'column2',
        path: ['column2'],
        role: 'role',
        type: 'NUMBER',
      },
    });

    expect(migrationResultObj.version).toEqual(APP_VERSION_BETA_4);
  });
  test('should migrate hierarchy name to path for STRUCT VIEW', () => {
    const originalModel = {
      '["dad", "column1"]': {
        role: 'role',
        type: 'STRING',
        name: ['dad', 'column1'],
      },
      '["dad", "column2"]': {
        role: 'role',
        type: 'NUMBER',
        name: ['dad', 'column2'],
      },
    };
    const migrationResultObj = beginViewModelMigration(
      JSON.stringify(originalModel),
      'STRUCT',
    );
    expect(JSON.parse(migrationResultObj).hierarchy).toEqual({
      '["dad", "column1"]': {
        name: '["dad", "column1"]',
        path: ['dad', 'column1'],
        role: 'role',
        type: 'STRING',
      },
      '["dad", "column2"]': {
        name: '["dad", "column2"]',
        path: ['dad', 'column2'],
        role: 'role',
        type: 'NUMBER',
      },
    });

    const originalModel2 = {
      '["dad", "column1"]': {
        role: 'role',
        type: 'STRING',
        name: ['dad', 'column1'],
        children: null,
      },
      '["dad", "column2"]': {
        role: 'role',
        type: 'NUMBER',
        name: ['dad', 'column2'],
      },
    };
    const migrationResultObj2 = beginViewModelMigration(
      JSON.stringify(originalModel2),
      'STRUCT',
    );
    expect(JSON.parse(migrationResultObj2).hierarchy).toEqual({
      '["dad", "column1"]': {
        name: '["dad", "column1"]',
        path: ['dad', 'column1'],
        role: 'role',
        type: 'STRING',
        children: null,
      },
      '["dad", "column2"]': {
        name: '["dad", "column2"]',
        path: ['dad', 'column2'],
        role: 'role',
        type: 'NUMBER',
      },
    });

    const originalModel3 = {
      file1: {
        role: 'role',
        type: 'STRING',
        name: 'file1',
        children: [
          {
            role: 'role',
            type: 'NUMBER',
            name: '["dad", "column1"]',
          },
        ],
      },
      file2: {
        role: 'role',
        type: 'NUMBER',
        name: 'file2',
        children: [
          {
            role: 'role',
            type: 'NUMBER',
            name: '["dad", "column2"]',
          },
        ],
      },
    };
    const migrationResultObj3 = beginViewModelMigration(
      JSON.stringify(originalModel3),
      'STRUCT',
    );
    expect(JSON.parse(migrationResultObj3).hierarchy).toEqual({
      file1: {
        name: 'file1',
        role: 'role',
        type: 'STRING',
        children: [
          {
            role: 'role',
            type: 'NUMBER',
            name: 'dad.column1',
            path: ['dad', 'column1'],
          },
        ],
      },
      file2: {
        name: 'file2',
        role: 'role',
        type: 'NUMBER',
        children: [
          {
            role: 'role',
            type: 'NUMBER',
            name: 'dad.column2',
            path: ['dad', 'column2'],
          },
        ],
      },
    });
  });
});
