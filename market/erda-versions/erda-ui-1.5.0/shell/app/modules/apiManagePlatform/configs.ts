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

export enum BASE_DATA_TYPE {
  string = 'string',
  number = 'number',
  boolean = 'boolean',
  array = 'array',
  object = 'object',
  integer = 'integer',
}

export const REQUIRED_OPTIONS = [
  { name: i18n.t('common:yes'), value: true },
  { name: i18n.t('common:no'), value: false },
];

export const RADIO_OPTIONS = [
  { name: 'true', value: true },
  { name: 'false', value: false },
  { name: 'none', value: null },
];

export enum NUMBER_TYPE_MAP {
  float = 'float',
  double = 'double',
}

export enum INTEGER_TYPE_MAP {
  int32 = 'int32',
  int64 = 'int64',
}

export const DATATYPE_EXAMPLE_MAP = {
  string: 'Example',
  number: 1,
  integer: 1,
  boolean: true,
  object: {},
  array: ['Example'],
};

export enum API_TREE_OPERATION {
  add = 'add',
  rename = 'rename',
  copy = 'copy',
  delete = 'delete',
  move = 'move',
}

export enum API_METHODS {
  get = 'get',
  post = 'post',
  put = 'put',
  delete = 'delete',
  head = 'head',
  options = 'options',
  patch = 'patch',
}
export const API_MEDIA_TYPE = ['application/json', 'application/xml', 'application/x-www-form-urlencoded'];

export const API_PROTOCOLS = ['HTTP', 'HTTPS'];

export enum API_WS_MSG_TYPE {
  beat = 'heart_beat_request',
  save = 'auto_save_request',
  commit = 'commit_request',
  error = 'error_response',
  submit = 'submit_request',
}

export const LIST_TITLE_MAP = {
  RESOURCE: i18n.t('dop:API list'),
  DATATYPE: i18n.t('dop:DATA TYPES'),
};

export const DEFAULT_TAG = 'other';

export const QUOTE_PREFIX = 'allOf';
export const QUOTE_PREFIX_NO_EXTENDED = '$ref';
export const API_FORM_KEY = 'x-dice-name';
export const API_PROPERTY_REQUIRED = 'x-dice-required';
export const API_MEDIA = 'x-dice-media';
export const API_FORBIDDEN_TYPES = 'x-dice-forbidden-types';

export const VERSION_RULE = {
  pattern: /^(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)$/,
  message: i18n.t('Please enter a valid version number, such as x.y.z.'),
};

export const READONLY_TIP = i18n.t('dop:read only and inoperable');

export const INPUT_MAX_LENGTH = 191;
export const TEXTAREA_MAX_LENGTH = 1024;

export const DEFAULT_PATH_PARAM = {
  required: true,
  in: 'path',
  schema: {
    example: 'Example',
    type: 'string',
  },
};

export const DEFAULT_RESPONSE = {
  200: {
    content: {
      'application/json': {
        schema: {
          description: '',
          'x-dice-name': 'schema',
          'x-dice-media': 'application/json',
          type: 'object',
          example: {},
        },
      },
    },
    description: '',
  },
};

export const NAME_REG = /^[a-z]([A-Z][a-z]+)*/;

export const MAX_VALUE = 1000000000000000;
export const MIN_VALUE = -1000000000000000;
export const NUMBER_PLACEHOLDER = i18n.t('please enter a number between {min} ~ {max}', {
  min: '-10^15',
  max: '10^15',
});
export const DEFAULT_NUMBER_PROPS = {
  min: MIN_VALUE,
  max: MAX_VALUE,
  placeholder: NUMBER_PLACEHOLDER,
};

const MAX_LENGTH_VALUE = Math.pow(10, 15);
const MIN_LENGTH_VALUE = 0;
const LENGTH_PLACEHOLDER = i18n.t('please enter a number between {min} ~ {max}', { min: '0', max: '10^15' });
export const DEFAULT_LENGTH_PROPS = {
  min: MIN_LENGTH_VALUE,
  max: MAX_LENGTH_VALUE,
  placeholder: LENGTH_PLACEHOLDER,
};

export enum API_RESOURCE_TAB {
  Summary = 'Summary',
  Params = 'Params',
  Headers = 'Headers',
  Body = 'Body',
  Response = 'Response',
  Test = 'Test',
}

export const API_LOCK_WARNING = i18n.t('dop:is editing, document is locked');
