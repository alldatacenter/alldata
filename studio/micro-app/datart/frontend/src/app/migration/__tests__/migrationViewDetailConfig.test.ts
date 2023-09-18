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

import { APP_VERSION_BETA_2 } from '../constants';
import { migrateViewConfig } from '../ViewConfig/migrationViewDetailConfig';

describe('migrateViewConfig Test', () => {
  test('Test when config is null', () => {
    const config = JSON.stringify(null);
    expect(migrateViewConfig(config)).toEqual('null');
  });

  test('Test when config is empty', () => {
    const config = '';
    expect(migrateViewConfig(config)).toEqual('');
  });

  test('Test config without expensiveQuery variable', () => {
    const config =
      '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}';
    expect(migrateViewConfig(config)).toEqual(
      `{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":false,"version":"${APP_VERSION_BETA_2}"}`,
    );
    expect(migrateViewConfig(config)).toMatch(/"version":/);
  });

  test('Test when there is a version number and expensiveQuery = false', () => {
    const config = `{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":false,"version":"${APP_VERSION_BETA_2}"}`;
    expect(migrateViewConfig(config)).toEqual(config);
    expect(migrateViewConfig(config)).toMatch(/"version":/);
  });

  test('Test when there is a version number and expensiveQuery = true', () => {
    const config = `{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":true,"version":"${APP_VERSION_BETA_2}"}`;
    expect(migrateViewConfig(config)).toEqual(config);
    expect(migrateViewConfig(config)).toMatch(/"version":/);
  });

  /**
   * The case of dirty data
   */
  test('Test when config is null', () => {
    const config =
      '{"expensiveQuery":false,"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}';
    expect(migrateViewConfig(config)).toEqual(
      `{"expensiveQuery":false,"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"version":"${APP_VERSION_BETA_2}"}`,
    );
    expect(migrateViewConfig(config)).toMatch(/"version":/);
  });

  /**
   * The case of dirty data
   */
  test('Test for dirty data', () => {
    const config =
      '{"expensiveQuery":true,"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0}';
    expect(migrateViewConfig(config)).toEqual(
      `{"expensiveQuery":false,"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"version":"${APP_VERSION_BETA_2}"}`,
    );
    expect(migrateViewConfig(config)).toMatch(/"version":/);
  });
});
