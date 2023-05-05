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
import { OpenAPI } from 'openapi-types';
import { groupBy, Dictionary, get, map, isEmpty } from 'lodash';
import { insertWhen } from 'common/utils';
import { Tooltip } from 'antd';
import InfoPreview from 'config-page/components/info-preview/info-preview';
import i18n from 'i18n';

interface IProps {
  dataSource: Merge<OpenAPI.Operation, { _method: string; _path: string }>;
  extra?: React.ReactNode;
}

const getBodyParams = (bodyData: any) => {
  const schema = bodyData?.schema || {};
  const hasMap = new WeakMap();
  const getProperties = (properties: Obj, requiredKeys: string[] = [], parentKey = '') => {
    const list = [] as any[];
    map(properties, (pData, pKey) => {
      const { type, description, items } = pData;
      if (!hasMap.get(pData)) {
        const curItem = {
          key: `${parentKey}-${pKey}`,
          name: pKey,
          type: { value: type, tooltip: (pData.enum || []).join(', ') },
          description,
          required: requiredKeys.includes(pKey),
        } as Obj;
        if (type === 'array' && items && !isEmpty(items.properties)) {
          curItem.children = getProperties(items.properties, items.required, `${parentKey}-${pKey}`);
        } else if (type === 'object' && !isEmpty(pData.properties)) {
          curItem.children = getProperties(pData.properties, pData.required, `${parentKey}-${pKey}`);
        }
        list.push(curItem);
        hasMap.set(pData, curItem);
      }
    });
    return list;
  };
  if (schema.type === 'array') {
    return getProperties(schema.items.properties, schema.items.required);
  }
  return getProperties(schema.properties, schema.required);
};

const getResponseBody = (dataSource: OpenAPI.Operation) => {
  const response = dataSource.responses || {};
  const responseCode = map(response, (item, code) => ({ code, desc: item.description }));
  const responseData = getBodyParams(response['200'] || {});
  const responseSchemaType = get(response['200'], 'schema.type');
  return { responseCode, responseData, responseSchemaType };
};

const columns = [
  { title: i18n.t('name'), dataIndex: 'name' },
  {
    title: i18n.t('type'),
    dataIndex: 'type',
    render: (val: { value: string; tooltip: string[] }) => {
      const { value, tooltip } = val || {};
      return tooltip ? (
        <Tooltip title={tooltip}>
          <span className="text-primary bg-light-active">{value}</span>
        </Tooltip>
      ) : (
        value
      );
    },
  },
  { title: i18n.t('description'), dataIndex: 'description' },
  {
    title: i18n.t('required'),
    dataIndex: 'required',
    render: (val: boolean) => (val ? i18n.t('common:yes') : i18n.t('common:no')),
    width: 64,
  },
];

const ApiPreviewV2 = (props: IProps) => {
  const { dataSource, extra } = props;
  // v2.0版本in的枚举值： query、header、path、formData、body
  const parametersMap: Dictionary<any[]> = groupBy(dataSource.parameters, 'in');
  const bodyParams = getBodyParams(get(parametersMap, 'body[0]'));
  const bodyParamsPureType = get(parametersMap, 'body[0].schema.type');
  const { responseCode, responseData, responseSchemaType } = getResponseBody(dataSource);
  const previewData = {
    data: {
      info: {
        title: dataSource.summary || dataSource.description || dataSource._path,
        desc: dataSource.description,
        apiInfo: { method: dataSource._method, path: dataSource._path },
        urlParams: parametersMap.query,
        pathParams: parametersMap.path,
        bodyParams,
        formData: parametersMap.formData,
        headerParams: parametersMap.header,
        responseCode,
        responseData,
      },
    },
    props: {
      render: [
        { type: 'Title', dataIndex: 'title', props: { titleExtra: extra } },
        { type: 'Desc', dataIndex: 'desc' },
        { type: 'BlockTitle', props: { title: i18n.t('request information') } },
        { type: 'API', dataIndex: 'apiInfo' },
        ...insertWhen(!!parametersMap.query, [
          {
            type: 'Table',
            dataIndex: 'urlParams',
            props: {
              title: i18n.t('URL parameters'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        ...insertWhen(!!parametersMap.path, [
          {
            type: 'Table',
            dataIndex: 'pathParams',
            props: {
              title: i18n.t('path parameters'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        !isEmpty(bodyParams)
          ? {
              type: 'Table',
              dataIndex: 'bodyParams',
              props: {
                title: i18n.t('request body'),
                rowKey: 'key',
                columns,
              },
            }
          : bodyParamsPureType
          ? {
              type: 'Title',
              props: { title: `${i18n.t('request body')}: ${bodyParamsPureType}`, level: 2 },
            }
          : null,
        !isEmpty(parametersMap.formData)
          ? {
              type: 'Table',
              dataIndex: 'formData',
              props: {
                title: i18n.t('request body'),
                rowKey: 'key',
                columns,
              },
            }
          : null,
        ...insertWhen(!!parametersMap.header, [
          {
            type: 'Table',
            dataIndex: 'headerParams',
            props: {
              title: i18n.t('request header'),
              rowKey: 'name',
              columns: [...columns, { title: i18n.t('default value'), dataIndex: 'default' }],
            },
          },
        ]),
        { type: 'BlockTitle', props: { title: i18n.t('response information') } },
        isEmpty(responseData)
          ? {
              type: 'Title',
              props: { title: `${i18n.t('response body')}: ${responseSchemaType || i18n.t('none')} `, level: 2 },
            }
          : {
              type: 'Table',
              dataIndex: 'responseData',
              props: {
                title: `${i18n.t('response body')}${responseSchemaType ? `: ${responseSchemaType}` : ''} `,
                rowKey: 'key',
                columns,
              },
            },
        isEmpty(responseCode)
          ? {
              type: 'Title',
              props: { title: `${i18n.t('response code')}: ${i18n.t('none')}`, level: 2 },
            }
          : {
              type: 'Table',
              dataIndex: 'responseCode',
              props: {
                title: i18n.t('response code'),
                rowKey: 'code',
                columns: [
                  { title: i18n.t('response code'), dataIndex: 'code' },
                  { title: i18n.t('description'), dataIndex: 'desc' },
                ],
              },
            },
      ],
    },
  } as Obj as CP_INFO_PREVIEW.Props;
  return <InfoPreview {...previewData} />;
};

export default ApiPreviewV2;
