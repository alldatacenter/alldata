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

import { APP_CURRENT_VERSION, APP_SEMANTIC_VERSIONS } from './constants';

const SemVerRegex =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$/gm;

export function validateSemVer(version) {
  return new RegExp(SemVerRegex).test(version);
}

export function getSemVer(version) {
  return new RegExp(SemVerRegex).exec(version);
}

export const versionCanDo = (curVersion: string, testVersion?: string) => {
  let testVersionIndex = APP_SEMANTIC_VERSIONS.indexOf(testVersion || '');
  if (testVersionIndex === -1) return true;
  let curVersionIndex = APP_SEMANTIC_VERSIONS.indexOf(curVersion);
  return curVersionIndex > testVersionIndex;
};

export const setLatestVersion = <T extends { version?: string }>(
  config: T,
): T => {
  if (!versionCanDo(APP_CURRENT_VERSION, config.version)) return config;
  if (config.version === APP_CURRENT_VERSION) return config;
  config.version = APP_CURRENT_VERSION;
  return config;
};
