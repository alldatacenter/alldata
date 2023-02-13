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
import { Copy, CRUDTable, LoadMoreSelector, Icon as CustomIcon, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import libraryRefStore from 'application/stores/library-reference';
import publisherStore from 'publisher/stores/publisher';
import { FormInstance } from 'core/common/interface';
import appStore from 'application/stores/application';
import { appMode, approvalStatus } from 'application/common/config';
import { formatTime } from 'common/utils';
import { WithAuth } from 'user/common';
import { useUnmount } from 'react-use';
import { Popconfirm, Modal, Button, Alert, Input, message } from 'antd';
import routeInfoStore from 'core/stores/route';
import { getArtifactsList } from 'publisher/services/publisher';

const AppLibraryReference = () => {
  const appID = +routeInfoStore.useStore((s) => s.params.appId);
  const { name: appName } = appStore.useStore((s) => s.detail);
  const [state, updater, update] = useUpdate({
    visible: false,
    dependence: '',
  });

  useUnmount(() => {
    publisherStore.reducers.clearArtifactsList();
  });

  const getColumns = ({ deleteItem }: any) => {
    const columns = [
      {
        title: i18n.t('name'),
        dataIndex: 'libName',
      },
      {
        title: i18n.t('description'),
        dataIndex: 'libDesc',
      },
      {
        title: i18n.t('time'),
        dataIndex: 'createdAt',
        width: 240,
        render: (text: string) => formatTime(text, 'YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: i18n.t('dop:approval status'),
        dataIndex: 'approvalStatus',
        width: 200,
        render: (text: string) => approvalStatus[text],
      },
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: 100,
        render: (_v: any, record: APP_SETTING.LibRef) => {
          if (record.approvalStatus === 'pending') {
            return null;
          }
          return (
            <div className="table-operations">
              <WithAuth pass={record.approvalStatus === 'approved'}>
                <span
                  className="table-operations-btn"
                  onClick={() => {
                    showApplyModal(record);
                  }}
                >
                  {i18n.t('dop:apply')}
                </span>
              </WithAuth>
              <Popconfirm
                title={`${i18n.t('common:confirm to delete')}?`}
                onConfirm={() => deleteItem(record.id).then(() => libraryRefStore.effects.getList({ appID }))}
              >
                <span className="table-operations-btn">{i18n.t('remove')}</span>
              </Popconfirm>
            </div>
          );
        },
      },
    ];
    return columns;
  };

  const showApplyModal = ({ libID }: APP_SETTING.LibRef) => {
    publisherStore.effects.getLibVersion({ libID }).then((data: PUBLISHER.LibVersion[]) => {
      if (data.length) {
        const { libName, version } = data[0];
        if (!version) {
          message.info(i18n.t('no available version'));
          return;
        }
        update({
          visible: true,
          dependence: `"${libName}": "^${version}"`,
        });
      } else {
        message.info(i18n.t('no available version'));
      }
    });
  };

  const getFieldsList = (form: FormInstance) => {
    const fieldsList = [
      {
        name: 'libName',
        itemProps: {
          type: 'hidden',
        },
      },
      {
        name: 'libDesc',
        itemProps: {
          type: 'hidden',
        },
      },
      {
        label: i18n.t('dop:choose module'),
        name: 'libID',
        type: 'custom',
        itemProps: {
          onChange: (v: string, opt: any) => {
            form.setFieldsValue({
              libName: opt.name,
              libDesc: opt.desc,
            });
          },
        },
        getComp: () => {
          const getData = (q: object) => {
            return getArtifactsList({ ...q, type: appMode.LIBRARY, public: true }).then((res: any) => res.data);
          };
          return (
            <LoadMoreSelector
              getData={getData}
              placeholder={i18n.t('please choose {name}', { name: i18n.t('publisher:publisher content') })}
              optionRender={(option) => {
                const { label, desc, refCount, latestVersion } = option;
                return (
                  <div className="load-more-selector-library">
                    <div className="library-name mb-1">{label}</div>
                    <div className="library-desc mb-2">{desc}</div>
                    <div className="library-tips">
                      <CustomIcon type="bb1" />
                      {i18n.t('version number')}: {latestVersion || '--'}
                      <CustomIcon type="renwushu" />
                      {i18n.t('publisher:subscriptions')}: {refCount || 0}
                    </div>
                  </div>
                );
              }}
            />
          );
        },
      },
    ];
    return fieldsList;
  };

  const addonAfter = (
    <span className="copy-btn cursor-copy" data-clipboard-text={state.dependence} data-clipboard-tip="dependence">
      <ErdaIcon type="copy" />
    </span>
  );

  return (
    <>
      <CRUDTable.StoreTable<APP_SETTING.LibRef>
        formTitle={i18n.t('dop:choose module')}
        getColumns={getColumns}
        getFieldsList={getFieldsList}
        handleFormSubmit={(data: APP_SETTING.LibRef, { addItem }) => {
          const { libID } = data;
          return addItem({ ...data, libID: +libID, appID, appName });
        }}
        extraQuery={{ appID }}
        store={libraryRefStore}
      />
      <Modal
        title={i18n.t('dop:refer-to-market')}
        width={570}
        visible={state.visible}
        destroyOnClose
        onCancel={() => {
          updater.visible(false);
        }}
        footer={
          <Button
            onClick={() => {
              updater.visible(false);
            }}
          >
            {i18n.t('default:close')}
          </Button>
        }
      >
        <Copy selector=".cursor-copy" />
        <Alert className="mb-4" message={i18n.t('dop:library-usage-tips')} type="warning" showIcon />
        <p>{i18n.t('dop:code content')}</p>
        <Input className="w-full mb-4" value={state.dependence} addonAfter={addonAfter} />
      </Modal>
    </>
  );
};

export default AppLibraryReference;
