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
import SwaggerParser from 'swagger-parser';
import { OpenAPI } from 'openapi-types';
import { cloneDeep, map, isEmpty, groupBy, pick, omit, Dictionary, uniqWith } from 'lodash';
import { FormModal, IFormItem } from 'common';
import { useUpdate } from 'common/use-hooks';
import ApiMenu from 'app/modules/apiManagePlatform/pages/api-market/detail/components/api-menu';
import ApiDetail from 'app/modules/apiManagePlatform/pages/api-market/detail/components/api-detail';
import TestModal from 'apiManagePlatform/pages/api-market/detail/components/test-modal';
import './api-view.scss';
import { message, Button, Input, Alert } from 'antd';
import i18n from 'i18n';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import routeInfoStore from 'core/stores/route';
import { insertWhen } from 'common/utils';
import { authenticationMap } from 'apiManagePlatform/pages/access-manage/components/config';
import { HTTP_METHODS } from 'app/modules/apiManagePlatform/pages/api-market/components/config';
import { convertToOpenApi2 } from 'apiManagePlatform/pages/api-market/detail/components/api-preview-3.0';

type ApiData = Merge<OpenAPI.Document, { basePath: string }>;
type ApiMapItem = Merge<OpenAPI.Operation, { _method: string; _path: string }>;
interface TagMap {
  [key: string]: ApiMapItem[];
}
interface AutoInfo {
  clientID: string;
  clientSecret: string;
}
interface IProps {
  deprecated: boolean;
  specProtocol: API_MARKET.SpecProtocol;
  dataSource: API_MARKET.AssetVersionItem<OpenAPI.Document>;
  onChangeVersion: (id: number) => void;
}
interface IState {
  apiData: ApiData;
  currentApi: string;
  testModalVisible: boolean;
  authModal: boolean;
  authed: boolean;
}

const ApiView = ({ dataSource, onChangeVersion, deprecated, specProtocol }: IProps) => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [hasAccess, accessDetail] = apiMarketStore.useStore((s) => [
    s.assetVersionDetail.hasAccess,
    s.assetVersionDetail.access,
  ]);
  const [{ apiData, currentApi, testModalVisible, authModal, authed }, updater, update] = useUpdate<IState>({
    apiData: {},
    currentApi: '',
    testModalVisible: false,
    authModal: false,
    authed: false,
  });
  React.useEffect(() => {
    if (dataSource.spec) {
      SwaggerParser.dereference(
        cloneDeep(dataSource.spec),
        {},
        (err: Error | null, data: OpenAPI.Document | undefined) => {
          if (err) {
            message.error(i18n.t('default:failed to parse API description document'));
            throw err;
          }
          updater.apiData((data || {}) as ApiData);
        },
      );
    }
    return () => {
      updater.apiData({} as ApiData);
    };
  }, [dataSource.spec, updater]);
  const getAuthInfo = React.useCallback(() => {
    const authInfo = JSON.parse(sessionStorage.getItem(`asset-${params.assetID}`) || '{}');
    return authInfo[`version-${params.versionID}`];
  }, [params.assetID, params.versionID]);
  React.useEffect(() => {
    const authInfo = getAuthInfo();
    let isAuthed = !isEmpty(authInfo);
    if (isAuthed && accessDetail.authentication === authenticationMap['sign-auth'].value) {
      isAuthed = !!authInfo.clientSecret;
    }
    updater.authed(isAuthed);
  }, [getAuthInfo, updater, accessDetail.authentication]);
  React.useEffect(() => {
    return () => {
      sessionStorage.removeItem(`asset-${params.assetID}`);
    };
  }, [params.assetID]);
  const fullingUrl = React.useCallback(
    (path: string) => {
      let url = path;
      if (apiData.basePath && !['', '/'].includes(apiData.basePath)) {
        url = apiData.basePath + path;
      }
      return url;
    },
    [apiData.basePath],
  );
  const [tagMap, apiMap] = React.useMemo(() => {
    const _tagMap = {} as TagMap;
    const _apiMap = {} as { [K: string]: ApiMapItem };
    if (isEmpty(apiData.paths)) {
      return [{}, {}];
    }
    map(apiData.paths, (methodMap, path) => {
      const _path = fullingUrl(path);
      const httpRequests = pick(methodMap, HTTP_METHODS);
      const restParams = omit(methodMap, HTTP_METHODS);
      map(httpRequests, (api, method) => {
        const parameters = uniqWith(
          [...(api.parameters || [])].concat(restParams.parameters || []),
          (a, b) => a.in === b.in && a.name === b.name,
        );
        const item: ApiMapItem = {
          _method: method,
          _path,
          ...api,
          ...restParams,
          parameters,
        };
        map(api.tags || ['OTHER'], (tagName) => {
          if (_tagMap[tagName]) {
            _tagMap[tagName].push(item);
          } else {
            _tagMap[tagName] = [];
            _tagMap[tagName].push(item);
          }
        });
        _apiMap[method + _path] = item;
      });
    });
    return [_tagMap, _apiMap];
  }, [apiData.paths, fullingUrl]);

  const handleChange = React.useCallback(
    (key: string) => {
      updater.currentApi(key);
    },
    [updater],
  );
  const handleShowTest = () => {
    if (!authed) {
      message.error(i18n.t('please authenticate first'));
      return;
    }
    updater.testModalVisible(true);
  };
  const handleOk = (data: AutoInfo) => {
    const authInfo = JSON.parse(sessionStorage.getItem(`asset-${params.assetID}`) || '{}');
    authInfo[`version-${params.versionID}`] = data;
    sessionStorage.setItem(`asset-${params.assetID}`, JSON.stringify(authInfo));
    update({
      authed: true,
      authModal: false,
    });
  };

  const testButton = hasAccess ? (
    <>
      <Button onClick={handleShowTest}>{i18n.t('test')}</Button>
      {
        <Button
          className="ml-2"
          onClick={() => {
            updater.authModal(true);
          }}
        >
          {authed ? i18n.t('recertification') : i18n.t('authentication')}
        </Button>
      }
    </>
  ) : null;
  const fieldsList: IFormItem[] = [
    {
      label: 'clientID',
      name: 'clientID',
    },
    ...insertWhen(accessDetail.authentication === authenticationMap['sign-auth'].value, [
      {
        label: 'clientSecret',
        name: 'clientSecret',
        getComp: () => <Input.Password />,
      },
    ]),
  ];
  const currentApiSource = apiMap[currentApi] || {};
  const parametersMap: Dictionary<any[]> = groupBy(currentApiSource.parameters, 'in');
  if (specProtocol && specProtocol.includes('oas3')) {
    Object.assign(parametersMap, convertToOpenApi2(currentApiSource));
  }
  const autoInfo = getAuthInfo();
  return (
    <div className="apis-view flex justify-between items-center flex-1">
      <div className="apis-view-left">
        <ApiMenu list={tagMap} onChange={handleChange} onChangeVersion={onChangeVersion} />
      </div>
      <div className="apis-view-right">
        {deprecated ? (
          <Alert className="mb-4" type="warning" message={i18n.t('the current version is deprecated')} />
        ) : null}
        <ApiDetail key={currentApi} dataSource={currentApiSource} extra={testButton} specProtocol={specProtocol} />
      </div>
      <TestModal
        key={`${currentApiSource._method}${currentApiSource._path}`}
        visible={testModalVisible}
        onCancel={() => {
          updater.testModalVisible(false);
        }}
        dataSource={{
          autoInfo,
          basePath: apiData.basePath,
          url: currentApiSource._path,
          method: currentApiSource._method?.toUpperCase(),
          requestScheme: parametersMap,
          host: apiData.host,
          protocol: apiData.schemes?.includes('https') ? 'https' : 'http',
        }}
      />
      <FormModal
        title={i18n.t('authentication')}
        visible={authModal}
        fieldsList={fieldsList}
        onCancel={() => {
          updater.authModal(false);
        }}
        formData={getAuthInfo()}
        onOk={handleOk}
      />
    </div>
  );
};

export default ApiView;
