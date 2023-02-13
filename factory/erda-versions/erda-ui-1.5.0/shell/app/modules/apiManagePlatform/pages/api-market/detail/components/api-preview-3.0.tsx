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

import React from 'react';
import { isEmpty, groupBy, forEach, map, get, merge, cloneDeep } from 'lodash';
import i18n from 'i18n';
import { Tooltip } from 'antd';
import InfoPreview from 'config-page/components/info-preview/info-preview';
import { insertWhen } from 'common/utils';

export interface IItem {
  key: string;
  name: string;
  required: string;
  type: { value: string; enum?: any };
  children?: IItem[];
  description?: string;
}

export type IDataType = 'string' | 'number' | 'integer' | 'boolean' | 'array' | 'object';

export interface IParameters {
  in: 'path' | 'query' | 'header';
  required: boolean;
  name: string;
  description?: string;
  schema: {
    type: IDataType;
    format?: string;
    example?: string;
    enum?: any[];
    default: string;
    description: string;
  };
}

export interface IRequestBodyProperties {
  [key: string]: {
    type: IDataType;
    enum: string[];
    description?: string;
    items: {
      type: IDataType;
      enum?: any[];
      properties?: IRequestBodyProperties;
      required?: string[];
    };
    properties: IRequestBodyProperties;
    required: string[];
  };
}

export interface IRequestBodyContent {
  schema: {
    properties: IRequestBodyProperties;
    items?: IRequestBodyProperties;
    required: string[];
    type: IDataType;
  };
  type: IDataType;
}

export interface IRequestBody {
  content: {
    [k: string]: IRequestBodyContent;
  };
  'x-amf-mediaType': string;
}
export interface IDataSource {
  _method: string;
  _path: string;
  summary: string;
  description: string;
  parameters: IParameters[];
  requestBody?: IRequestBody;
  responses: {
    [k: number]: {
      description: string;
      content: {
        [k: string]: IRequestBodyContent;
      };
    };
  };
}

interface IProps {
  dataSource: IDataSource;
  extra?: React.ReactNode;
}

interface IParseOas3 {
  title: string;
  apiInfo: {
    method: string;
    path: string;
  };
  header: any[];
  path: any[];
  query: any[];
  requestBody: any[];
  responsesStatus: any[];
  responsesBody: {
    data: any[];
    type: IDataType;
  };
}

const columns = [
  { title: i18n.t('name'), dataIndex: 'name' },
  {
    title: i18n.t('type'),
    width: 120,
    dataIndex: 'type',
    render: ({ value, enum: enumData }: { value: IDataType; enum: string[] }) => {
      return isEmpty(enumData) ? (
        value
      ) : (
        <Tooltip title={enumData.toString()}>
          <span className="hover-active">{value}</span>
        </Tooltip>
      );
    },
  },
  { title: i18n.t('description'), dataIndex: 'description' },
  { title: i18n.t('required'), dataIndex: 'required', width: 80 },
];

const getSchema = (content = {}) => {
  if (isEmpty(content)) {
    return {};
  }
  if ('application/json' in content) {
    return get(content, ['application/json', 'schema']);
  } else {
    return get(content, [Object.keys(content)[0], 'schema']);
  }
};
/**
 * @description convert openApi3's body to openApi2's body
 */
export const convertToOpenApi2 = (data: IDataSource) => {
  const { requestBody } = data;
  if (requestBody?.content) {
    const { content } = requestBody;
    const contentType =
      'application/json' in content ? 'application/json' : Object.keys(content)[0] || 'application/json';
    const type = contentType === 'application/x-www-form-urlencoded' ? 'formData' : 'body';
    const schema = getSchema(content) || {};
    let value = [
      {
        in: type,
        schema: {
          type: 'object',
          contentType,
          ...schema,
        },
      },
    ];
    if (type === 'formData') {
      const { properties } = schema;
      value = map(properties, (item, key) => {
        return {
          ...item,
          name: key,
        };
      });
    }
    return {
      [type]: value,
    };
  } else {
    return {};
  }
};

const transformBody = (data: IRequestBodyContent['schema']): any[] => {
  const wmap = new WeakMap();
  const parse = (schema: IRequestBodyContent['schema'], parent: string) => {
    const { properties, required = [], allOf = [] } = schema;
    const allProperties = merge(properties, get(allOf, [0, 'properties']));
    return map(allProperties, (property, key) => {
      const { type, items: childItems, enum: paramsEnum, description } = property;
      const childProperties = merge(get(property, ['allOf', 0, 'properties']), get(property, 'properties'));
      const complexType = { value: type as string, enum: paramsEnum };
      if (type === 'array' && childItems.type) {
        complexType.value = `${type}<${childItems.type}>`;
        childItems.enum && (complexType.enum = childItems.enum);
      }
      let item: IItem = {
        key: `${parent}-${key}`,
        name: key,
        required: required.includes(key) ? i18n.t('common:yes') : i18n.t('common:no'),
        type: complexType,
        description,
      };
      if (type === 'array') {
        if (childItems.properties && !wmap.get(childItems.properties)) {
          wmap.set(childItems.properties, true);
          item = {
            ...item,
            children: parse(
              { properties: childItems.properties, required: childItems.required } as IRequestBodyContent['schema'],
              key,
            ),
          };
        }
      }
      if (!isEmpty(childProperties) && !wmap.get(childProperties)) {
        wmap.set(childProperties, true);
        item = {
          ...item,
          children: parse(property, key),
        };
      }
      if (get(childItems, ['allOf', 0])) {
        item.children = (item.children || []).concat(
          parse(get(childItems, ['allOf', 0]) as IRequestBodyContent['schema'], key),
        );
      }
      return {
        ...item,
      };
    });
  };
  return parse(cloneDeep(data), 'root');
};

export const parseOpenApi3 = (dataSource: IDataSource): IParseOas3 => {
  const { _method, _path, summary, description, parameters = [], requestBody, responses } = dataSource;
  const info = {
    title: summary || description || _path,
    apiInfo: {
      method: _method,
      path: _path,
    },
  } as IParseOas3;
  if (!isEmpty(parameters)) {
    const params = groupBy<IParameters>(parameters, 'in');
    forEach(params, (items, paramsName) => {
      info[paramsName] = info[paramsName] ? info[paramsName] : [];
      forEach(items, (item) => {
        const { name, required, schema, description: paramsDesc } = item;
        const {
          type,
          example,
          enum: paramsEnum,
          default: defaultValue,
          description: desc,
        } = schema || { type: 'string' };
        info[paramsName].push({
          name,
          required: required ? i18n.t('common:yes') : i18n.t('common:no'),
          type: { value: type, enum: paramsEnum },
          description: desc || paramsDesc,
          defaultValue: example,
          default: defaultValue,
        });
      });
    });
  }
  if (requestBody?.content) {
    info.requestBody = transformBody(getSchema(requestBody.content)) as any[];
  }
  if (!isEmpty(responses)) {
    info.responsesStatus = map(responses, ({ description: desc, content }, code) => {
      if (String(code) === '200' && !isEmpty(content)) {
        const schema = getSchema(content);
        if (schema.type === 'array' && isEmpty(schema.properties) && schema.items) {
          schema.properties = schema.items?.properties;
          if (isEmpty(schema.properties)) {
            schema.properties = get(schema.items, ['allOf', 0, 'properties'], {});
          }
        }
        info.responsesBody = {
          data: transformBody(schema),
          type: schema.type,
        };
      }
      return {
        code,
        description: desc,
      };
    });
  }
  return info;
};

const ApiPreviewV3 = ({ dataSource, extra }: IProps) => {
  const info = parseOpenApi3(dataSource);
  const previewProps = {
    data: {
      info,
    },
    props: {
      render: [
        { type: 'Title', dataIndex: 'title', props: { titleExtra: extra } },
        { type: 'Desc', dataIndex: 'desc' },
        { type: 'BlockTitle', props: { title: i18n.t('request information') } },
        { type: 'API', dataIndex: 'apiInfo' },
        ...insertWhen(!isEmpty(info.header), [
          {
            type: 'Table',
            dataIndex: 'header',
            props: {
              title: i18n.t('request header'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        ...insertWhen(!isEmpty(info.query), [
          {
            type: 'Table',
            dataIndex: 'query',
            props: {
              title: i18n.t('URL parameters'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        ...insertWhen(!isEmpty(info.path), [
          {
            type: 'Table',
            dataIndex: 'path',
            props: {
              title: i18n.t('path parameters'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        ...insertWhen(!isEmpty(info.requestBody), [
          {
            type: 'Table',
            dataIndex: 'requestBody',
            props: {
              title: i18n.t('request body'),
              rowKey: 'name',
              columns: [...columns],
            },
          },
        ]),
        { type: 'BlockTitle', props: { title: i18n.t('response information') } },
        ...insertWhen(!isEmpty(info.responsesBody), [
          ['object', 'array'].includes(info.responsesBody?.type)
            ? {
                type: 'Table',
                dataIndex: 'responsesBody.data',
                props: {
                  title: `${i18n.t('response body')}: ${info.responsesBody.type}`,
                  columns,
                },
              }
            : {
                type: 'Title',
                props: {
                  title: `${i18n.t('response body')}: ${info.responsesBody?.type || i18n.t('none')} `,
                  level: 2,
                },
              },
        ]),
        ...insertWhen(!isEmpty(info.responsesStatus), [
          {
            type: 'Table',
            dataIndex: 'responsesStatus',
            props: {
              title: i18n.t('response code'),
              columns: [
                { title: i18n.t('response code'), dataIndex: 'code', width: 128 },
                { title: i18n.t('description'), dataIndex: 'description' },
              ],
            },
          },
        ]),
      ],
    },
  } as Obj as CP_INFO_PREVIEW.Props;

  return <InfoPreview {...previewProps} />;
};

export default ApiPreviewV3;
