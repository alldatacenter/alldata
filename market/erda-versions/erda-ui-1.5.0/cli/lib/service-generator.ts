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

import path from 'path';
import fs from 'fs';
import { logSuccess, logInfo, logError } from './util/log';
import http from 'superagent';
import { walker } from './util/file-walker';
import { get, forEach, keys } from 'lodash';
import { JSONSchema7, JSONSchema7TypeName } from 'json-schema';
import { decode } from 'js-base64';
import { EOL } from 'os';
import { isCwdInRoot } from './util/env';
import license from '../templates/license';

type ParamsType = 'query' | 'body' | 'path';

interface JSONSchema extends Omit<JSONSchema7, 'type'> {
  type: JSONSchema7TypeName | 'integer' | 'float' | 'double' | 'long';
}

interface ApiItem {
  apiName: string;
  method: string;
  resType: string;
  parameters: string;
  resData: string;
}

interface SwaggerData {
  paths: {
    [p: string]: {
      [m: string]: object;
    };
  };
  definitions: {
    [p: string]: JSONSchema;
  };
}

const NUMBER_TYPES = ['integer', 'float', 'double', 'number', 'long'];

const REG_SEARCH = /(AUTO\s*GENERATED)/g;

/*
to regex the case of:
  getClusterList: {
    api: 'get@/api/clusters',
  },
find the apiName, apiPath and method
*/
const REG_API = /([a-zA-Z]+):\s*{\n\s*api:\s*'(get|post|put|delete)@(\/api(\/:?[a-zA-Z-]+)+)'/g;

const formatJson = (data: string | object) => {
  const jsonData = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
  return jsonData.replace(/\\+n/g, '\n').replace(/"/g, '');
};

const formatApiPath = (apiPath: string) => {
  const pathParams: { [p: string]: string } = {};
  const newApiPath = apiPath.replace(/(:[a-zA-Z]+)/g, (p: string) => {
    const pName = p.slice(1);
    pathParams[pName] = pName.toLocaleLowerCase().includes('id') ? 'number' : 'string';
    return `<${pName}>`;
  });
  return {
    url: newApiPath,
    pathParams,
  };
};

const getSteadyContent = (filePath: string, content?: string) => {
  if (!fs.existsSync(filePath) || !content) {
    return `
${license.GPLV3}`;
  } else if (!content.includes('GENERATED')) {
    return `${content}
// > AUTO GENERATED  
`;
  } else {
    const lastIndex = content.indexOf('GENERATED');
    const steadyContent = lastIndex ? content.slice(0, lastIndex + 9) : content;

    return `${steadyContent}${EOL}`;
  }
};

const getBasicTypeData = (props: JSONSchema & { propertyName?: string }, swaggerData: SwaggerData) => {
  const { type, properties = {}, items, required = [] } = props || {};
  let value;

  if (type === 'object') {
    const data: { [p: string]: string | object } = {};
    // eslint-disable-next-line guard-for-in
    for (const key in properties) {
      const pData = properties[key] as JSONSchema;
      const __key = required.includes(key) ? key : `${key}?`;
      data[__key] = getBasicTypeData({ ...pData, propertyName: key }, swaggerData);
    }
    value = data;
  } else if (type === 'array' && items) {
    const itemsType = get(items, 'type');
    if (itemsType === 'object' || itemsType === 'array') {
      value = `Array<${formatJson(getSchemaData(items, swaggerData))}>`;
    } else {
      value = `${itemsType}[]`;
    }
  } else if (type === 'integer' || type === 'number') {
    value = 'number';
  } else {
    value = type;
  }
  return value;
};

const getSchemaData: any = (schemaData: JSONSchema, swaggerData: SwaggerData) => {
  const { $ref, type } = schemaData || {};
  let res;
  if ($ref) {
    const quoteList = $ref.split('/').slice(1);
    res = getSchemaData(get(swaggerData, quoteList), swaggerData);
  } else if (type) {
    res = getBasicTypeData(schemaData, swaggerData);
  } else {
    res = schemaData;
  }
  return res || {};
};

const getResponseType = (schemaData: JSONSchema, swaggerData: SwaggerData) => {
  const { $ref } = schemaData || {};
  if ($ref) {
    const quoteList = $ref.split('/').slice(1);
    const _data = get(swaggerData, [...quoteList, 'properties', 'data']) || {};
    if (_data.type === 'object' && _data.properties?.total && _data.properties?.list) {
      return 'pagingList';
    } else {
      return _data.type;
    }
  } else {
    return schemaData?.type;
  }
};

const autoGenerateService = (
  content: string,
  filePath: string,
  isEnd: boolean,
  swaggerData: SwaggerData,
  resolve: (value: void | PromiseLike<void>) => void,
) => {
  // only deal with suffix of '-api.ts'
  if (!filePath.endsWith('-api.ts')) {
    return;
  }

  if (content.match(REG_SEARCH)) {
    const apiList: ApiItem[] = [];

    let serviceContent = getSteadyContent(filePath, content);
    const typeFilePath = filePath.replace(/\/services\//, '/types/').replace(/-api\.ts/, '.d.ts');
    let typeContent = getSteadyContent(
      typeFilePath,
      fs.existsSync(typeFilePath) ? fs.readFileSync(typeFilePath, 'utf8') : '',
    );

    let regRes = REG_API.exec(content);

    while (regRes) {
      const [, apiName, method, _apiPath] = regRes;

      const { url: apiPath, pathParams } = formatApiPath(_apiPath);
      // get params from api path
      let parameters: { [p: string]: string } = { ...pathParams };

      // get params from 'parameters'
      forEach(
        get(swaggerData, ['paths', apiPath, method, 'parameters']),
        ({
          name,
          type,
          in: paramsIn,
          schema,
          required,
        }: {
          name: string;
          type: string;
          in: ParamsType;
          schema: JSONSchema;
          required?: boolean;
        }) => {
          if (paramsIn === 'query') {
            parameters[`${name}${required ? '' : '?'}`] = NUMBER_TYPES.includes(type) ? 'number' : type;
          } else if (paramsIn === 'path') {
            parameters[name] = NUMBER_TYPES.includes(type) ? 'number' : type;
          } else {
            parameters = {
              ...parameters,
              ...(getSchemaData(schema) || {}),
            };
          }
        },
      );

      const _schemaData = get(swaggerData, ['paths', apiPath, method, 'responses', '200', 'schema']);

      const resType = getResponseType(_schemaData, swaggerData);
      const fullData = getSchemaData(_schemaData) || {};

      const responseData = (resType === 'pagingList' ? fullData.data?.list : fullData.data || fullData['data?']) || {};

      const _resData: string = JSON.stringify(responseData, null, 2);
      let resData = formatJson(_resData);

      if (_resData.startsWith('"Array<')) {
        resData = formatJson(_resData.slice(7, _resData.length - 2));
      }

      apiList.push({
        apiName,
        method,
        parameters: formatJson(parameters),
        resData,
        resType,
      });
      regRes = REG_API.exec(content);
    }

    forEach(apiList, ({ apiName, parameters, resData, resType }) => {
      let pType = '{}';
      let bType = 'RAW_RESPONSE';
      if (resData !== '{}') {
        if (resType === 'pagingList') {
          bType = `IPagingResp<T_${apiName}_item>`;
        } else if (resType === 'array') {
          bType = `T_${apiName}_item[]`;
        } else {
          bType = `T_${apiName}_data`;
        }

        typeContent += `
interface T_${apiName}_${['array', 'pagingList'].includes(resType) ? 'item' : 'data'} ${resData}\n`;
      }
      if (parameters !== '{}') {
        typeContent += `
interface T_${apiName}_params ${parameters}\n`;
        pType = `T_${apiName}_params`;
      }

      serviceContent += `
export const ${apiName} = apiCreator<(p: ${pType}) => ${bType}>(apis.${apiName});
        `;
    });

    fs.writeFileSync(typeFilePath, typeContent, 'utf8');
    fs.writeFileSync(filePath, serviceContent, 'utf8');
  }

  if (isEnd) {
    logSuccess('service data is updated, bye 👋');
    resolve();
  }
};

const getSwaggerData = (url: string) => {
  return http
    .get(url)
    .set('content-type', 'application-json')
    .set('User-Agent', '')
    .then((response) => {
      logInfo('get swagger data successfully!');
      const content = decode(response?.body?.content);
      return content ? JSON.parse(content) : '';
    })
    .catch((err) => {
      logError('fail to get swagger data: ', err);
      return false;
    });
};

/*
  read ’swagger-config.json‘ in root path of erda-ui.
  this file should include swagger file info like 'repository', 'username' and 'filePath'
  If not found, we will use default data;
*/
const getLocalSwaggerConfig = (configPath: string) => {
  if (!configPath.includes('erda-ui')) {
    return null;
  }

  let curPath = configPath;

  while (!isCwdInRoot({ currentPath: curPath })) {
    curPath = !curPath.endsWith('erda-ui-enterprise')
      ? path.resolve(curPath, '..')
      : path.resolve(curPath, '../erda-ui');
  }

  const filePath = path.join(curPath, 'swagger-config.json');
  return fs.existsSync(filePath) ? fs.readFileSync(filePath, 'utf8') : null;
};

export default async ({ workDir }: { workDir: string }) => {
  try {
    const config = getLocalSwaggerConfig(workDir) as {
      repository?: string;
      username?: string;
      swaggerFilePath?: string;
    };
    logInfo(`local swagger config: ${config}`);

    const repository = config?.repository || 'erda';
    const username = config?.username || 'erda-project';
    const swaggerFilePath = config?.swaggerFilePath || 'pkg/swagger/oas3/testdata/swagger_all.json';

    const swagger = await getSwaggerData(
      `https://api.github.com/repos/${username}/${repository}/contents/${swaggerFilePath}`,
    );

    if (keys(swagger?.paths)?.length) {
      // search all files with suffix of '-api.ts' in target work directory, and handle the target file
      await new Promise<void>((resolve) => {
        walker({
          root: workDir,
          dealFile: (...args) => {
            autoGenerateService.apply(null, [...args, swagger, resolve]);
          },
        });
      });
    } else {
      logError('It is an empty swagger!');
      process.exit(1);
    }
  } catch (error) {
    logError(error);
  }
};
