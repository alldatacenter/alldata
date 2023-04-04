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
import { APP_CURRENT_VERSION, APP_SEMANTIC_VERSIONS } from '../constants';
import { setLatestVersion, versionCanDo } from '../utils';

const v1 = APP_SEMANTIC_VERSIONS[0];
const v2 = APP_SEMANTIC_VERSIONS[1];

describe('test versionCanDo ', () => {
  test(`no testVersion `, () => {
    expect(versionCanDo('v1', undefined)).toBe(true);
  });

  test(`v2 canDo v1 `, () => {
    expect(versionCanDo(v2, v1)).toBe(true);
  });

  test(`v1 not canDo v2 `, () => {
    expect(versionCanDo(v1, v2)).toBe(false);
  });

  test(`should set current version`, () => {
    expect(setLatestVersion({ version: 'v1' }).version).toBe(
      APP_CURRENT_VERSION,
    );
  });
});
