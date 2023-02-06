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

import React, { useRef, MutableRefObject } from 'react';
import { FormModal, LoadMoreSelector, IFormItem } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { insertWhen, regRules } from 'common/utils';
import { FormInstance, RadioChangeEvent } from 'core/common/interface';
import { getMyProject, getApps, getAppInstance, getAppDetail } from 'apiManagePlatform/services/api-market';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import routeInfoStore from 'core/stores/route';
import { ChooseVersion } from 'apiManagePlatform/pages/api-market/version/version-info';
import { get, pick, groupBy, map, isEmpty, uniqBy } from 'lodash';
import { Select } from 'antd';

export type RelationMode = 'instance' | 'asset';

interface IProps {
  visible: boolean;
  mode: RelationMode;
  versionInfo: ChooseVersion;

  onCancel: () => void;
}

interface IState {
  serviceList: Record<string, API_MARKET.AppInstanceItem[]>;
  branchList: API_MARKET.AppInstanceItem[];
  instanceList: API_MARKET.AppInstanceItem[];
  instanceType: API_MARKET.InstanceType;
  chooseProjectID: number | undefined;
}

const RelationModal = ({ visible, onCancel, versionInfo, mode }: IProps) => {
  const formRef = useRef({}) as MutableRefObject<FormInstance>;
  const [assetDetail, instance] = apiMarketStore.useStore((s) => [s.assetDetail.asset, s.instance]);
  const defaultAppID = mode === 'asset' ? assetDetail.appID : instance.appID || assetDetail.appID;
  const defaultProjectID = mode === 'asset' ? assetDetail.projectID : instance.projectID || assetDetail.projectID;
  const params = routeInfoStore.useStore((s) => s.params);
  const instanceType = get(instance, 'type', 'dice');
  const relationRef = useRef<{ projectList: PROJECT.Detail[]; appList: IApplication[] }>({
    projectList: [],
    appList: [],
  });
  const [state, updater, update] = useUpdate<IState>({
    serviceList: {},
    branchList: [],
    instanceList: [],
    instanceType: 'dice',
    chooseProjectID: undefined,
  });
  const { getAssetDetail, editAsset, editInstance } = apiMarketStore.effects;
  const getProjects = (query: { pageSize: number; pageNo: number; q?: string }) => {
    return getMyProject<Promise<API_MARKET.CommonResList<PROJECT.Detail[]>>>({ ...query }).then((res) => {
      if (res.success) {
        const { projectList } = relationRef.current;
        const list = res.data?.list || [];
        relationRef.current.projectList = uniqBy([...projectList, ...list], 'id');
        return { list, total: res.data.total };
      } else {
        return { list: [], total: 0 };
      }
    });
  };
  const chosenItemConvertProject = (selectItem: { label: string }) => {
    if (isEmpty(selectItem)) {
      return [];
    }
    if (!selectItem.label) {
      if (mode === 'instance' && instance.projectID) {
        return [{ value: instance.projectID, label: instance.projectName }];
      } else {
        return [{ value: assetDetail.projectID, label: assetDetail.projectName }];
      }
    }
    return selectItem;
  };
  const getApplications = React.useCallback(
    (query: { pageSize: number; pageNo: number; q?: string }) => {
      if (!state.chooseProjectID) {
        relationRef.current.appList = [];
        return;
      }
      return getApps<Promise<API_MARKET.CommonResList<IApplication[]>>>({
        ...query,
        projectId: state.chooseProjectID,
      }).then((res) => {
        if (res.success) {
          const list = res.data?.list || [];
          const { appList } = relationRef.current;
          relationRef.current.appList = uniqBy([...appList, ...list], 'id');
          return { list, total: res.data.total };
        } else {
          return { list: [], total: 0 };
        }
      });
    },
    [state.chooseProjectID],
  );
  const chosenItemConvertApp = (selectItem: { label: string }) => {
    if (isEmpty(selectItem)) {
      return [];
    }
    if (!selectItem.label) {
      if (mode === 'instance' && instance.appID) {
        return getAppDetail(instance.appID).then(({ data }) => {
          return [{ value: data.id, label: data.name }];
        });
      } else {
        return [{ value: assetDetail.appID, label: assetDetail.appName }];
      }
    }
    return selectItem;
  };
  const getAppInstances = React.useCallback(
    (appID: number) => {
      if (!appID) {
        update({
          serviceList: {},
          branchList: [],
          instanceList: [],
        });
        return;
      }
      getAppInstance<Promise<{ success: boolean; data: API_MARKET.AppInstanceItem[] }>>({ appID }).then((res) => {
        if (res.success) {
          const serviceList = groupBy(res.data || [], 'serviceName');
          let branchList: API_MARKET.AppInstanceItem[] = [];
          let instanceList: API_MARKET.AppInstanceItem[] = [];
          if (instance.serviceName) {
            branchList = serviceList[instance.serviceName] || [];
          }
          if (instance.runtimeID) {
            instanceList = branchList.filter((item) => item.runtimeID === instance.runtimeID);
          }
          update({
            serviceList,
            branchList,
            instanceList,
          });
        }
      });
    },
    [instance.runtimeID, instance.serviceName, update],
  );
  React.useEffect(() => {
    if (visible) {
      update({
        instanceType,
        chooseProjectID: defaultProjectID,
      });
      if (instanceType === 'dice' && defaultAppID && mode === 'instance') {
        getAppInstances(defaultAppID);
      }
    } else {
      relationRef.current = {
        projectList: [],
        appList: [],
      };
    }
  }, [assetDetail.appID, instanceType, defaultProjectID, mode, visible, update, defaultAppID, getAppInstances]);
  const handleChange = (name: string, value: number | API_MARKET.InstanceType, clearFields: string[]) => {
    const temp = {};
    clearFields.forEach((item) => {
      temp[item] = undefined;
    });
    switch (name) {
      case 'type':
        updater.instanceType(value as API_MARKET.InstanceType);
        temp.type = value;
        temp.url = instanceType !== value ? undefined : instance.url;
        if (value === 'dice') {
          if (defaultAppID) {
            getAppInstances(defaultAppID);
          }
        }
        break;
      case 'projectID':
        relationRef.current.appList = [];
        update({
          serviceList: {},
          branchList: [],
          instanceList: [],
          chooseProjectID: +value,
        });
        break;
      case 'appID':
        if (mode === 'instance') {
          getAppInstances(+value);
        }
        break;
      case 'serviceName':
        update({
          branchList: state.serviceList[value] || [],
          instanceList: [],
        });
        break;
      case 'runtimeID':
        update({
          instanceList: state.branchList.filter((item) => item.runtimeID === +value),
        });
        break;
      default:
        break;
    }
    formRef.current.setFieldsValue(temp);
  };
  const handleOk = async (data: any) => {
    if (mode === 'instance') {
      const instantiationID = instance.id;
      const { minor, swaggerVersion } = versionInfo;
      const { projectID, appID, type, url, serviceName, runtimeID } = data;
      const { workspace } = state.branchList.find((item) => item.runtimeID === +runtimeID) || {};
      const payload = {
        minor,
        swaggerVersion,
        assetID: params.assetID,
        projectID: type === 'dice' ? +projectID : undefined,
        appID: type === 'dice' ? +appID : undefined,
        type,
        url,
        serviceName,
        runtimeID: +runtimeID,
        workspace,
      } as API_MARKET.UpdateInstance;
      if (instantiationID) {
        payload.instantiationID = instantiationID;
      }
      await editInstance(payload);
      onCancel();
    } else if (mode === 'asset') {
      const { appList, projectList } = relationRef.current;
      const asset = pick(assetDetail, ['assetName', 'desc', 'logo', 'assetID']);
      let projectName: string | undefined;
      let appName: string | undefined;
      if (data.projectID) {
        projectName = get(
          projectList.find((item) => item.id === +data.projectID),
          'name',
        );
      }
      if (data.appID) {
        appName = get(
          appList.find((item) => item.id === +data.appID),
          'name',
        );
      }
      await editAsset({
        ...asset,
        projectID: +data.projectID,
        appID: +data.appID,
        assetID: params.assetID,
        projectName,
        appName,
      });
      onCancel();
      getAssetDetail({ assetID: params.assetID }, true);
    }
  };
  const fieldsList: IFormItem[] = [
    ...insertWhen(mode === 'instance', [
      {
        label: i18n.t('instance source'),
        name: 'type',
        type: 'radioGroup',
        required: true,
        initialValue: instanceType,
        itemProps: {
          onChange: (e: RadioChangeEvent) => {
            handleChange('type', e.target.value, []);
          },
        },
        options: [
          {
            name: i18n.t('internal'),
            value: 'dice',
          },
          {
            name: i18n.t('external'),
            value: 'external',
          },
        ],
      },
    ]),
    ...insertWhen(mode === 'asset' || (mode === 'instance' && state.instanceType === 'dice'), [
      {
        label: i18n.t('project name'),
        name: 'projectID',
        required: false,
        initialValue: defaultProjectID,
        getComp: () => {
          return (
            <LoadMoreSelector
              chosenItemConvert={chosenItemConvertProject}
              getData={getProjects}
              dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
                total,
                list: map(list, ({ id, name }) => {
                  return {
                    label: name,
                    value: id,
                  };
                }),
              })}
            />
          );
        },
        itemProps: {
          allowClear: true,
          placeholder: i18n.t('please select'),
          onChange: (v: number) => {
            handleChange('projectID', v, ['appID', 'serviceName', 'runtimeID', 'url']);
          },
        },
      },
      {
        label: i18n.t('dop:app name'),
        name: 'appID',
        initialValue: defaultAppID,
        required: false,
        getComp: () => {
          return (
            <LoadMoreSelector
              chosenItemConvert={chosenItemConvertApp}
              extraQuery={{ projectId: state.chooseProjectID }}
              getData={getApplications}
              dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
                total,
                list: map(list, ({ id, name }) => {
                  return {
                    label: name,
                    value: id,
                  };
                }),
              })}
            />
          );
        },
        itemProps: {
          allowClear: true,
          placeholder: i18n.t('please select'),
          onChange: (v) => {
            handleChange('appID', v, ['serviceName', 'runtimeID', 'url']);
          },
        },
      },
    ]),
    ...insertWhen(mode === 'instance', [
      ...(state.instanceType === 'dice'
        ? [
            {
              label: i18n.t('service name'),
              required: false,
              type: 'select',
              initialValue: get(instance, 'serviceName'),
              options: map(state.serviceList, (_v, k) => ({ name: k, value: k })),
              name: 'serviceName',
              itemProps: {
                allowClear: true,
                placeholder: i18n.t('please select'),
                onChange: (v) => {
                  handleChange('serviceName', v, ['runtimeID', 'url']);
                },
              },
            },
            {
              label: i18n.t('msp:deployment branch'),
              required: false,
              type: 'select',
              name: 'runtimeID',
              initialValue: get(instance, 'runtimeID'),
              options: map(state.branchList, ({ runtimeID, runtimeName }) => ({ name: runtimeName, value: runtimeID })),
              itemProps: {
                allowClear: true,
                placeholder: i18n.t('please select'),
                onChange: (v: number) => {
                  handleChange('runtimeID', v, ['url']);
                },
              },
            },
            {
              label: i18n.t('instance'),
              type: 'select',
              name: 'url',
              initialValue: instanceType === 'dice' ? get(instance, 'url') : undefined,
              required: false,
              itemProps: {
                allowClear: true,
              },
              getComp: () => (
                <Select placeholder={i18n.t('please select')}>
                  {state.instanceList.map(({ serviceAddr }) => {
                    return (serviceAddr || []).map((url) => (
                      <Select.Option key={url} value={url}>
                        {url}
                      </Select.Option>
                    ));
                  })}
                </Select>
              ),
            },
          ]
        : [
            {
              label: i18n.t('instance'),
              name: 'url',
              required: false,
              initialValue: instanceType === 'dice' ? undefined : get(instance, 'url'),
              rules: [{ pattern: regRules.url, message: i18n.t('Please enter address started with http') }],
            },
          ]),
    ]),
  ];
  return (
    <FormModal
      title={mode === 'asset' ? i18n.t('connection relation') : i18n.t('related instance')}
      fieldsList={fieldsList}
      ref={formRef}
      visible={visible}
      onCancel={onCancel}
      onOk={handleOk}
      modalProps={{
        destroyOnClose: true,
      }}
    />
  );
};

export default RelationModal;
