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

/* eslint-disable no-case-declarations */
import React from 'react';
import KeyValueEdit from 'apiManagePlatform/pages/api-market/detail/components/key-val-edit';
import APIBody from 'apiManagePlatform/pages/api-market/detail/components/api-body';
import { ColumnProps } from 'core/common/interface';
import { map } from 'lodash';
import { mock } from 'mock-json-schema';
import i18n from 'i18n';

export const APITabs = [
  {
    title: 'Query',
    dataIndex: 'params',
    render: (props: any) => {
      return (
        <KeyValueEdit
          dataModel={{
            editKey: true,
            key: '',
            value: '',
            desc: '',
          }}
          itemMap={[
            {
              type: 'key',
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
              getProps: ({ editKey }: { editKey: boolean }) => {
                return {
                  disabled: !editKey,
                };
              },
            },
            {
              type: 'value',
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
              getProps: ({ error, required }: { error: boolean; required: true }) => {
                return {
                  className: error ? 'error-red' : '',
                  placeholder: required ? i18n.t('dop:parameter value (required)') : i18n.t('dop:parameter value'),
                };
              },
            },
            {
              type: 'desc',
              props: {
                placeholder: i18n.t('description'),
              },
            },
          ]}
          {...props}
        />
      );
    },
  },
  {
    title: 'Headers',
    dataIndex: 'header',
    render: (props: any) => {
      return (
        <KeyValueEdit
          dataModel={{
            editKey: true,
            key: '',
            value: '',
            desc: '',
          }}
          itemMap={[
            {
              type: 'key',
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
              getProps: ({ editKey }: { editKey: boolean }) => {
                return {
                  disabled: !editKey,
                };
              },
            },
            {
              type: 'value',
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
              getProps: ({ error, required }: { error: boolean; required: true }) => {
                return {
                  className: error ? 'error-red' : '',
                  placeholder: required ? i18n.t('dop:parameter value (required)') : i18n.t('dop:parameter value'),
                };
              },
            },
            {
              type: 'desc',
              props: {
                placeholder: i18n.t('description'),
              },
            },
          ]}
          {...props}
        />
      );
    },
  },
  {
    title: 'Body',
    dataIndex: 'body',
    render: (props: any) => {
      return <APIBody {...props} />;
    },
  },
  {
    title: 'Path',
    dataIndex: 'path',
    render: (props: any) => {
      return (
        <KeyValueEdit
          itemMap={[
            {
              type: 'key',
              props: {
                placeholder: i18n.t('dop:parameter name'),
                disabled: true,
              },
            },
            {
              type: 'value',
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
              getProps: ({ error, required }: { error: boolean; required: true }) => {
                return {
                  className: error ? 'error-red' : '',
                  placeholder: required ? i18n.t('dop:parameter value (required)') : i18n.t('dop:parameter value'),
                };
              },
            },
            {
              type: 'desc',
              props: {
                placeholder: i18n.t('description'),
              },
            },
          ]}
          {...props}
        />
      );
    },
  },
];

export const PreviewTabs: API_MARKET.PreviewTabs[] = ['Request', 'Response'];

export const RequestTabs: { [k in API_MARKET.RequestTabs]: { name: string; value: k } } = {
  params: {
    name: 'Query',
    value: 'params',
  },
  header: {
    name: 'Headers',
    value: 'header',
  },
  body: {
    name: 'Body',
    value: 'body',
  },
};

export const ResponseTabs: { [k in API_MARKET.ResponseTabs]: { name: string; value: k } } = {
  header: {
    name: 'Headers',
    value: 'header',
  },
  body: {
    name: 'Body',
    value: 'body',
  },
};

export const commonColumn: Array<ColumnProps<any>> = [
  {
    title: 'Key',
    dataIndex: 'key',
  },
  {
    title: 'Value',
    dataIndex: 'value',
  },
];

export const stringifyPro = (data: Record<string, any> | any[], space: number, replace?: any): string => {
  let valueCache: any[] = [];
  let keyCache: any[] = [];
  const str = JSON.stringify(
    data || {},
    (key: string, value: any) => {
      if (typeof value === 'object' && value !== null) {
        const index = valueCache.indexOf(value);
        if (index !== -1) {
          return replace || `[Circular reference: ${keyCache[index]}]`;
        }
        valueCache.push(value);
        keyCache.push(key);
      }
      return value;
    },
    space,
  );
  valueCache = [];
  keyCache = [];
  return str;
};

export const formatData = (data = {}) => {
  const apis = {
    params: [],
    path: [],
    body: {
      isAdd: true,
      type: 'none',
      content: '',
    },
    header: [],
  };
  map(data, (value: any[] = [], key) => {
    switch (key) {
      case 'query':
        apis.params = map(value, (item: any) => {
          return {
            desc: item.description,
            key: item.name,
            value: '',
            required: item.required,
          };
        });
        break;
      case 'path':
        apis.path = map(value, (item: any) => {
          return {
            desc: item.description,
            key: item.name,
            value: '',
            required: item.required,
          };
        });
        break;
      case 'body':
        const { schema } = value[0] || ({} as any);
        const s = JSON.parse(stringifyPro(schema, 2, []));
        apis.body = {
          content: JSON.stringify(mock(s), null, 2),
          type: 'application/json',
        };
        break;
      case 'header':
        apis.header = map(value, (item: any) => {
          return {
            desc: item.description,
            key: item.name,
            value: '',
            required: item.required,
          };
        });
        break;
      case 'formData':
        const content = map(value, (item: any) => {
          return {
            desc: item.description,
            key: item.name,
            value: '',
            required: item.required,
          };
        });
        apis.body = {
          content,
          type: 'application/x-www-form-urlencoded',
        };
        break;
      default:
        break;
    }
  });
  return apis;
};

export const formatBody = (body: { type: string; content: any }) => {
  const data = {};
  if (body.type === 'application/x-www-form-urlencoded') {
    map(body.content, (item) => {
      if (item.key) {
        data[item.key] = item.value;
      }
    });
  }
  return ['application/json', 'Text', 'Text(text/plain)'].includes(body.type) ? body.content : data;
};

export const fillingUrl = (url: string, path: any[]) => {
  let fetchURl = url;
  if (!path.length) {
    return fetchURl;
  }
  const replacePattern = /\{([\w.])+\}/g;
  const matches = fetchURl.match(replacePattern);
  if (matches) {
    matches.forEach((match) => {
      const key = match.slice(1, -1);
      const { value } = path.find((item: any) => item.key === key) || ({} as any);
      if (value !== '' && typeof value !== 'undefined' && value !== null) {
        fetchURl = fetchURl.replace(match, value);
      }
    });
  }
  return fetchURl;
};
