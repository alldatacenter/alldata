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
import migrateChartConfig, { RC0 } from '../ChartConfig/migrateChartConfig';
import { APP_VERSION_RC_0 } from '../constants';

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
});

describe('test migrateChartConfig  ', () => {
  test('should add name fields for chartConfig computedFields', () => {
    const chartConfig = JSON.stringify({
      computedFields: [{ id: '1' }],
    }) as any;
    const result = migrateChartConfig(chartConfig);

    expect(result).toEqual(
      JSON.stringify({
        computedFields: [{ id: '1', name: '1' }],
        version: APP_VERSION_RC_0,
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
        version: APP_VERSION_RC_0,
      }),
    );
  });

  test('should not computedFields for chartConfig', () => {
    const chartConfig = JSON.stringify({}) as any;
    const result = migrateChartConfig(chartConfig as any);

    expect(result).toEqual(JSON.stringify({ version: APP_VERSION_RC_0 }));
  });
});
