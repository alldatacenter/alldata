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

import i18n from 'i18n';
import { map } from 'lodash';

export const resourcesValidator = (_rule: any, value: any, callback: (message?: string) => void) => {
  if (!value.mem || value.mem < 0) {
    return callback(i18n.t('dop:the memory size cannot be empty'));
  } else if (!value.cpu || value.cpu < 0) {
    return callback(i18n.t('dop:the number of cpu cannot be empty'));
  }

  callback();
};

export const portsValidator = (_rule: any, value: any, callback: (message?: string) => void) => {
  let pass = true;
  map(value, (item) => {
    if (item.protocol && item.port === undefined) {
      // 有协议无端口 不通过
      pass = false;
    }
  });
  if (!pass) {
    return callback(i18n.t('dop:port cannot be empty'));
  }

  callback();
};
