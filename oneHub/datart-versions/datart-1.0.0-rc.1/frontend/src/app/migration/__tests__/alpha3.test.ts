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

import { alpha3, hasWrongDimensionName } from '../alpha3';

describe('alpha3 - ', () => {
  test('should return false when config is null', () => {
    const config = null;
    expect(hasWrongDimensionName(config as any)).toBe(false);
  });

  test('should not match has wrong dimension name when config datas is empty', () => {
    const config = {
      datas: [],
    };
    expect(hasWrongDimensionName(config as any)).toBe(false);
  });

  test('should not match has wrong dimension name when config datas not contains deminsion key', () => {
    const config = {
      datas: [{ key: 'dimension' }, { key: 'metrics' }],
    };
    expect(hasWrongDimensionName(config as any)).toBe(false);
  });

  test('should match has wrong dimension name when config datas contains deminsion key', () => {
    const config = {
      datas: [{ key: 'deminsion' }, { key: 'metrics' }],
    };
    expect(hasWrongDimensionName(config as any)).toBe(true);
  });

  test('should match has wrong dimension name when config datas contains deminsionL key', () => {
    const config = {
      datas: [{ key: 'deminsionL' }],
    };
    expect(hasWrongDimensionName(config as any)).toBe(true);
  });

  test('should match has wrong dimension name when config datas contains deminsionR key', () => {
    const config = {
      datas: [{ key: 'deminsionR' }],
    };
    expect(hasWrongDimensionName(config as any)).toBe(true);
  });

  test('should not change anything when datas is emtpy', () => {
    const config = {
      chartConfig: {
        datas: [],
      },
    };
    expect(alpha3(config as any)).toBe(config);
  });

  test('should change key when key name is matched', () => {
    const config = {
      chartConfig: {
        datas: [{ key: 'deminsion' }],
      },
    };
    expect(alpha3(config as any)).toMatchObject({
      chartConfig: {
        datas: [{ key: 'metrics' }],
      },
    });
  });

  test('should change key when key name is matched', () => {
    const config = {
      chartConfig: {
        datas: [
          { key: 'deminsion', value: 1 },
          { key: 'metrics', value: 2 },
        ],
      },
    };
    expect(alpha3(config as any)).toMatchObject({
      chartConfig: {
        datas: [
          { key: 'metrics', value: 1 },
          { key: 'dimension', value: 2 },
        ],
      },
    });
  });

  test('should change key when key name is matched', () => {
    const config = {
      chartConfig: {
        datas: [
          { key: 'metrics', rows: [2] },
          { key: 'deminsionL', rows: [11] },
          { key: 'deminsionR', rows: [12] },
        ],
      },
    };
    expect(alpha3(config as any)).toMatchObject({
      chartConfig: {
        datas: [
          { key: 'dimension', rows: [2] },
          { key: 'metricsL', rows: [11] },
          { key: 'metricsR', rows: [12] },
        ],
      },
    });
  });
});
