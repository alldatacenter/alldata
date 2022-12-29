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

import {
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
  RUNTIME_DATE_LEVEL_KEY,
} from 'app/constants';
import { ChartDataSetRow } from 'app/models/ChartDataSet';
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import {
  ChartDataConfig,
  ChartDataSectionField,
  ChartStyleConfig,
  FormatFieldAction,
} from 'app/types/ChartConfig';
import { IChartDataSet } from 'app/types/ChartDataSet';
import {
  clearRuntimeDateLevelFieldsInChartConfig,
  compareSelectedItems,
  getColorizeGroupSeriesColumns,
  getColumnRenderName,
  getDataColumnMaxAndMin2,
  getDrillableRows,
  getGridStyle,
  getReference2,
  getRuntimeComputedFields,
  getRuntimeDateLevelFields,
  getScatterSymbolSizeFn,
  getSelectedItemStyles,
  getSeriesTooltips4Polar2,
  getSeriesTooltips4Rectangular2,
  getSettingValue,
  getStyles,
  getStyleValue,
  getStyleValueByGroup,
  getUnusedHeaderRows,
  getValue,
  getValueByColumnKey,
  isMatchRequirement,
  setRuntimeDateLevelFieldsInChartConfig,
  toFormattedValue,
  transformToDataSet,
  valueFormatter,
} from '../chartHelper';

describe('Chart Helper ', () => {
  describe.each([
    [
      [
        { key: '1', value: 1 },
        { key: '2', value: 2 },
      ],
      ['1'],
      'value',
      1,
    ],
    [
      [
        { key: '1', value: 1 },
        { key: '2', other: 2 },
      ],
      ['1'],
      undefined,
      1,
    ],
    [
      [
        { key: '1', other: 1 },
        { key: '2', value: 2 },
      ],
      ['1'],
      'other',
      1,
    ],
    [
      [
        { key: '1', other: 1 },
        { key: '2', value: 2 },
      ],
      ['1'],
      'unknown',
      undefined,
    ],
    [
      [
        { key: '1', other: 1 },
        { key: '2', value: 2 },
      ],
      ['unknown'],
      'value',
      undefined,
    ],
    [
      [
        { key: '1', other: 1, rows: [{ key: '1-1', value: 11 }] },
        { key: '2', value: 2 },
      ],
      ['1', '1-1'],
      'other',
      undefined,
    ],
    [
      [
        { key: '2', value: 2 },
        {
          key: '1',
          value: 1,
          rows: [
            {
              key: '1-1',
              value: 11,
              rows: [],
            },
          ],
        },
      ],
      ['1', '1-1'],
      'value',
      11,
    ],
    [
      [
        { key: '2', value: 2 },
        {
          key: '1',
          value: 1,
          rows: [
            {
              key: '1-1',
              value: 11,
              rows: [
                {
                  key: '1-1-1',
                  value: 111,
                  rows: [
                    {
                      key: '1-1-1-1',
                      value: 1111,
                      rows: [],
                    },
                    {
                      key: '1-1-1-2',
                      value: 1112,
                      rows: [
                        {
                          key: '1-1-1-2-1',
                          value: 11121,
                        },
                      ],
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
      ['1', '1-1', '1-1-1', '1-1-1-2', '1-1-1-2-1'],
      'value',
      11121,
    ],
  ])('getValue Test - ', (configs, paths, targetKey, expected) => {
    test(`get key of ${targetKey} from configs with path ${paths?.toString()} to be ${expected}`, () => {
      expect(getValue(configs as any, paths, targetKey)).toBe(expected);
    });
  });

  describe.each([
    [
      [
        {
          key: '1',
          value: 1,
          rows: [
            {
              key: '1-1',
              value: 11,
            },
            {
              key: '1-2',
              value: 12,
            },
            {
              key: '1-3',
              value: 13,
            },
          ],
        },
        { key: '2', value: 2 },
      ],
      ['2'],
      ['1-1', '1-2'],
      [undefined, undefined],
    ],
    [
      [
        {
          key: '1',
          value: 1,
          rows: [
            {
              key: '1-1',
              value: 11,
            },
            {
              key: '1-2',
              value: 12,
            },
            {
              key: '1-3',
              value: 13,
            },
          ],
        },
        { key: '2', value: 2 },
      ],
      ['1'],
      ['1-1', '1-3'],
      [11, 13],
    ],
    [
      [
        {
          key: '1',
          value: 1,
          rows: [
            {
              key: '1-1',
              value: 11,
            },
            {
              key: '1-2',
              value: 12,
              rows: [
                {
                  key: '1-2-1',
                  value: 121,
                  rows: [
                    {
                      key: '1-2-1-1',
                      value: 1211,
                    },
                  ],
                },
                {
                  key: '1-2-2',
                  other: 122,
                },
                {
                  key: '1-2-3',
                  value: 123,
                },
              ],
            },
            {
              key: '1-3',
              value: 13,
            },
          ],
        },
        { key: '2', value: 2 },
      ],
      ['1', '1-2'],
      ['1-2-1', '1-2-2', '1-2-4'],
      [121, undefined, undefined],
    ],
  ])('getStyles Test - ', (configs, paths, targetKeys, expected) => {
    test(`get keys of ${targetKeys} from configs with path ${paths?.toString()} to be ${expected}`, () => {
      expect(getStyles(configs as any, paths, targetKeys)).toEqual(expected);
    });
  });

  describe('getColumnRenderName Test', () => {
    test('should get [unknown] string when field has no colName or aggregation', () => {
      expect(getColumnRenderName(undefined)).toEqual('[unknown]');
    });

    test('should get column render name by data field when there is no aggregation', () => {
      const field = {
        colName: 'a',
      } as any;
      expect(getColumnRenderName(field)).toEqual('a');
    });

    test('should get column render name by data field with aggregation', () => {
      const field = {
        colName: 'a',
        aggregate: 'SUM',
      } as any;
      expect(getColumnRenderName(field)).toEqual('SUM(a)');
    });

    test('should get alias name by data field when there is alias and colName', () => {
      const field = {
        alias: {
          name: 'some alias name',
        },
        colName: 'a',
        aggregate: 'SUM',
      } as any;
      expect(getColumnRenderName(field)).toEqual('some alias name');
    });
  });

  describe('isMatchRequirement Test', () => {
    test('should match meta requirement when no limition', () => {
      const meta = {
        requirements: [
          {
            group: null,
            aggregate: null,
          },
        ],
      } as any;
      const config = {
        datas: [{}],
      } as any;
      expect(isMatchRequirement(meta, config)).toBeTruthy();
    });

    test('should match meta requirement when only group have limition', () => {
      const meta = {
        requirements: [
          {
            group: 1,
            aggregate: null,
          },
        ],
      } as any;
      const config = {
        datas: [
          {
            type: 'group',
            required: true,
            rows: [
              {
                colName: 'category',
              },
            ],
          },
        ],
      } as any;
      expect(isMatchRequirement(meta, config)).toBeTruthy();
    });

    test('should match meta requirement when group and aggregate need more than one field', () => {
      const meta = {
        requirements: [
          {
            group: [1, 999],
            aggregate: [1, 999],
          },
        ],
      } as any;
      const config = {
        datas: [
          {
            type: 'group',
            required: true,
            rows: [
              {
                colName: 'category',
              },
            ],
          },
          {
            type: 'aggregate',
            required: true,
            rows: [
              {
                colName: 'amount',
              },
            ],
          },
        ],
      } as any;
      expect(isMatchRequirement(meta, config)).toBeTruthy();
    });

    test('should not match meta requirement when not match all requirement of fields', () => {
      const meta = {
        requirements: [
          {
            group: 1,
            aggregate: 2,
          },
        ],
      } as any;
      const config = {
        datas: [
          {
            type: 'group',
            required: true,
            rows: [
              {
                colName: 'category',
              },
            ],
          },
          {
            type: 'aggregate',
            required: true,
            rows: [
              {
                colName: 'amount',
              },
            ],
          },
        ],
      } as any;
      expect(isMatchRequirement(meta, config)).toBeFalsy();
    });
  });

  describe('getColorizeGroupSeriesColumns Test', () => {
    test('should group dataset', () => {
      const columns = [
        ['stephen', 'engineer', '36'],
        ['jack', 'sales', '28'],
        ['tom', 'engineer', '30'],
        ['john', 'sales', '32'],
      ];
      const metas = [
        { name: 'name' },
        { name: 'current(profession)' },
        { name: 'age' },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        {
          rows: [
            {
              colName: 'name',
            },
            {
              colName: 'profession',
              aggregate: 'current',
            },

            {
              colName: 'age',
            },
          ],
        },
      ] as any);

      expect(
        JSON.stringify(
          getColorizeGroupSeriesColumns(chartDataSet, {
            colName: 'profession',
            aggregate: 'current',
          } as any),
        ),
      ).toBe(
        JSON.stringify([
          {
            engineer: [
              ['stephen', 'engineer', '36'],
              ['tom', 'engineer', '30'],
            ],
          },
          {
            sales: [
              ['jack', 'sales', '28'],
              ['john', 'sales', '32'],
            ],
          },
        ]),
      );
    });
  });

  describe('transformToDataSet Test', () => {
    test('should get dataset model with ignore case compare', () => {
      const columns = [
        ['r1-c1-v', 'r1-c2-v'],
        ['r2-c1-v', 'r2-c2-v'],
      ];
      const metas = [{ name: 'name' }, { name: 'age' }];
      const chartDataSet = transformToDataSet(columns, metas);

      expect(chartDataSet?.length).toEqual(2);
      expect(chartDataSet[0] instanceof ChartDataSetRow).toBeTruthy();
      expect(chartDataSet[0].convertToObject()).toEqual({
        NAME: 'r1-c1-v',
        AGE: 'r1-c2-v',
      });
      expect(chartDataSet[1].convertToObject()).toEqual({
        NAME: 'r2-c1-v',
        AGE: 'r2-c2-v',
      });
      expect(chartDataSet[0].getCell({ colName: 'age' } as any)).toEqual(
        'r1-c2-v',
      );
      expect(chartDataSet[0].getFieldKey({ colName: 'age' } as any)).toEqual(
        'AGE',
      );
      expect(chartDataSet[0].getFieldIndex({ colName: 'age' } as any)).toEqual(
        1,
      );
      expect(chartDataSet[0].getCellByKey('age')).toEqual('r1-c2-v');
    });

    test('should get dataset model when meta have aggregation', () => {
      const columns = [['r1-c1-v', 'r1-c2-v']];
      const metas = [{ name: 'name' }, { name: 'AVG(age)' }];
      const chartDataSet = transformToDataSet(columns, metas);

      expect(chartDataSet?.length).toEqual(1);
      expect(chartDataSet[0] instanceof ChartDataSetRow).toBeTruthy();
      expect(chartDataSet[0].convertToObject()).toEqual({
        NAME: 'r1-c1-v',
        'AVG(AGE)': 'r1-c2-v',
      });
      expect(
        chartDataSet[0].getCell({ colName: 'age', aggregate: 'AVG' } as any),
      ).toEqual('r1-c2-v');
      expect(
        chartDataSet[0].getFieldKey({
          colName: 'age',
          aggregate: 'AVG',
        } as any),
      ).toEqual('AVG(AGE)');
      expect(
        chartDataSet[0].getFieldIndex({
          colName: 'age',
          aggregate: 'AVG',
        } as any),
      ).toEqual(1);
      expect(chartDataSet[0].getCellByKey('AVG(age)')).toEqual('r1-c2-v');
    });

    test('should get dataset row data with case sensitive', () => {
      const columns = [['r1-c1-v', 'r1-c2-v']];
      const metas = [{ name: 'name' }, { name: 'avg(age)' }];
      const chartDataSet = transformToDataSet(columns, metas, [
        {
          rows: [
            {
              colName: 'Name',
            },
            {
              colName: 'Age',
              aggregate: 'AVG',
            },
          ],
        },
      ] as any);

      expect(chartDataSet?.length).toEqual(1);
      expect(chartDataSet[0] instanceof ChartDataSetRow).toBeTruthy();
      expect(chartDataSet[0].convertToCaseSensitiveObject()).toEqual({
        Name: 'r1-c1-v',
        'AVG(Age)': 'r1-c2-v',
      });
    });
  });

  describe('toFormattedValue Test', () => {
    describe.each([
      [
        2000,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            prefix: 'a',
            suffix: 'b',
          },
        },
        'a2.000Kb',
      ],
      [
        1111,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 3,
            unitKey: 'none',
            useThousandSeparator: true,
            prefix: '',
            suffix: 'b',
          },
        },
        '1,111.000b',
      ],
      [
        1111,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 0,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: 'b',
          },
        },
        '1111b',
      ],
      [
        3332,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 1,
            unitKey: 'none',
            useThousandSeparator: true,
            prefix: '',
            suffix: '',
          },
        },
        '3,332.0',
      ],
      [
        3322,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: -1,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '3322',
      ],
      [
        3333,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 1000,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '3333',
      ],
      [
        '3232a',
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 10,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '3232a',
      ],
      [
        11,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: undefined,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '11',
      ],
      [
        12,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 'null',
            unitKey: undefined,
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '12',
      ],
      [
        13,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: NaN,
            unitKey: NaN,
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '13',
      ],
      [
        0,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: 0,
            unitKey: 'none',
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        '0',
      ],
      [
        NaN,
        {
          type: 'numeric',
          numeric: {
            decimalPlaces: NaN,
            unitKey: NaN,
            useThousandSeparator: false,
            prefix: '',
            suffix: '',
          },
        },
        NaN,
      ],
    ])('toFormattedValue Test - numeric', (value, format, expected) => {
      test(`format aggregate data`, () => {
        expect(toFormattedValue(value, format as FormatFieldAction)).toEqual(
          expected,
        );
      });
    });

    describe.each([
      [
        3,
        {
          type: 'currency',
          currency: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            currency: 'CNY',
          },
        },
        '¥0.003 K',
      ],
      [
        NaN,
        {
          type: 'currency',
          currency: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            currency: 'CNY',
          },
        },
        NaN,
      ],
    ])('toFormattedValue Test - currency', (value, format, expected) => {
      test(`format aggregate data`, () => {
        expect(toFormattedValue(value, format as FormatFieldAction)).toEqual(
          expected,
        );
      });
    });

    describe.each([
      [1, undefined, 1],
      [
        4,
        {
          type: 'percentage',
          percentage: {
            decimalPlaces: 2,
          },
        },
        '400.00%',
      ],
      [
        NaN,
        {
          type: 'percentage',
          percentage: {
            decimalPlaces: 2,
          },
        },
        NaN,
      ],
      [
        1,
        {
          type: 'percentage',
          percentage: {
            decimalPlaces: 20,
          },
        },
        `100.00000000000000000000%`,
      ],
      [
        1,
        {
          type: 'percentage',
          percentage: {
            decimalPlaces: 99,
          },
        },
        `100%`,
      ],
      [
        50,
        {
          type: 'scientificNotation',
          scientificNotation: {
            decimalPlaces: 2,
          },
        },
        `5.00e+1`,
      ],
      [
        55,
        {
          type: 'scientificNotation',
          scientificNotation: {
            decimalPlaces: 3,
          },
        },
        `5.500e+1`,
      ],
      [
        NaN,
        {
          type: 'scientificNotation',
          scientificNotation: {
            decimalPlaces: 3,
          },
        },
        NaN,
      ],
      [
        '20130208',
        {
          type: 'date',
          date: {
            format: 'YYYY-MM-DD',
          },
        },
        `2013-02-08`,
      ],
      [
        '2013-02-08 00:00:00',
        {
          type: 'date',
          date: {
            format: 'YYYY-MM-DD',
          },
        },
        `2013-02-08 00:00:00`,
      ],
      [
        3,
        {
          type: '',
          currency: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            currency: 'CNY',
          },
        },
        3,
      ],
      [
        '2022-03-01',
        {
          type: 'string',
          currency: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            currency: 'CNY',
          },
        },
        '2022-03-01',
      ],
      [
        '3',
        {
          type: 'string',
          currency: {
            decimalPlaces: 3,
            unitKey: 'thousand',
            useThousandSeparator: true,
            currency: 'CNY',
          },
        },
        '3',
      ],
    ])('toFormattedValue Test - other', (value, format, expected) => {
      test(`format aggregate data`, () => {
        expect(toFormattedValue(value, format as FormatFieldAction)).toEqual(
          expected,
        );
      });
    });
  });

  describe.each([
    [undefined, undefined, `[unknown]: -`],
    [
      {
        alias: {
          name: 'aa',
        },
        aggregate: 'SUM',
        colName: 'name',
        type: 'STRING',
        category: 'field',
      },
      undefined,
      `aa: -`,
    ],
    [
      {
        alias: {
          name: 'bb',
        },
        aggregate: '',
        colName: 'name',
        type: 'STRING',
        category: 'field',
      },
      undefined,
      `bb: -`,
    ],
    [
      {
        aggregate: '',
        colName: 'name',
        type: 'STRING',
        category: 'field',
      },
      55,
      `name: 55`,
    ],
    [
      {
        aggregate: 'SUM',
        colName: 'name',
        type: 'STRING',
        category: 'field',
      },
      55,
      `SUM(name): 55`,
    ],
    [
      {
        format: {
          type: 'scientificNotation',
          scientificNotation: {
            decimalPlaces: 3,
          },
        },
        aggregate: 'SUM',
        colName: 'name',
        type: 'STRING',
        category: 'field',
      },
      55,
      `SUM(name): 5.500e+1`,
    ],
  ])('valueFormatter Test - ', (config, value, expected) => {
    test(`Get chart render string with field name and value`, () => {
      expect(valueFormatter(config as ChartDataSectionField, value)).toEqual(
        expected,
      );
    });
  });

  describe('getValueByColumnKey Test', () => {
    describe.each([
      [undefined, ''],
      [
        {
          aggregate: undefined,
          colName: 'a',
        },
        'a',
      ],
      [
        {
          aggregate: null,
          colName: 'b',
        },
        'b',
      ],
      [
        {
          aggregate: undefined,
          colName: '',
        },
        '',
      ],
      [
        {
          aggregate: 'SUM',
          colName: '',
        },
        'SUM()',
      ],
      [
        {
          aggregate: 'SUM',
          colName: 'c',
        },
        'SUM(c)',
      ],
    ])('getValueByColumnKey Test - ', (config, expected) => {
      test(`Get column key by data config`, () => {
        expect(getValueByColumnKey(config)).toEqual(expected);
      });
    });
  });

  describe.each([
    [
      'constant',
      true,
      false,
      {
        markLine: {
          data: [
            {
              yAxis: 0,
              label: {
                show: true,
                position: 'start',
                fontFamily: 'PingFang SC',
                fontSize: '12',
                fontWeight: 'normal',
                fontStyle: 'normal',
                color: 'black',
              },
              lineStyle: {
                type: 'solid',
                width: 1,
                color: 'blue',
              },
            },
          ],
        },
        markArea: {
          data: [
            [
              {
                yAxis: 0,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
              {
                yAxis: 10,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
            ],
          ],
        },
      },
    ],
    [
      'constant',
      true,
      true,
      {
        markLine: {
          data: [
            {
              xAxis: 0,
              label: {
                show: true,
                position: 'start',
                fontFamily: 'PingFang SC',
                fontSize: '12',
                fontWeight: 'normal',
                fontStyle: 'normal',
                color: 'black',
              },
              lineStyle: {
                type: 'solid',
                width: 1,
                color: 'blue',
              },
            },
          ],
        },
        markArea: {
          data: [
            [
              {
                xAxis: 0,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
              {
                xAxis: 10,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
            ],
          ],
        },
      },
    ],
    [
      'average',
      true,
      false,
      {
        markLine: {
          data: [
            {
              yAxis: 25,
              label: {
                show: true,
                position: 'start',
                fontFamily: 'PingFang SC',
                fontSize: '12',
                fontWeight: 'normal',
                fontStyle: 'normal',
                color: 'black',
              },
              lineStyle: {
                type: 'solid',
                width: 1,
                color: 'blue',
              },
            },
          ],
        },
        markArea: {
          data: [
            [
              {
                yAxis: 0,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
              {
                yAxis: 25,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
            ],
          ],
        },
      },
    ],
    [
      'max',
      true,
      false,
      {
        markLine: {
          data: [
            {
              yAxis: 30,
              label: {
                show: true,
                position: 'start',
                fontFamily: 'PingFang SC',
                fontSize: '12',
                fontWeight: 'normal',
                fontStyle: 'normal',
                color: 'black',
              },
              lineStyle: {
                type: 'solid',
                width: 1,
                color: 'blue',
              },
            },
          ],
        },
        markArea: {
          data: [
            [
              {
                yAxis: 0,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
              {
                yAxis: 30,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
            ],
          ],
        },
      },
    ],
    [
      'min',
      true,
      false,
      {
        markLine: {
          data: [
            {
              yAxis: 20,
              label: {
                show: true,
                position: 'start',
                fontFamily: 'PingFang SC',
                fontSize: '12',
                fontWeight: 'normal',
                fontStyle: 'normal',
                color: 'black',
              },
              lineStyle: {
                type: 'solid',
                width: 1,
                color: 'blue',
              },
            },
          ],
        },
        markArea: {
          data: [
            [
              {
                yAxis: 0,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
              {
                yAxis: 20,
                label: {
                  show: true,
                  position: 'start',
                  fontFamily: 'PingFang SC',
                  fontSize: '12',
                  fontWeight: 'normal',
                  fontStyle: 'normal',
                  color: 'black',
                },
                itemStyle: {
                  opacity: 0.6,
                  color: 'grey',
                  borderColor: 'grey',
                  borderWidth: 1,
                  borderType: 'dashed',
                },
              },
            ],
          ],
        },
      },
    ],
    [
      'min',
      false,
      false,
      {
        markLine: {
          data: [],
        },
        markArea: {
          data: [],
        },
      },
    ],
  ])('getReference2 Test - ', (valueType, show, isHorizonDisplay, expected) => {
    const style = [
      {
        key: 'reference',
        rows: [
          {
            key: 'panel',
            rows: [
              {
                key: 'configuration',
                rows: [
                  {
                    key: 'a6fa3454',
                    rows: [
                      {
                        key: 'markLine',
                        rows: [
                          {
                            key: 'enableMarkLine',
                            value: show,
                          },
                          {
                            key: 'valueType',
                            value: valueType,
                          },
                          {
                            key: 'constantValue',
                            value: 0,
                          },
                          {
                            key: 'metric',
                            value: 'a6fa3454',
                          },
                          {
                            key: 'showLabel',
                            value: true,
                          },
                          {
                            key: 'position',
                            value: 'start',
                          },
                          {
                            key: 'lineStyle',
                            value: { type: 'solid', width: 1, color: 'blue' },
                          },
                          {
                            key: 'font',
                            value: {
                              fontFamily: 'PingFang SC',
                              fontSize: '12',
                              fontWeight: 'normal',
                              fontStyle: 'normal',
                              color: 'black',
                            },
                          },
                        ],
                      },
                      {
                        key: 'markArea',
                        rows: [
                          {
                            key: 'enableMarkArea',
                            value: show,
                          },
                          {
                            key: 'startValueType',
                            value: 'constant',
                          },
                          {
                            key: 'startConstantValue',
                            value: 0,
                          },
                          {
                            key: 'startMetric',
                            value: undefined,
                          },
                          {
                            key: 'endValueType',
                            value: valueType,
                          },
                          {
                            key: 'endConstantValue',
                            value: 10,
                          },
                          {
                            key: 'endMetric',
                            value: 'a6fa3454',
                          },
                          {
                            key: 'showLabel',
                            value: true,
                          },
                          {
                            key: 'position',
                            value: 'start',
                          },
                          {
                            key: 'font',
                            value: {
                              fontFamily: 'PingFang SC',
                              fontSize: '12',
                              fontWeight: 'normal',
                              fontStyle: 'normal',
                              color: 'black',
                            },
                          },
                          {
                            key: 'backgroundColor',
                            value: 'grey',
                          },
                          {
                            key: 'opacity',
                            value: 0.6,
                          },
                          {
                            key: 'borderStyle',
                            value: { type: 'dashed', width: 1, color: 'grey' },
                          },
                        ],
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
      },
    ];
    test(`Get chart reference config`, () => {
      const config = [
        {
          key: 'dimension',
          rows: [
            {
              category: 'field',
              colName: 'name',
              type: 'STRING',
              uid: '',
            },
          ],
        },
        {
          key: 'metrics',
          rows: [
            {
              aggregate: 'SUM',
              category: 'field',
              colName: 'num',
              type: 'NUMERIC',
              uid: 'a6fa3454',
            },
          ],
        },
      ] as ChartDataConfig[];
      const chartDataSet = transformToDataSet(
        [
          [30, 'name1'],
          [20, 'name2'],
        ],
        [
          {
            name: 'SUM(num)',
            type: 'NUMERIC',
          },
          {
            name: 'name',
            type: 'STRING',
          },
        ],
        config,
      );
      expect(
        JSON.stringify(
          getReference2(
            style as ChartStyleConfig[],
            chartDataSet as IChartDataSet<string>,
            config[1]!.rows![0] as ChartDataSectionField,
            isHorizonDisplay,
          ),
        ),
      ).toEqual(JSON.stringify(expected));
    });
  });

  describe.each([
    [
      [
        { colName: 'name1' },
        { colName: 'name2' },
        { colName: 'name3' },
        { colName: 'name4' },
        { colName: 'name5' },
        { colName: 'name6' },
        { colName: 'name7' },
      ],
      [
        {
          colName: 'a',
          children: [
            {
              colName: 'name1',
            },
            {
              colName: 'name2',
              children: [
                {
                  colName: 'name3',
                },
                {
                  colName: 'name4',
                },
              ],
            },
          ],
        },
        {
          colName: 'name5',
        },
      ],
      [{ colName: 'name6' }, { colName: 'name7' }],
    ],
    [
      [
        { colName: 'name7' },
        { colName: 'name6' },
        { colName: 'name5' },
        { colName: 'name4' },
        { colName: 'name3' },
        { colName: 'name2' },
        { colName: 'name1' },
      ],
      [
        {
          colName: 'a',
          children: [
            {
              colName: 'name1',
            },
            {
              colName: 'name2',
              children: [
                {
                  colName: 'name3',
                },
                {
                  colName: 'name4',
                },
              ],
            },
          ],
        },
        {
          colName: 'name5',
        },
      ],
      [{ colName: 'name7' }, { colName: 'name6' }],
    ],
    [
      [
        { colName: 'name1' },
        { colName: 'name2' },
        { colName: 'name3' },
        { colName: 'name4' },
        { colName: 'name5' },
        { colName: 'name6' },
        { colName: 'name7' },
      ],
      [
        {
          colName: 'a',
          isGroup: true,
          children: [
            {
              colName: 'name1',
            },
            {
              colName: 'name2',
              isGroup: true,
              children: [
                {
                  colName: 'name3',
                },
                {
                  colName: 'name4',
                },
              ],
            },
          ],
        },
        {
          colName: 'name5',
        },
      ],
      [{ colName: 'name2' }, { colName: 'name6' }, { colName: 'name7' }],
    ],
  ])('getUnusedHeaderRows Test - ', (allRows, originalRows, expected) => {
    test(`Get table header rows `, () => {
      expect(
        JSON.stringify(getUnusedHeaderRows(allRows, originalRows)),
      ).toEqual(JSON.stringify(expected));
    });
  });

  describe.each([
    [
      [['5'], ['3'], ['-10'], ['999']],
      {
        colName: 'num',
        aggregate: 'SUM',
        type: 'STRING',
        category: 'field',
      },
      {
        min: -10,
        max: 999,
      },
    ],
    [
      [['null'], ['3'], ['99']],
      {
        colName: 'num',
        aggregate: 'SUM',
        type: 'STRING',
        category: 'field',
      },
      {
        min: 0,
        max: 100,
      },
    ],
    [
      [['null'], ['3'], ['990']],
      null,
      {
        min: 0,
        max: 100,
      },
    ],
  ])('getDataColumnMaxAndMin2 Test - ', (data, config, expected) => {
    test(`Get column max and min value`, () => {
      const chartDataSet = transformToDataSet(data, [{ name: 'sum(num)' }], [
        {
          rows: [
            {
              colName: 'num',
              aggregate: 'SUM',
            },
          ],
        },
      ] as any);
      expect(
        JSON.stringify(
          getDataColumnMaxAndMin2(
            chartDataSet as IChartDataSet<string>,
            config as ChartDataSectionField,
          ),
        ),
      ).toEqual(JSON.stringify(expected));
    });
  });

  describe.each([
    [0, 100, 0, 0, [100], 20],
    [0, 100, 0, 2, [100], 40],
    [1, 999, -30, 0, [0, 100], 3],
    [1, 999, -30, 0, [0, 800], 16.132167152575317],
  ])(
    'getScatterSymbolSizeFn Test - ',
    (valueIndex, max, min, cycleRatio, value, expected) => {
      test(`Get some scatter symbol size`, () => {
        const symbolSizeFunc = getScatterSymbolSizeFn(
          valueIndex,
          max,
          min,
          cycleRatio,
        );
        expect(symbolSizeFunc(value)).toEqual(expected);
      });
    },
  );

  describe('getSeriesTooltips4Rectangular2 Test', () => {
    test(`Get series tooltips`, () => {
      const columns = [['r1-c1-v', 'r1-c2-v', '#fff', '10', '20']];
      const metas = [
        { name: 'name' },
        { name: 'avg(age)' },
        { name: 'color' },
        { name: 'sum(info)' },
        { name: 'count(size)' },
      ];
      const rows = [
        {
          category: 'field',
          type: 'STRING',
          colName: 'Name',
        },
        {
          colName: 'Age',
          aggregate: 'AVG',
          type: 'STRING',
          category: 'field',
        },
        {
          colName: 'Color',
          type: 'STRING',
          category: 'field',
        },
        {
          colName: 'Info',
          aggregate: 'SUM',
          type: 'NUMERIC',
          category: 'field',
        },
        {
          colName: 'Size',
          aggregate: 'COUNT',
          type: 'NUMERIC',
          category: 'field',
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        {
          rows,
        },
      ] as any);

      expect(
        getSeriesTooltips4Rectangular2(
          chartDataSet,
          {
            seriesName: 'b',
            componentType: '',
            data: {
              name: 'a',
              rowData: {
                Name: 'r1-c1-v',
                Color: '#fff',
                'AVG(Age)': 'r1-c2-v',
                'SUM(Info)': '10',
                'COUNT(Size)': '20',
              },
            },
          },
          [rows[0]] as ChartDataSectionField[],
          [rows[2]] as ChartDataSectionField[],
          [rows[1]] as ChartDataSectionField[],
          [rows[3]] as ChartDataSectionField[],
          [rows[4]] as ChartDataSectionField[],
        ),
      ).toEqual('');

      expect(
        getSeriesTooltips4Rectangular2(
          chartDataSet,
          {
            seriesName: '',
            componentType: 'series',
            data: {
              name: 'AVG(Age)',
              rowData: {
                Name: 'r1-c1-v',
                Color: '#fff',
                'AVG(Age)': 'r1-c2-v',
                'SUM(Info)': '10',
                'COUNT(Size)': '20',
              },
            },
          },
          [rows[0]] as ChartDataSectionField[],
          [rows[2]] as ChartDataSectionField[],
          rows as ChartDataSectionField[],
          [rows[3]] as ChartDataSectionField[],
          [rows[4]] as ChartDataSectionField[],
        ),
      ).toEqual(
        'Name: r1-c1-v<br />Color: #fff<br />AVG(Age): r1-c2-v<br />COUNT(Size): 20<br />SUM(Info): 10',
      );

      expect(
        getSeriesTooltips4Rectangular2(
          chartDataSet,
          {
            seriesName: 'Name',
            componentType: 'series',
            data: {
              name: '',
              rowData: {
                Name: 'r1-c1-v',
                Color: '#fff',
                'AVG(Age)': 'r1-c2-v',
                'SUM(Info)': '10',
                'COUNT(Size)': '20',
              },
            },
          },
          [rows[0]] as ChartDataSectionField[],
          [rows[2]] as ChartDataSectionField[],
          rows as ChartDataSectionField[],
          [rows[3]] as ChartDataSectionField[],
          [rows[4]] as ChartDataSectionField[],
        ),
      ).toEqual(
        'Name: r1-c1-v<br />Color: #fff<br />Name: r1-c1-v<br />COUNT(Size): 20<br />SUM(Info): 10',
      );
    });
  });

  describe('getSeriesTooltips4Polar2 Test', () => {
    test(`Get series tooltips`, () => {
      const columns = [['r1-c1-v', 'r1-c2-v', '#fff', '10', '20']];
      const metas = [
        { name: 'name' },
        { name: 'avg(age)' },
        { name: 'color' },
        { name: 'sum(info)' },
        { name: 'count(size)' },
      ];
      const rows = [
        {
          category: 'field',
          type: 'STRING',
          colName: 'Name',
        },
        {
          colName: 'Age',
          aggregate: 'AVG',
          type: 'STRING',
          category: 'field',
        },
        {
          colName: 'Color',
          type: 'STRING',
          category: 'field',
        },
        {
          colName: 'Info',
          aggregate: 'SUM',
          type: 'NUMERIC',
          category: 'field',
        },
        {
          colName: 'Size',
          aggregate: 'COUNT',
          type: 'NUMERIC',
          category: 'field',
        },
      ];
      const chartDataSet = transformToDataSet(columns, metas, [
        {
          rows,
        },
      ] as any);
      const tooltipParam = {
        data: {
          name: 'string',
          rowData: {
            Name: 'r1-c1-v',
            Color: '#fff',
            'AVG(Age)': 'r1-c2-v',
            'SUM(Info)': '10',
            'COUNT(Size)': '20',
          },
        },
      };

      expect(
        getSeriesTooltips4Polar2(
          chartDataSet,
          tooltipParam,
          [rows[0]] as ChartDataSectionField[],
          [rows[2]] as ChartDataSectionField[],
          [rows[1]] as ChartDataSectionField[],
          [rows[3]] as ChartDataSectionField[],
          [rows[4]] as ChartDataSectionField[],
        ),
      ).toEqual(
        'Name: r1-c1-v<br />Color: #fff<br />AVG(Age): r1-c2-v<br />COUNT(Size): 20<br />SUM(Info): 10',
      );
    });
  });

  describe('getGridStyle Test', () => {
    describe.each([
      [
        [
          {
            key: 'margin',
            rows: [
              {
                key: 'containLabel',
                value: true,
              },
              {
                key: 'marginLeft',
                value: '5%',
              },
              {
                key: 'marginRight',
                value: '5%',
              },
              {
                key: 'marginBottom',
                value: '5%',
              },
              {
                key: 'marginTop',
                value: '5%',
              },
            ],
          },
        ],
        {
          left: '5%',
          right: '5%',
          bottom: '5%',
          top: '5%',
          containLabel: true,
        },
      ],
    ])('getGridStyle Test - ', (data, expected) => {
      test(`Get grid margin config`, () => {
        expect(
          JSON.stringify(getGridStyle(data as ChartStyleConfig[])),
        ).toEqual(JSON.stringify(expected));
      });
    });
  });

  describe('getStyleValue Test', () => {
    test('should get value', () => {
      expect(
        getStyleValue([{ key: 'a', rows: [{ key: 'a-1', value: 1 }] }] as any, [
          'a',
          'a-1',
        ]),
      ).toEqual(1);
    });
  });

  describe('getSettingValue Test', () => {
    test('should get value', () => {
      expect(
        getSettingValue(
          [{ key: 'a', rows: [{ key: 'a-1', value: 1 }] }] as any,
          'a.a-1',
          'value',
        ),
      ).toEqual(1);
    });
  });

  describe('getStyleValueByGroup Test', () => {
    test('should get value', () => {
      expect(
        getStyleValueByGroup(
          [{ key: 'a', rows: [{ key: 'a-1', value: 1 }] }] as any,
          'a',
          'a-1',
        ),
      ).toEqual(1);
    });
  });

  describe('getDrillableRows Test', () => {
    test('should get group section rows when drill option is null', () => {
      const config = [
        {
          type: ChartDataSectionType.Group,
          rows: [1, 2],
        },
      ] as any[];
      const drillRows = getDrillableRows(config, undefined);
      expect(drillRows).toEqual([1, 2]);
    });

    test('should not get group section rows when is not group section', () => {
      const config = [
        {
          type: ChartDataSectionType.Aggregate,
          rows: [1, 2],
        },
      ] as any[];
      const drillRows = getDrillableRows(config, undefined);
      expect(drillRows).toEqual([]);
    });

    test('should get group section rows when drillale is false and drillOption is not empty', () => {
      const config = [
        {
          type: ChartDataSectionType.Group,
          key: 'col1',
          rows: [
            {
              uid: '1',
              colName: 'col1',
              id: '["col1"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
            {
              uid: '2',
              colName: 'col2',
              id: '["col2"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
          ],
        },
      ];
      const drillOption = new ChartDrillOption(config[0].rows);
      const drillRows = getDrillableRows(config, drillOption);
      expect(drillRows).toEqual([
        {
          uid: '1',
          colName: 'col1',
          type: 'STRING',
          category: 'field',
          id: '["col1"]',
        },
        {
          uid: '2',
          colName: 'col2',
          type: 'STRING',
          category: 'field',
          id: '["col2"]',
        },
      ]);
    });

    test('should get drillable group section rows with drillOption', () => {
      const config = [
        {
          type: ChartDataSectionType.Group,
          key: 'col1',
          drillable: true,
          rows: [
            {
              uid: '1',
              colName: 'col1',
              id: '["col1"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
            {
              uid: '2',
              colName: 'col2',
              id: '["col2"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
          ],
        },
      ];
      const drillOption = new ChartDrillOption(config[0].rows);
      drillOption.drillDown();
      const drillRows = getDrillableRows(config, drillOption);
      expect(drillRows).toEqual([
        {
          uid: '2',
          colName: 'col2',
          type: 'STRING',
          category: 'field',
          id: '["col2"]',
        },
      ]);
    });

    test('should get drillable group section first row without drillOption', () => {
      const config = [
        {
          type: ChartDataSectionType.Group,
          key: 'col1',
          drillable: true,
          rows: [
            {
              uid: '1',
              colName: 'col1',
              id: '["col1"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
            {
              uid: '2',
              colName: 'col2',
              id: '["col2"]',
              type: DataViewFieldType.STRING,
              category: ChartDataViewFieldCategory.Field,
            },
          ],
        },
      ];
      const drillOption = new ChartDrillOption(config[0].rows);
      const drillRows = getDrillableRows(config, drillOption);
      expect(drillRows).toEqual([
        {
          uid: '1',
          colName: 'col1',
          type: 'STRING',
          category: 'field',
          id: '["col1"]',
        },
      ]);
    });
  });

  describe('getRuntimeDateLevelFields Test', () => {
    test('test have RUNTIME_DATE_LEVEL_KEY', () => {
      const config = [
        {
          [RUNTIME_DATE_LEVEL_KEY]: { name: 'lyp', age: '12', sex: 'male' },
        },
        {},
      ];
      expect(getRuntimeDateLevelFields(config)).toEqual([
        { name: 'lyp', age: '12', sex: 'male' },
        {},
      ]);
    });

    test('test does not contain RUNTIME_DATE_LEVEL_KEY', () => {
      const config = [
        {
          name: 'liutao',
          age: '22',
          sex: 'Female',
        },
        { name: 'dilireba', age: '23', sex: 'Female' },
      ];
      expect(getRuntimeDateLevelFields(config)).toEqual(config);
    });
  });

  describe('getRuntimeComputedFields Test', () => {
    test('Test to modify the first runtime date level', () => {
      const dateLevelComputedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '签署日期（按周）',
          expression: 'AGG_DATE_WEEK(签署日期)',
          field: '签署日期',
          type: DataViewFieldType.DATE,
          uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
          [RUNTIME_DATE_LEVEL_KEY]: null,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '合同截止日期（按月）',
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          field: '合同截止日期',
          type: DataViewFieldType.DATE,
          uid: 'fe3f3810-7fe1-41dc-b745-298aaa8b4b95',
        },
      ];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        colName: '签署日期（按月）',
        expression: 'AGG_DATE_MONTH(签署日期)',
        field: '签署日期',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
        [RUNTIME_DATE_LEVEL_KEY]: {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '签署日期（按周）',
          expression: 'AGG_DATE_WEEK(签署日期)',
          field: '签署日期',
          type: DataViewFieldType.DATE,
          uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
          [RUNTIME_DATE_LEVEL_KEY]: null,
        },
      };

      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];
      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
          true,
        ),
      ).toEqual([
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
          [RUNTIME_DATE_LEVEL_KEY]: {
            category: ChartDataViewFieldCategory.DateLevelComputedField,
            expression: 'AGG_DATE_WEEK(签署日期)',
            name: '签署日期（按周）',
            type: DataViewFieldType.DATE,
          },
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ]);
    });

    test('Test to modify the second runtime date level', () => {
      const dateLevelComputedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '签署日期（按月）',
          expression: 'AGG_DATE_MONTH(签署日期)',
          field: '签署日期',
          type: DataViewFieldType.DATE,
          uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
          [RUNTIME_DATE_LEVEL_KEY]: null,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '合同截止日期（按周）',
          expression: 'AGG_DATE_WEEK(合同截止日期)',
          field: '合同截止日期',
          type: DataViewFieldType.DATE,
          uid: 'fe3f3810-7fe1-41dc-b745-298aaa8b4b95',
        },
      ];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        colName: '合同截止日期（按月）',
        expression: 'AGG_DATE_MONTH(合同截止日期)',
        field: '合同截止日期',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
      };

      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];
      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
          true,
        ),
      ).toEqual([
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
          [RUNTIME_DATE_LEVEL_KEY]: {
            category: ChartDataViewFieldCategory.DateLevelComputedField,
            expression: 'AGG_DATE_WEEK(合同截止日期)',
            name: '合同截止日期（按周）',
            type: DataViewFieldType.DATE,
          },
        },
      ]);
    });

    test('Test select default date level', () => {
      const dateLevelComputedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '合同截止日期（按周）',
          expression: 'AGG_DATE_WEEK(合同截止日期)',
          field: '合同截止日期',
          type: DataViewFieldType.DATE,
          uid: 'fe3f3810-7fe1-41dc-b745-298aaa8b4b95',
        },
      ];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        colName: '签署日期（按月）',
        expression: 'AGG_DATE_MONTH(签署日期)',
        field: '签署日期',
        id: '签署日期（按月）',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
      };

      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];
      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
          true,
        ),
      ).toEqual([
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ]);
    });

    test('Test from default level to other date levels for default', () => {
      const dateLevelComputedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '合同截止日期（按周）',
          expression: 'AGG_DATE_WEEK(合同截止日期)',
          field: '合同截止日期',
          type: DataViewFieldType.DATE,
          uid: 'fe3f3810-7fe1-41dc-b745-298aaa8b4b95',
        },
      ];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.Field,
        colName: '签署日期',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
      };

      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];
      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
          true,
        ),
      ).toEqual([
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(合同截止日期)',
          name: '合同截止日期（按月）',
          type: DataViewFieldType.DATE,
        },
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_WEEK(合同截止日期)',
          name: '合同截止日期（按周）',
          type: DataViewFieldType.DATE,
        },
      ]);
    });

    test('Test are not run when modifying the date level', () => {
      const dateLevelComputedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          colName: '签署日期（按周）',
          expression: 'AGG_DATE_WEEK(签署日期)',
          field: '签署日期',
          type: DataViewFieldType.DATE,
          uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
        },
      ];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        colName: '签署日期（按月）',
        expression: 'AGG_DATE_MONTH(签署日期)',
        field: '签署日期',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
      };
      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];

      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
        ),
      ).toEqual([
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_WEEK(签署日期)',
          name: '签署日期（按周）',
          type: DataViewFieldType.DATE,
        },
      ]);
    });

    test('Test are not run when modifying the date level', () => {
      const dateLevelComputedFields = [];
      const replacedConfig = {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        colName: '签署日期（按月）',
        expression: 'AGG_DATE_MONTH(签署日期)',
        field: '签署日期',
        type: DataViewFieldType.DATE,
        uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
      };
      const computedFields = [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_MONTH(签署日期)',
          name: '签署日期（按月）',
          type: DataViewFieldType.DATE,
        },
      ];

      expect(
        getRuntimeComputedFields(
          dateLevelComputedFields,
          replacedConfig,
          computedFields,
        ),
      ).toEqual([]);
    });
  });

  describe('clearRuntimeDateLevelFieldsInChartConfig Test', () => {
    test('Clear all runtime state in chart config', () => {
      const chartConfig: any = {
        datas: [
          {
            drillable: true,
            key: 'dimension',
            label: 'dimension',
            limit: [0, 1],
            required: true,
            type: 'group',
            rows: [
              {
                category: 'dateLevelComputedField',
                colName: '签署日期（按月）',
                expression: 'AGG_DATE_MONTH(签署日期)',
                field: '签署日期',
                type: DataViewFieldType.DATE,
                uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
                [RUNTIME_DATE_LEVEL_KEY]: {
                  category: 'dateLevelComputedField',
                  colName: '签署日期（按季度）',
                  expression: 'AGG_DATE_QUARTER(签署日期)',
                  field: '签署日期',
                  type: 'DATE',
                  uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
                },
              },
            ],
          },
        ],
      };
      expect(clearRuntimeDateLevelFieldsInChartConfig(chartConfig)).toEqual({
        datas: [
          {
            drillable: true,
            key: 'dimension',
            label: 'dimension',
            limit: [0, 1],
            required: true,
            type: 'group',
            rows: [
              {
                category: 'dateLevelComputedField',
                colName: '签署日期（按月）',
                expression: 'AGG_DATE_MONTH(签署日期)',
                field: '签署日期',
                type: DataViewFieldType.DATE,
                uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
              },
            ],
          },
        ],
      });
    });
  });

  describe('setRuntimeDateLevelFieldsInChartConfig Test', () => {
    test('set all runtime state in chart config', () => {
      const chartConfig: any = {
        datas: [
          {
            drillable: true,
            key: 'dimension',
            label: 'dimension',
            limit: [0, 1],
            required: true,
            type: 'group',
            rows: [
              {
                category: 'dateLevelComputedField',
                colName: '签署日期（按月）',
                expression: 'AGG_DATE_MONTH(签署日期)',
                field: '签署日期',
                type: DataViewFieldType.DATE,
                uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
                [RUNTIME_DATE_LEVEL_KEY]: {
                  category: 'dateLevelComputedField',
                  colName: '签署日期（按季度）',
                  expression: 'AGG_DATE_QUARTER(签署日期)',
                  field: '签署日期',
                  type: DataViewFieldType.DATE,
                  uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
                },
              },
            ],
          },
        ],
      };
      expect(setRuntimeDateLevelFieldsInChartConfig(chartConfig)).toEqual({
        datas: [
          {
            drillable: true,
            key: 'dimension',
            label: 'dimension',
            limit: [0, 1],
            required: true,
            type: 'group',
            rows: [
              {
                category: 'dateLevelComputedField',
                colName: '签署日期（按季度）',
                expression: 'AGG_DATE_QUARTER(签署日期)',
                field: '签署日期',
                type: DataViewFieldType.DATE,
                uid: 'd8a3ca7e-7513-4b31-b09c-ea3611bc3c54',
              },
            ],
          },
        ],
      });
    });
  });

  describe.each([
    [
      1,
      1,
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: {} },
    ],
    [
      1,
      1,
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { color: '#FFF' } },
      {
        color: '#FFF',
      },
    ],
    [
      1,
      'name_level1',
      [
        {
          index: '1,name_level1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: {} },
    ],
    [
      1,
      'name_level1',
      [
        {
          index: '1,name_level1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { color: '#FFF' } },
      {
        color: '#FFF',
      },
    ],
    [
      0,
      1,
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { opacity: 0.5 } },
    ],
    [
      0,
      1,
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { color: '#FFF', opacity: 0.5 } },
      {
        color: '#FFF',
      },
    ],
    [
      0,
      'name_level1',
      [
        {
          index: '1,name_level1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { opacity: 0.5 } },
    ],
    [
      0,
      'name_level1',
      [
        {
          index: '1,name_level1',
          data: { rowData: {} },
        },
      ],
      { itemStyle: { color: '#FFF', opacity: 0.5 } },
      {
        color: '#FFF',
      },
    ],
  ])(
    'getSelectedItemStyles Test - ',
    (comIndex, dcIndex, selectionList, expected, itemStyle?) => {
      test(`Get select style`, () => {
        expect(
          JSON.stringify(
            getSelectedItemStyles(comIndex, dcIndex, selectionList, itemStyle),
          ),
        ).toEqual(JSON.stringify(expected));
      });
    },
  );

  describe.each([
    [[], [], false],
    [
      [
        {
          index: '1,2',
          data: { rowData: {} },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      true,
    ],
    [
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      false,
    ],
    [
      [
        {
          index: '1,1',
          data: { rowData: { a: 1 } },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      true,
    ],
    [
      [
        {
          index: '1,1',
          data: { rowData: { a: 1 } },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: { a: 1, b: 1 } },
        },
      ],
      true,
    ],
    [
      [
        {
          index: '1,2',
          data: { rowData: {} },
        },
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
        {
          index: '1,2',
          data: { rowData: {} },
        },
      ],
      false,
    ],
    [
      [
        {
          index: '1,2',
          data: { rowData: {} },
        },
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      [
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      true,
    ],
    [
      [
        {
          index: '1,2',
          data: { rowData: {} },
        },
        {
          index: '1,1',
          data: { rowData: {} },
        },
      ],
      undefined,
      true,
    ],
    [[], undefined, true],
  ])(
    'compareSelectedItems Test - ',
    (newSelectedItems, oldSelectedItems, expected) => {
      test(`Get compare whether update`, () => {
        expect(
          JSON.stringify(
            compareSelectedItems(newSelectedItems, oldSelectedItems),
          ),
        ).toEqual(JSON.stringify(expected));
      });
    },
  );
});
