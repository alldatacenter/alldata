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

import i18n, { getCurrentLocale, setLocale, isZh, getLang } from 'core/i18n';
import zh from './locales/zh.json';
import en from './locales/en.json';
import defaultZh from '../../locales/zh.json';
import defaultEn from '../../locales/en.json';
import { map, merge } from 'lodash';

map(merge(defaultZh, zh), (zhValue, zhKey) => {
  i18n.addResourceBundle('zh', zhKey, zhValue);
});
map(merge(defaultEn, en), (enValue, enKey) => {
  i18n.addResourceBundle('en', enKey, enValue);
});

// TODO: move to page-container
const currentLocale = getCurrentLocale();
document.body.lang = currentLocale.key;
const docDesc = document.querySelector('meta[name=description]');
if (currentLocale.key === 'en') {
  docDesc &&
    (docDesc.content =
      'A simple and efficient one-stop enterprise-level digital platform that provides enterprises with multiple platform services such as DevOps, microservice governance, multi-cloud management, and fast data management');
}

export default i18n;
export { getCurrentLocale, setLocale, isZh, getLang };
