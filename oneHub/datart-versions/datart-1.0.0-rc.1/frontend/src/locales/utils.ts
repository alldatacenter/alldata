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

import { StorageKeys } from 'globalConstants';
export function getInitialLocale() {
  const storedLocale = localStorage.getItem(StorageKeys.Locale);
  if (!storedLocale) {
    const browserLocale = ['zh', 'zh-CN'].includes(navigator.language) // FIXME locale
      ? 'zh'
      : 'en';
    localStorage.setItem(StorageKeys.Locale, browserLocale);
    return browserLocale;
  } else {
    return storedLocale;
  }
}
