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

export const urlSearchTransfer = {
  toParams: (url: string) => {
    const params = new URLSearchParams(url);
    const obj: any = {};
    const keysEntries = params.keys();
    for (const key of keysEntries) {
      obj[key] = params.getAll(key);
    }
    return obj;
  },
  toUrlString: (obj: Object): string => {
    const keys = Object.keys(obj);
    const params = new URLSearchParams();
    keys.forEach(k => {
      const value = obj[k];
      if (value instanceof Array) {
        value.forEach(v => {
          params.append(k, v);
        });
      } else {
        params.append(k, value);
      }
    });
    return params.toString();
  },
};
