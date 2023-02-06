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

export default {
  HTTP_METHOD_LIST: ['GET', 'POST', 'PUT', 'HEAD'],
  TIME_LIMITS: [15, 30, 60, 120, 180, 300, 600, 1800],
  OPERATORS: {
    '=': i18n.t('dop:equal to'),
    '>': i18n.t('greater than'),
    '>=': i18n.t('dop:greater than or equal to'),
    '<=': i18n.t('dop:less than or equal to'),
    '<': i18n.t('less than'),
  },
  CONTAINS: {
    contains: i18n.t('dop:contains'),
    not_contains: i18n.t('dop:does not contain'),
    regex: i18n.t('msp:regular matching'),
    not_regex: i18n.t('msp:regular mismatch'),
  },
  MAX_BODY_LENGTH: 10000,
};
