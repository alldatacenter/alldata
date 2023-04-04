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
import { ChartDataViewFieldCategory } from 'app/constants';
import { DATE_LEVEL_DELIMITER } from 'globalConstants';
import migrateChartConfig, {
  RC0,
  RC2,
} from '../ChartConfig/migrateChartConfig';
import { APP_VERSION_RC_2 } from '../constants';

describe('test RC0 Function ', () => {
  test('should add name fields for chartConfig computedFields', () => {
    const chartConfig = {
      computedFields: [{ id: '1' }],
    } as any;
    const result = RC0(chartConfig as any);

    expect(result).toMatchObject({
      computedFields: [{ id: '1', name: '1' }],
    });
  });

  test('should not add name fields for chartConfig computedFields', () => {
    const chartConfig = {
      computedFields: [{ name: '1' }],
    } as any;
    const result = RC0(chartConfig as any);

    expect(result).toMatchObject({
      computedFields: [{ name: '1' }],
    });
  });

  test('should not computedFields for chartConfig', () => {
    const chartConfig = {} as any;
    const result = RC0(chartConfig as any);

    expect(result).toMatchObject({});
  });
  test('test chartConfig is null', () => {
    const chartConfig = {
      computedFields: '1111',
    } as any;
    const result = RC0(chartConfig as any);

    expect(result).toEqual(chartConfig);
  });
  test('test RCO Try Catch function', () => {
    const chartConfig = null as any;
    const result = RC0(chartConfig as any);

    expect(result).toEqual(null);
  });
});

describe('test migrateChartConfig  ', () => {
  test('test config is null', () => {
    const chartConfig: any = null;
    const result = migrateChartConfig(chartConfig);
    expect(result).toEqual(null);
  });

  test('should add name fields for chartConfig computedFields', () => {
    const chartConfig = JSON.stringify({
      computedFields: [{ id: '1' }],
    }) as any;
    const result = migrateChartConfig(chartConfig);

    expect(result).toEqual(
      JSON.stringify({
        computedFields: [{ id: '1', name: '1' }],
        version: APP_VERSION_RC_2,
      }),
    );
  });

  test('should not add name fields for chartConfig computedFields', () => {
    const chartConfig = JSON.stringify({
      computedFields: [{ name: '1' }],
    }) as any;
    const result = migrateChartConfig(chartConfig as any);

    expect(result).toEqual(
      JSON.stringify({
        computedFields: [{ name: '1' }],
        version: APP_VERSION_RC_2,
      }),
    );
  });

  test('should not computedFields for chartConfig', () => {
    const chartConfig = JSON.stringify({}) as any;
    const result = migrateChartConfig(chartConfig as any);

    expect(result).toEqual(JSON.stringify({ version: APP_VERSION_RC_2 }));
  });
});

describe('test RC2 Function ', () => {
  test('test if config if empty', () => {
    const config = null;
    const result = RC2(config);
    expect(result).toEqual(config);
  });

  test('should return correct colName', () => {
    const chartConfig = {
      chartConfig: {
        datas: [
          {
            rows: [
              {
                category: ChartDataViewFieldCategory.DateLevelComputedField,
                colName: 'birthday（Year）',
                expression: 'AGG_DATE_YEAR([birthday])',
                field: 'birthday',
              },
            ],
          },
        ],
      },
      computedFields: [
        {
          category: ChartDataViewFieldCategory.DateLevelComputedField,
          expression: 'AGG_DATE_YEAR([birthday])',
          name: 'birthday（Year）',
          type: 'DATE',
        },
      ],
    } as any;
    const result = RC2(chartConfig as any);

    expect(result.chartConfig.datas[0].rows[0].colName).toEqual(
      'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
    );
    expect(result.computedFields[0].name).toEqual(
      'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
    );
  });

  test('test try catch function', () => {
    const chartConfig = {
      chartConfig: {},
    } as any;
    const result = RC2(chartConfig as any);
    expect(result).toEqual(chartConfig);
  });
});
