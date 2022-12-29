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

import { APP_SEMANTIC_VERSIONS, APP_VERSION_INIT } from '../constants';
import SemVer from '../Semver';
import { validateSemVer } from '../utils';

describe('Semantic Version Tests', () => {
  test('should get semver model without preRelease, buildMetaData', () => {
    const semver = new SemVer('1.0.1');
    expect(semver.major).toBe('1');
    expect(semver.minor).toBe('0');
    expect(semver.patch).toBe('1');
    expect(semver.preRelease).toBe(undefined);
    expect(semver.buildMetaData).toBe(undefined);
  });

  test('should get semver model without buildMetaData', () => {
    const semver = new SemVer('1.0.1-beta.4');
    expect(semver.major).toBe('1');
    expect(semver.minor).toBe('0');
    expect(semver.patch).toBe('1');
    expect(semver.preRelease).toBe('beta.4');
    expect(semver.buildMetaData).toBe(undefined);
  });

  test('should get semver model with major, minor, patch, preRelease, buildMetaData', () => {
    const semver = new SemVer('1.0.0-beta4+100');
    expect(semver.major).toBe('1');
    expect(semver.minor).toBe('0');
    expect(semver.patch).toBe('0');
    expect(semver.preRelease).toBe('beta4');
    expect(semver.buildMetaData).toBe('100');
  });

  test('should match all datart versions except APP_VERSION_INIT', () => {
    APP_SEMANTIC_VERSIONS.filter(v => v !== APP_VERSION_INIT).forEach(
      version => {
        expect(validateSemVer(version)).toBeTruthy();
      },
    );
  });

  test('should all have major,minor,patch identity except APP_VERSION_INIT', () => {
    APP_SEMANTIC_VERSIONS.filter(v => v !== APP_VERSION_INIT).forEach(
      version => {
        const semver = new SemVer(version);
        expect(semver.major).not.toBeUndefined();
        expect(semver.minor).not.toBeUndefined();
        expect(semver.patch).not.toBeUndefined();
      },
    );
  });

  test('should be numeric buildMetaData if exist except APP_VERSION_INIT', () => {
    APP_SEMANTIC_VERSIONS.filter(v => v !== APP_VERSION_INIT).forEach(
      version => {
        const semver = new SemVer(version);
        if (semver.buildMetaData !== undefined) {
          expect(+semver.buildMetaData).not.toBe(NaN);
        }
      },
    );
  });
});
