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

import { ChartDataViewFieldCategory, DataViewFieldType } from 'app/constants';
import { ChartDrillOption, DrillMode } from '../ChartDrillOption';

describe('ChartDrillOption Test', () => {
  test('should init model', () => {
    const fields = [];
    const option = new ChartDrillOption(fields);
    expect(option).not.toBeNull();
    expect(option.mode).toEqual(DrillMode.Normal);
    expect(option.canSelect).toBeTruthy();
    expect(option.isSelectedDrill).toBeFalsy();
    expect(option.getAllFields()).toEqual(fields);
    expect(option.getDrilledFields()).toEqual([]);
  });

  test('should get original fields', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllFields()).toEqual(fields);
  });

  test('should toggle select drill status', () => {
    const fields = [];
    const option = new ChartDrillOption(fields);
    expect(option.isSelectedDrill).toBeFalsy();
    option.toggleSelectedDrill();
    expect(option.isSelectedDrill).toBeTruthy();
  });

  test('should get drill down fields', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllDrillDownFields()).toEqual([]);
    expect(option.mode).toEqual(DrillMode.Normal);
    option.drillDown();
    expect(option.mode).toEqual(DrillMode.Drill);
    expect(option.getAllDrillDownFields()).toEqual([
      {
        field: {
          colName: '1',
          type: DataViewFieldType.STRING,
          category: ChartDataViewFieldCategory.Field,
        },
        condition: undefined,
      },
    ]);
    expect(option.getDrilledFields()).toEqual(fields.slice(0, 2));
    expect(option.getCurrentFields()).toEqual([
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ]);
  });

  test('should get drill down fields and filter', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    option.drillDown({ '1': 0 });
    expect(option.getAllDrillDownFields()).toEqual([
      {
        field: {
          colName: '1',
          type: DataViewFieldType.STRING,
          category: ChartDataViewFieldCategory.Field,
        },
        condition: {
          name: '1',
          type: 1022,
          value: 0,
          visualType: '',
          operator: 'EQ',
          children: undefined,
        },
      },
    ]);
  });

  test('should get fields when selected drill', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    option.toggleSelectedDrill();
    option.drillDown();
    expect(option.isSelectedDrill).toBeTruthy();
    expect(option.mode).toEqual(DrillMode.Drill);
    expect(option.getCurrentFields()).toEqual([
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ]);
  });

  test('should get fields when drill up', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllDrillDownFields()).toEqual([]);
    expect(option.mode).toEqual(DrillMode.Normal);
    option.drillDown();
    option.drillDown();
    expect(option.mode).toEqual(DrillMode.Drill);
    option.drillUp();
    expect(option.getCurrentFields()).toEqual([
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ]);
  });

  test('should rollback to normal status when drill up to beginning', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllDrillDownFields()).toEqual([]);
    expect(option.mode).toEqual(DrillMode.Normal);
    option.drillDown();
    expect(option.mode).toEqual(DrillMode.Drill);
    option.drillUp();
    expect(option.mode).toEqual(DrillMode.Normal);
    expect(option.getCurrentFields()).toEqual(undefined);
  });

  test('should get expand fields', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllDrillDownFields()).toEqual([]);
    expect(option.mode).toEqual(DrillMode.Normal);
    option.expandDown();
    expect(option.mode).toEqual(DrillMode.Expand);
    expect(option.getCurrentFields()).toEqual(fields.slice(0, 2));
    expect(option.getDrilledFields()).toEqual(fields.slice(0, 2));
  });

  test('should get fields when expand up', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '3',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option.getAllDrillDownFields()).toEqual([]);
    expect(option.mode).toEqual(DrillMode.Normal);
    option.expandDown();
    option.expandDown();
    expect(option.mode).toEqual(DrillMode.Expand);
    option.expandUp();
    expect(option.getCurrentFields()).toEqual(fields.slice(0, 2));
  });

  test('should clear all status when rollUp', () => {
    const fields = [
      {
        colName: '1',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
      {
        colName: '2',
        type: DataViewFieldType.STRING,
        category: ChartDataViewFieldCategory.Field,
      },
    ];
    const option = new ChartDrillOption(fields);
    expect(option).not.toBeNull();
    expect(option.mode).toEqual(DrillMode.Normal);
    expect(option.canSelect).toBeTruthy();
    expect(option.isSelectedDrill).toBeFalsy();
    expect(option.getAllFields()).toEqual(fields);
    expect(option.getDrilledFields()).toEqual([]);

    option.drillDown();
    option.rollUp();

    expect(option).not.toBeNull();
    expect(option.mode).toEqual(DrillMode.Normal);
    expect(option.canSelect).toBeTruthy();
    expect(option.isSelectedDrill).toBeFalsy();
    expect(option.getAllFields()).toEqual(fields);
    expect(option.getDrilledFields()).toEqual([]);
  });
});
