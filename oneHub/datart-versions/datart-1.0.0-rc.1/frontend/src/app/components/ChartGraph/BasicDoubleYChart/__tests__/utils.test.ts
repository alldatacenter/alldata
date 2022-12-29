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
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { getMinAndMaxNumber, transformToDataSet } from 'app/utils/chartHelper';
import { getYAxisIntervalConfig } from '../utils';

describe('DoubleYChart calc medthod', () => {
  describe('getYAxisIntervalConfig Test - ', () => {
    test(`Get chart y axis interval, datas typeof string, datas > 0`, () => {
      const columns = [
        ['stephen', '36', '90'],
        ['jack', '28', '102'],
        ['tom', '30', '3587'],
        ['john', '32', '324'],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        0, 36,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 3587,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 10,
        leftMax: 40,
        leftMin: 0,
        rightInterval: 1000,
        rightMax: 4000,
        rightMin: 0,
      });
    });

    test(`Get chart y axis interval, datas typeof number, datas > 0`, () => {
      const columns = [
        ['stephen', 36, 90],
        ['jack', 28, 102],
        ['tom', 30, 3587],
        ['john', 32, 324],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        0, 36,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 3587,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 10,
        leftMax: 40,
        leftMin: 0,
        rightInterval: 1000,
        rightMax: 4000,
        rightMin: 0,
      });
    });

    test(`Get chart y axis interval, datas typeof number`, () => {
      const columns = [
        ['stephen', -90, 90],
        ['jack', -100, 102],
        ['tom', -102, 105],
        ['john', -98, 95],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        -102, 0,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 105,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 20,
        leftMax: 120,
        leftMin: -120,
        rightInterval: 20,
        rightMax: 120,
        rightMin: -120,
      });
    });

    test(`Get chart y axis interval, datas typeof number, undefined and null`, () => {
      const columns = [
        ['stephen', undefined, 90],
        ['jack', -100, 102],
        ['tom', -102, null],
        ['john', -98, 95],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        -102, 0,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 102,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 20,
        leftMax: 120,
        leftMin: -120,
        rightInterval: 20,
        rightMax: 120,
        rightMin: -120,
      });
    });

    test(`Get chart y axis interval, datas has NaN`, () => {
      const columns = [
        ['stephen', NaN, 90],
        ['jack', -100, 102],
        ['tom', -102, NaN],
        ['john', -98, 95],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        -102, 0,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 102,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 20,
        leftMax: 120,
        leftMin: -120,
        rightInterval: 20,
        rightMax: 120,
        rightMin: -120,
      });
    });

    test(`Get chart y axis interval, datas has Infinity`, () => {
      const columns = [
        ['stephen', -Infinity, 90],
        ['jack', -100, 102],
        ['tom', -102, Infinity],
        ['john', -98, 95],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        -102, 0,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 102,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 20,
        leftMax: 120,
        leftMin: -120,
        rightInterval: 20,
        rightMax: 120,
        rightMin: -120,
      });
    });
    test(`Get chart y axis interval, datas all undefined`, () => {
      const columns = [
        ['stephen', undefined, undefined],
        ['jack', undefined, undefined],
        ['tom', undefined, undefined],
        ['john', undefined, undefined],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }, { name: 'number' }];
      const rows: ChartDataSectionField[] = [
        {
          colName: 'name',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
        {
          colName: 'age',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
        {
          colName: 'number',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        { rows },
      ] as any);
      expect(getMinAndMaxNumber([rows[1]], chartDataSet as any)).toEqual([
        0, 0,
      ]);
      expect(getMinAndMaxNumber([rows[2]], chartDataSet as any)).toEqual([
        0, 0,
      ]);
      expect(
        getYAxisIntervalConfig([rows[1]], [rows[2]], chartDataSet as any),
      ).toEqual({
        leftInterval: 0.2,
        leftMax: 0,
        leftMin: 0,
        rightInterval: 0.2,
        rightMax: 0,
        rightMin: 0,
      });
    });
  });
});
