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

export type IProtocol = {
  [k in API_MARKET.SpecProtocol]: {
    name: 'JSON' | 'YAML';
    fullName: string;
    suffix: string;
    pattern: RegExp;
  };
};

export const protocolMap: IProtocol = {
  'oas3-json': {
    name: 'JSON',
    fullName: i18n.t('{name} format', { name: 'Swagger 3.0 Json' }),
    suffix: '.json',
    pattern: /\.json$/,
  },
  'oas3-yaml': {
    name: 'YAML',
    fullName: i18n.t('{name} format', { name: 'Swagger 3.0 Yaml' }),
    suffix: '.yml,.yaml',
    pattern: /(\.yml|\.yaml)$/,
  },
  'oas2-json': {
    name: 'JSON',
    fullName: i18n.t('{name} format', { name: 'Swagger 2.0 Json' }),
    suffix: '.json',
    pattern: /\.json$/,
  },
  'oas2-yaml': {
    name: 'YAML',
    fullName: i18n.t('{name} format', { name: 'Swagger 2.0 Yaml' }),
    suffix: '.yml,.yaml',
    pattern: /(\.yml|\.yaml)$/,
  },
};

export const HTTP_METHODS = ['put', 'get', 'post', 'delete', 'options', 'head', 'patch', 'trace'];
