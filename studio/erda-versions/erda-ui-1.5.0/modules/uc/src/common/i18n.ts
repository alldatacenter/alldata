// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import i18n from 'i18next';
import zh from 'src/locales/zh.json';
import en from 'src/locales/en.json';

let userLanguage = window.navigator.language;
userLanguage = userLanguage === 'zh-CN' ? 'zh' : 'en';

const defaultLocale = window.localStorage.getItem('locale') || userLanguage;
const curLocal = window.localStorage.getItem('locale');
!curLocal && window.localStorage.setItem('locale', defaultLocale);

export function isZh() {
  return window.localStorage.getItem('locale');
}

export const initI18n = i18n.init({
  lng: defaultLocale,
  fallbackLng: 'zh',
  debug: process.env.NODE_ENV !== 'production',
  resources: {
    zh,
    en,
  },
  // have a common namespace used around the full app
  ns: [],
  defaultNS: 'default',
  interpolation: {
    prefix: '{',
    suffix: '}',
    formatSeparator: ',',
    format(value, format, lng) {
      if (format === 'uppercase') return value.toUpperCase();
      return value;
    },
  },
  keySeparator: false, // we use content as keys
  react: {
    wait: true,
  },
});

export default i18n;
