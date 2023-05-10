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
import { Spin, Alert, Button } from 'antd';
import { RenderForm, MultiInput } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { useEffectOnce } from 'react-use';
import { FormInstance } from 'core/common/interface';
import { map, groupBy, find, get, isEmpty } from 'lodash';
import {
  authenticationMap,
  authorizationMap,
  envMap,
  addonStatusMap,
} from 'apiManagePlatform/pages/access-manage/components/config';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import { goTo } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';

const formatVersion = (data: API_MARKET.VersionTreeItem[]) => {
  return data.map(({ swaggerVersion, versions }) => {
    const major = get(versions, ['0', 'major']);
    return {
      swaggerVersion,
      major,
      versions,
    };
  });
};

interface FormData {
  assetID: string;
  projectID: string;
  major: number;
  minor: number;
  authentication: API_ACCESS.AuthenticationEnum;
  authorization: API_ACCESS.AuthorizationEnum;
  bindDomain: string[];
}

interface IState {
  projectList: Array<{ projectID: number; projectName: string }>;
  resourceVersions: API_MARKET.VersionTreeChild[];
  formData: FormData;
  instanceWorkSpace: '' | API_ACCESS.Workspace;
}

const AccessEdit = () => {
  const formRef = React.useRef<FormInstance>({} as FormInstance);
  const { type, accessID } = routeInfoStore.useStore((s) => s.params);
  const [assetList, versionTree] = apiMarketStore.useStore((s) => [s.assetList, s.versionTree]);
  const [apiGateways, accessDetail] = apiAccessStore.useStore((s) => [s.apiGateways, s.accessDetail]);
  const { getApiGateway, createAccess, getAccessDetail, updateAccess } = apiAccessStore.effects;
  const { clearAccessDetail, clearApiGateways } = apiAccessStore.reducers;
  const { getAssetList, getVersionTree, getInstance } = apiMarketStore.effects;
  const { clearVersionTree, clearState } = apiMarketStore.reducers;
  const [isfetchApi, ...isLoading] = useLoading(apiAccessStore, [
    'getApiGateway',
    'getAccessDetail',
    'createAccess',
    'updateAccess',
  ]);
  const [isfetchIns] = useLoading(apiMarketStore, ['getInstance']);
  const [state, updater, update] = useUpdate<IState>({
    projectList: [],
    resourceVersions: [],
    formData: {},
    instanceWorkSpace: '',
  });
  useEffectOnce(() => {
    if (accessID) {
      getAccessDetail({ accessID: +accessID }).then((res) => {
        const { access } = res;
        getInstance({
          assetID: access.assetID,
          swaggerVersion: access.swaggerVersion,
          minor: access.minor,
          major: access.major,
        }).then(({ instantiation }) => {
          updater.instanceWorkSpace(instantiation.workspace);
        });
        getApiGateway({ projectID: access.projectID });
        getVersionTree({ assetID: access.assetID, patch: false, instantiation: true, access: false }).then(
          ({ list }) => {
            const { versions } =
              find(formatVersion(list), (t) => t.major === access.major) || ({} as API_MARKET.VersionTreeItem);
            updater.resourceVersions(versions);
          },
        );
        updater.projectList([{ projectID: access.projectID, projectName: access.projectName }]);
      });
    }
    getAssetList({ paging: false, scope: 'mine', hasProject: true, latestSpec: false, latestVersion: false });
    return () => {
      clearAccessDetail();
      clearVersionTree();
      clearState({ key: 'instance', value: [] });
    };
  });
  const assetVersions = React.useMemo(() => formatVersion(versionTree), [versionTree]);
  const refreshApiGateway = (data?: { workspace: API_ACCESS.Workspace; addonInstanceID: string }) => {
    const projectID = formRef.current.getFieldValue('projectID');
    if (projectID) {
      getApiGateway({ projectID: +projectID });
    }
    if (!isEmpty(data)) {
      window.refreshApiGateway = null;
    }
  };
  const gotoServer = React.useCallback((e: React.MouseEvent<HTMLSpanElement>, env: string) => {
    e.stopPropagation();
    const projectId = formRef.current.getFieldValue('projectID');
    window.refreshApiGateway = refreshApiGateway;
    goTo(goTo.pages.projectService, { projectId, jumpOut: true, query: { env, addon: 'api-gateway' } });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const gateways = React.useMemo(() => {
    if (!state.projectList.length || isfetchApi) {
      return [];
    }
    const workspace: API_ACCESS.Workspace[] = !state.instanceWorkSpace
      ? ['DEV', 'TEST', 'STAGING', 'PROD']
      : [state.instanceWorkSpace];
    const temp = groupBy(apiGateways, 'workspace');
    const apiGateWays: Array<Merge<{ name: React.ReactNode }, API_ACCESS.ApiGateway>> = [];
    workspace.forEach((item) => {
      const name = envMap[item];
      const gatewayTemp = temp[item] || [];
      if (gatewayTemp.length) {
        apiGateWays.push(
          ...gatewayTemp.map((t) => ({
            name: `${addonStatusMap[t.status]?.name}-${name}-${t.addonInstanceID}`,
            ...t,
          })),
        );
      } else {
        apiGateWays.push({
          addonInstanceID: `${item}-noAPIGateway`,
          workspace: item,
          status: 'ATTACHED',
          name: (
            <div
              className="flex justify-between items-center"
              onClick={(e) => {
                gotoServer(e, item);
              }}
            >
              <span>{name}</span>
              <span className="text-link">{i18n.t('establish')}</span>
            </div>
          ),
        });
      }
    });
    return apiGateWays;
  }, [apiGateways, gotoServer, state.projectList, state.instanceWorkSpace, isfetchApi]);
  const handleSubmit = (form: FormInstance) => {
    form
      .validateFields()
      .then((data: any) => {
        const { workspace } = gateways.find((t) => t.addonInstanceID === data.addonInstanceID) || {};
        const payload = { ...data, minor: +data.minor, major: +data.major, projectID: +data.projectID, workspace };
        if (accessID) {
          updateAccess(payload).then(() => {
            window.history.back();
          });
        } else {
          createAccess(payload).then((res) => {
            goTo(goTo.pages.apiAccessManageDetail, { accessID: res.access.id });
          });
        }
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        form.scrollToField(errorFields[0].name);
      });
  };
  const handleChange = (name: string, value: string, clearFields: string[]) => {
    let versions = [];
    const temp = {} as FormData;
    let asset = {} as API_MARKET.Asset;
    let swaggerVersion = '';
    clearFields.forEach((item) => {
      temp[item] = undefined;
    });
    switch (name) {
      case 'assetID':
        getVersionTree({ assetID: value, patch: false, instantiation: true, access: false });
        clearApiGateways();
        updater.projectList([]);
        break;
      case 'major':
        versions = (find(assetVersions, (t) => t.major === +value) || ({} as API_MARKET.VersionTreeItem)).versions;
        clearApiGateways();
        update({
          projectList: [],
          resourceVersions: versions,
        });
        break;
      case 'minor':
        // eslint-disable-next-line no-case-declarations
        const { assetID, major } = formRef.current.getFieldsValue(['assetID', 'major']);
        swaggerVersion = (assetVersions.find((item) => item.major === +major) || ({} as any)).swaggerVersion;
        clearApiGateways();
        updater.projectList([]);
        getInstance({ assetID, swaggerVersion, minor: +value, major: +major }).then(({ instantiation }) => {
          // 内部实例
          let projectList = [];
          if (instantiation.type === 'dice') {
            temp.projectID = `${instantiation.projectID}`;
            projectList = [{ projectID: instantiation.projectID, projectName: instantiation.projectName }];
          } else {
            asset = (find(assetList, (t) => t.asset.assetID === assetID) || ({} as API_MARKET.AssetListItem)).asset;
            temp.projectID = `${asset.projectID}`;
            projectList = [{ projectID: asset.projectID, projectName: asset.projectName }];
          }
          update({
            projectList,
            instanceWorkSpace: instantiation.workspace,
          });
          formRef.current.setFieldsValue({ projectID: temp.projectID });
          getApiGateway({ projectID: +temp.projectID });
        });
        break;
      default:
        break;
    }
    formRef.current.setFieldsValue(temp);
  };
  const { access } = accessDetail;
  const fieldsList = [
    {
      getComp(): React.ReactElement<any> | string {
        return (
          <Alert
            showIcon
            type="info"
            message={i18n.t(
              'Note: The precondition to create access management is that the API must first complete the project association and version instance association.',
              { nsSeparator: '|' },
            )}
          />
        );
      },
    },
    {
      label: i18n.t('API name'),
      name: 'assetID',
      type: 'select',
      options: assetList.map((item) => ({ name: item.asset.assetName, value: item.asset.assetID })),
      initialValue: access.assetID,
      itemProps: {
        placeholder: i18n.t('please select'),
        disabled: type === 'edit',
        onChange: (v: string) => {
          handleChange('assetID', v, ['projectID', 'major', 'minor', 'addonInstanceID']);
        },
      },
    },
    {
      label: i18n.t('API version'),
      type: 'select',
      name: 'major',
      initialValue: access.major,
      options: map(assetVersions, (item) => ({ name: item.swaggerVersion, value: item.major })),
      itemProps: {
        disabled: type === 'edit',
        placeholder: i18n.t('please select'),
        onChange: (v: string) => {
          handleChange('major', v, ['minor', 'projectID', 'addonInstanceID']);
        },
      },
    },
    {
      label: i18n.t('resource version'),
      type: 'select',
      name: 'minor',
      initialValue: access.minor,
      options: map(state.resourceVersions, (item) => ({ name: `V${item.major}.${item.minor}.*`, value: item.minor })),
      itemProps: {
        placeholder: i18n.t('please select'),
        onChange: (v: string) => {
          handleChange('minor', v, ['projectID', 'addonInstanceID']);
        },
      },
    },
    {
      label: i18n.t('related project name'),
      type: 'select',
      name: 'projectID',
      initialValue: access.projectID,
      options: map(state.projectList, ({ projectID, projectName }) => ({ name: projectName, value: projectID })),
      itemProps: {
        disabled: true,
        placeholder: i18n.t('please select'),
      },
    },
    {
      label: i18n.t('bind domain'),
      name: 'bindDomain',
      initialValue: access.bindDomain || [],
      config: {
        valuePropType: 'array',
      },
      getComp(): React.ReactElement<any> | string {
        return <MultiInput placeholder={`${i18n.t('such as')}: example.com`} />;
      },
      rules: [
        {
          validator: (_rule: any, value: string[], callback: Function) => {
            const domainReg = /^([a-z]|\d|-|\*)+(\.([a-z]|\d|-|\*)+)+$/;
            let errMsg: any;
            (value || []).forEach((domain, index) => {
              if (!domainReg.test(domain) && !errMsg) {
                errMsg = i18n.t('item {index} should be lowercase letters, numbers, dot, -, *', { index: index + 1 });
              }
            });
            callback(errMsg);
          },
        },
      ],
    },
    {
      label: i18n.t('API gateway'),
      type: 'select',
      name: 'addonInstanceID',
      initialValue: access.addonInstanceID,
      options: gateways.map((item) => ({
        name: item.name,
        value: item.addonInstanceID,
        disabled: item.status !== 'ATTACHED',
      })),
      itemProps: {
        placeholder: i18n.t('please select'),
        onFocus: () => {
          refreshApiGateway();
        },
        notFoundContent: isfetchApi || isfetchIns ? <Spin size="small" /> : null,
      },
    },
    {
      label: i18n.t('authentication method'),
      name: 'authentication',
      type: 'select',
      initialValue: access.authentication,
      options: map(authenticationMap, (item) => item),
      itemProps: {
        placeholder: i18n.t('please select'),
      },
    },
    {
      label: i18n.t('authorization method'),
      name: 'authorization',
      type: 'select',
      initialValue: access.authorization,
      options: map(authorizationMap, (item) => item),
      itemProps: {
        placeholder: i18n.t('please select'),
      },
    },
    {
      getComp(): React.ReactElement<any> | string {
        return (
          <Alert
            showIcon
            type="info"
            message={i18n.t(
              'Auto authorization: apply and call Manual authorization: apply, manual authorization and call',
              { nsSeparator: '|' },
            )}
          />
        );
      },
    },
    {
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="mt-5">
          <Button type="primary" onClick={() => handleSubmit(form)}>
            {i18n.t('ok')}
          </Button>
          <Button className="ml-3" onClick={() => window.history.back()}>
            {i18n.t('cancel')}
          </Button>
        </div>
      ),
    },
  ];
  return (
    <Spin spinning={isLoading.some((t) => t)}>
      <div>{(access.assetID || !accessID) && <RenderForm list={fieldsList} layout="vertical" ref={formRef} />}</div>
    </Spin>
  );
};

export default AccessEdit;
