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
import { CRUDTable, FormModal, LoadMoreSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import certRefStore from 'application/stores/certificate-reference';
import { approvalStatus } from 'application/common/config';
import { formatTime, insertWhen } from 'common/utils';
import certificateStore from 'org/stores/certificate';
import { useUnmount } from 'react-use';
import { WORKSPACE_LIST } from 'common/constants';
import { get } from 'lodash';
import { Popconfirm } from 'antd';
import routeInfoStore from 'core/stores/route';
import { getCertificateList } from 'org/services/certificate';

const rules = [
  {
    pattern: /^[a-zA-Z_]+[.a-zA-Z0-9_-]*$/,
    message: i18n.t('common:start with letters, which can contain letters, numbers, dots, underscores and hyphens.'),
  },
];

const CertMap = {
  Android: {
    name: 'Android',
    value: 'Android',
    pushConfigField: [
      {
        label: `Debug keystore ${i18n.t('file')} ${i18n.t('variable name')}`,
        name: ['androidKey', 'debugKeyStoreFile'],
        initialValue: 'Debug-keystore',
        rules,
      },
      {
        label: `Debug keystore ${i18n.t('cmp:alias')}`,
        name: ['androidKey', 'debugKeyStoreAlias'],
        initialValue: 'Debug-keystore-alias',
        rules,
      },
      {
        label: `Debug key-password ${i18n.t('variable name')}`,
        name: ['androidKey', 'debugKeyPassword'],
        initialValue: 'Debug-key-password',
        rules,
      },
      {
        label: `Debug store-password ${i18n.t('variable name')}`,
        name: ['androidKey', 'debugStorePassword'],
        initialValue: 'Debug-store-password',
        rules,
      },
      {
        label: `Release keystore ${i18n.t('file')} ${i18n.t('variable name')}`,
        name: ['androidKey', 'releaseKeyStoreFile'],
        initialValue: 'Release-keystore',
        rules,
      },
      {
        label: `Release keystore ${i18n.t('cmp:alias')}`,
        name: ['androidKey', 'releaseKeyStoreAlias'],
        initialValue: 'Release-keystore-alias',
        rules,
      },
      {
        label: `Release key-password ${i18n.t('variable name')}`,
        name: ['androidKey', 'releaseKeyPassword'],
        initialValue: 'Release-key-password',
        rules,
      },
      {
        label: `Release store-password ${i18n.t('variable name')}`,
        name: ['androidKey', 'releaseStorePassword'],
        initialValue: 'Release-store-password',
        rules,
      },
    ],
  },
  IOS: {
    name: 'IOS',
    value: 'IOS',
    pushConfigField: [
      {
        label: `Keychain-p12 ${i18n.t('file')} ${i18n.t('variable name')}`,
        name: ['iosKey', 'keyChainP12File'],
        initialValue: 'Keychain-p12',
        rules,
      },
      {
        label: `Keychain-p12 ${i18n.t('password')} ${i18n.t('variable name')}`,
        name: ['iosKey', 'keyChainP12Password'],
        initialValue: 'Keychain-p12-password',
        rules,
      },
      {
        label: `Debug-mobileprovision ${i18n.t('file')} ${i18n.t('variable name')}`,
        name: ['iosKey', 'debugMobileProvision'],
        initialValue: 'Debug-mobileprovision',
        rules,
      },
      {
        label: `Release-mobileprovision ${i18n.t('file')} ${i18n.t('variable name')}`,
        name: ['iosKey', 'releaseMobileProvision'],
        initialValue: 'Release-mobileprovision',
        rules,
      },
    ],
  },
  Message: {
    name: i18n.t('message'),
    value: 'Message',
    pushConfigField: [
      {
        label: 'Key',
        name: ['messageKey', 'key'],
        rules,
      },
    ],
  },
};

const AppCertificateReference = () => {
  const { appId } = routeInfoStore.useStore((s) => s.params);
  const { pushToConfig, getList: getRefList } = certRefStore.effects;

  useUnmount(() => {
    certificateStore.reducers.clearList();
  });

  const [state, updater, update] = useUpdate({
    visible: false,
    editData: {},
  });

  const getColumns = ({ deleteItem }: any) => {
    const columns = [
      {
        title: i18n.t('dop:certificate name'),
        dataIndex: 'name',
      },
      {
        title: i18n.t('dop:certificate type'),
        dataIndex: 'type',
        render: (v: string) => CertMap[v] && CertMap[v].name,
      },
      {
        title: i18n.t('create time'),
        dataIndex: 'createdAt',
        width: 240,
        render: (text: string) => formatTime(text, 'YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: i18n.t('dop:approval status'),
        dataIndex: 'status',
        width: 200,
        render: (text: string) => approvalStatus[text],
      },
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: 140,
        render: (_v: any, record: APP_SETTING.CertRef) => {
          return (
            <div className="table-operations">
              {record.status === 'approved' && (
                <span
                  className="table-operations-btn"
                  onClick={() =>
                    update({
                      editData: {
                        enable: false,
                        certificateType: record.type,
                        ...record.pushConfig,
                        certificateId: record.certificateId,
                        appId: record.appId,
                      },
                      visible: true,
                    })
                  }
                >
                  {i18n.t('config')}
                </span>
              )}
              {record.status !== 'pending' && (
                <Popconfirm
                  title={`${i18n.t('common:confirm to delete')}?`}
                  onConfirm={() =>
                    deleteItem({ certificateId: record.certificateId, appId: record.appId }).then(() => {
                      certRefStore.effects.getList({ appId });
                    })
                  }
                >
                  <span className="table-operations-btn">{i18n.t('remove')}</span>
                </Popconfirm>
              )}
            </div>
          );
        },
      },
    ];
    return columns;
  };

  const getFieldsList = () => {
    const fieldsList = [
      {
        name: 'appId',
        itemProps: {
          type: 'hidden',
        },
        initialValue: +appId,
      },
      {
        label: i18n.t('dop:choose certificate'),
        name: 'certificateId',
        type: 'custom',
        getComp: () => {
          const getData = (q: any) => {
            const { q: searchKey, ...qRest } = q;
            return getCertificateList({ ...qRest, name: searchKey, appId }).then((res: any) => res.data);
          };
          return (
            <LoadMoreSelector
              getData={getData}
              placeholder={i18n.t('please choose {name}', { name: i18n.t('certificate') })}
            />
          );
        },
      },
    ];
    return fieldsList;
  };

  const configFields = [
    {
      name: 'appId',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      name: 'certificateId',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      name: 'certificateType',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('dop:push to variable config'),
      name: 'enable',
      type: 'switch',
      itemProps: {
        // TODO: 如何在fields变化时触发Form重渲染？
        onChange: (v: boolean) => updater.editData((prev: any) => ({ ...prev, enable: v })),
      },
    },
    ...insertWhen(state.editData.enable, [
      {
        label: i18n.t('env'),
        name: 'envs',
        type: 'select',
        options: WORKSPACE_LIST.map((env) => ({ name: env, value: env })),
        itemProps: {
          mode: 'multiple',
        },
      },
      ...(get(CertMap, `${[state.editData.certificateType]}.pushConfigField`) || []),
    ]),
  ];

  const onOk = (data: any) => {
    onCancel();
    pushToConfig(data)
      .then(() => {
        getRefList({ appId });
      })
      .catch(() => {
        // TODO 2020/4/20 请求失败后，现有FormModal会充值表单域（是否合理），导致显示逻辑不正确，需重置editData
        updater.editData({});
      });
  };

  const onCancel = () => {
    update({
      editData: {},
      visible: false,
    });
  };
  return (
    <>
      <FormModal
        title={i18n.t('dop:push config')}
        visible={state.visible}
        formData={state.editData}
        fieldsList={configFields}
        onOk={onOk}
        onCancel={onCancel}
      />
      <CRUDTable.StoreTable<APP_SETTING.LibRef>
        getColumns={getColumns as any}
        getFieldsList={getFieldsList}
        extraQuery={{ appId }}
        store={certRefStore}
        handleFormSubmit={(submitData: APP_SETTING.LibRef, { addItem, updateItem, isEdit }) => {
          const data = { ...submitData, appId: +appId };
          if (isEdit) {
            return updateItem(data);
          } else {
            return addItem(data);
          }
        }}
      />
    </>
  );
};

export default AppCertificateReference;
