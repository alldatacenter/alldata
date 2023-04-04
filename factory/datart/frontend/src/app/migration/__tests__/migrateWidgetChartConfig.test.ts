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
import migrateWidgetChartConfig from '../BoardConfig/migrateWidgetChartConfig';

describe('migrate Widget ChartConfig', () => {
  test('should return empty array if widget is empty ', () => {
    const widget = [];

    const result = migrateWidgetChartConfig(widget);
    expect(result).toEqual([]);
  });
  test('should return null if widget is null ', () => {
    const widget: any = null;

    const result = migrateWidgetChartConfig(widget);
    expect(result).toEqual([]);
  });

  test('should No processing of widgets', () => {
    const widget: any = [
      {
        config: {
          content: {
            dataChart: null,
          },
        },
      },
    ] as any;

    const result = migrateWidgetChartConfig(widget);
    expect(result).toEqual(widget);
  });

  test('should return array if widget is not empty ', () => {
    // widget?.config?.content?.dataChart?.config
    const widget = [
      {
        config: {
          content: {
            dataChart: {
              config: {
                chartConfig: {
                  datas: [
                    {
                      rows: [
                        {
                          category:
                            ChartDataViewFieldCategory.DateLevelComputedField,
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
              },
            },
          },
        },
      },
    ] as any;

    const result = migrateWidgetChartConfig(widget);
    expect(result[0].config.content.dataChart.config.chartConfig.datas).toEqual(
      [
        {
          rows: [
            {
              category: ChartDataViewFieldCategory.DateLevelComputedField,
              colName: 'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
              expression: 'AGG_DATE_YEAR([birthday])',
              field: 'birthday',
            },
          ],
        },
      ],
    );

    expect(result[0].config.content.dataChart.config.computedFields).toEqual([
      {
        category: ChartDataViewFieldCategory.DateLevelComputedField,
        expression: 'AGG_DATE_YEAR([birthday])',
        name: 'birthday' + DATE_LEVEL_DELIMITER + 'AGG_DATE_YEAR',
        type: 'DATE',
      },
    ]);
  });
});
