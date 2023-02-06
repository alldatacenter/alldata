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

import React, { useState } from 'react';
import moment from 'moment';
import i18n from 'i18n';
import { isEmpty, map, find, pick } from 'lodash';
import { Spin, Modal, Tooltip, Switch, Select, Table, Button } from 'antd';
import { FormModal } from 'common';
import { useSwitch } from 'common/use-hooks';
import { FormInstance, ColumnProps } from 'core/common/interface';
import { useMount, useUnmount } from 'react-use';
import { useUserMap } from 'core/stores/userMap';
import { useLoading } from 'core/stores/loading';
import notifyGroupStore from '../../../../stores/notify-group';
import appNotifyStore from '../../../../stores/notify';
import {
  notifyChannelOptionsMap,
  getFinalNotifyChannelOptions,
  ListTargets,
} from 'application/pages/settings/components/app-notify/common-notify-group';
import { getNotifyChannelMethods } from 'application/services/notify';
import './index.scss';

const { confirm } = Modal;

interface IProps {
  commonPayload: {
    scopeType: string;
    scopeId: string;
    module: string;
  };
  memberStore: any;
}

export const NotifyConfig = ({ commonPayload, memberStore }: IProps) => {
  const roleMap = memberStore.useStore((s) => s.roleMap);
  const { getRoleMap } = memberStore.effects;
  const [notifyConfigs, notifyItems] = appNotifyStore.useStore((s) => [s.notifyConfigs, s.notifyItems]);
  const {
    getNotifyConfigs,
    deleteNotifyConfigs,
    createNotifyConfigs,
    updateNotifyConfigs,
    toggleNotifyConfigs,
    getNotifyItems,
  } = appNotifyStore.effects;
  const notifyGroups = notifyGroupStore.useStore((s) => s.notifyGroups);
  const [toggleNotifyConfigsLoading, getNotifyConfigsLoading] = useLoading(appNotifyStore, [
    'toggleNotifyConfigs',
    'getNotifyConfigs',
  ]);
  const userMap = useUserMap();
  const { getNotifyGroups } = notifyGroupStore.effects;
  const { clearNotifyGroups } = notifyGroupStore.reducers;
  const channelMethods = getNotifyChannelMethods.useData() as Obj<string>;
  const [modalVisible, openModal, closeModal] = useSwitch(false);
  const [activedData, setActivedData] = useState({});
  const [activedGroupId, setActivedGroupId] = useState('');
  const isEditing = !isEmpty(activedData);
  const [allChannelMethods, setAllChannelMethods] = useState(notifyChannelOptionsMap);

  useMount(() => {
    getRoleMap({ scopeType: commonPayload.scopeType, scopeId: commonPayload.scopeId });
    handleGetNotifyConfigs();
    getNotifyItems(pick(commonPayload, ['scopeType', 'module']));
    getNotifyGroups({ ...pick(commonPayload, ['scopeType', 'scopeId']), pageSize: 100 });
    getNotifyChannelMethods.fetch();
  });

  React.useEffect(() => {
    setAllChannelMethods(getFinalNotifyChannelOptions(channelMethods, false));
  }, [channelMethods]);

  useUnmount(() => {
    clearNotifyGroups();
  });

  const handleGetNotifyConfigs = () => {
    getNotifyConfigs(pick(commonPayload, ['scopeType', 'scopeId']));
  };

  const handleEdit = (item: APP_NOTIFY.INotify) => {
    openModal();
    const {
      id,
      name,
      notifyItems: items,
      channels,
      notifyGroup: { id: notifyGroupId },
    } = item;
    setActivedData({
      id,
      name,
      notifyItemIds: map(items, ({ id: notifyItemId }) => `${notifyItemId}`),
      notifyGroupId,
      channels: channels.split(','),
    });
    setActivedGroupId(`${notifyGroupId}`);
  };

  const handleDele = (id: number) => {
    confirm({
      title: i18n.t('dop:are you sure you want to delete this item?'),
      content: i18n.t('dop:the notification will be permanently deleted'),
      onOk() {
        deleteNotifyConfigs(id).then(() => {
          handleGetNotifyConfigs();
        });
      },
    });
  };

  const handleSubmit = (values: any) => {
    const { name, channels, notifyItemIds, notifyGroupId } = values;
    if (isEditing) {
      updateNotifyConfigs({
        id: activedData.id,
        withGroup: false,
        notifyGroupId: +notifyGroupId,
        notifyItemIds: map(notifyItemIds, (id) => +id),
        channels: channels.join(','),
      }).then(() => {
        closeModal();
        handleGetNotifyConfigs();
      });
      return;
    }
    createNotifyConfigs({
      ...pick(commonPayload, ['scopeType', 'scopeId']),
      name,
      enabled: false,
      withGroup: false,
      notifyGroupId: +notifyGroupId,
      notifyItemIds: map(notifyItemIds, (id) => +id),
      channels: channels.join(','),
    }).then(() => {
      closeModal();
      handleGetNotifyConfigs();
    });
  };

  const handleCancel = () => {
    setActivedData({});
    closeModal();
  };

  let fieldsList = [
    {
      name: 'name',
      label: i18n.t('dop:notification name'),
      required: true,
      itemProps: {
        disabled: isEditing,
        maxLength: 50,
      },
    },
    {
      name: 'notifyItemIds',
      label: i18n.t('dop:trigger timing'),
      required: true,
      type: 'select',
      options: map(notifyItems, ({ id, displayName }) => ({ name: displayName, value: id })),
      itemProps: {
        mode: 'multiple',
      },
    },
    {
      name: 'notifyGroupId',
      label: i18n.t('dop:select group'),
      required: true,
      getComp: ({ form }: { form: FormInstance }) => {
        return (
          <Select
            onSelect={(id: any) => {
              form && form.setFieldsValue({ channels: [] });
              setActivedGroupId(id);
            }}
          >
            {map(notifyGroups, ({ id, name }) => (
              <Select.Option key={id} value={id}>
                {name}
              </Select.Option>
            ))}
          </Select>
        );
      },
    },
  ] as any[];

  if (activedGroupId) {
    const activedGroup = find(notifyGroups, ({ id }) => id === +activedGroupId);
    fieldsList = [
      ...fieldsList,
      {
        name: 'channels',
        label: i18n.t('notification method'),
        required: true,
        type: 'select',
        options: (activedGroup && allChannelMethods[activedGroup.targets[0].type]) || [],
        itemProps: {
          mode: 'multiple',
        },
      },
    ];
  }

  const columns: Array<ColumnProps<APP_NOTIFY.INotify>> = [
    {
      title: i18n.t('dop:notification name'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('default:notification target'),
      dataIndex: ['notifyGroup', 'targets'],
      ellipsis: true,
      className: 'notify-info',
      width: 200,
      render: (targets) => (
        <div className="flex-div flex truncate">
          <ListTargets targets={targets || []} roleMap={roleMap} />
        </div>
      ),
    },
    {
      title: i18n.t('default:creator'),
      dataIndex: 'creator',
      width: 160,
      render: (text) => userMap[text]?.nick,
    },
    {
      title: i18n.t('default:create time'),
      dataIndex: 'createdAt',
      width: 176,
      render: (text) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('default:operation'),
      dataIndex: 'id',
      width: 160,
      fixed: 'right',
      render: (text, record) => {
        return (
          <div className="table-operations">
            <span className="table-operations-btn" onClick={() => handleEdit(record)}>
              {i18n.t('edit')}
            </span>
            <span
              className="table-operations-btn"
              onClick={() => {
                handleDele(record.id);
              }}
            >
              {i18n.t('delete')}
            </span>
            <Switch
              size="small"
              defaultChecked={record.enabled}
              loading={toggleNotifyConfigsLoading}
              onChange={() => {
                toggleNotifyConfigs({
                  id: text,
                  action: record.enabled ? 'disable' : 'enable',
                }).then(() => {
                  handleGetNotifyConfigs();
                });
              }}
            />
          </div>
        );
      },
    },
  ];

  return (
    <div className="notify-group-manage">
      <Tooltip title={i18n.t('dop:new notification')}>
        <div
          className="notify-group-action hover-active"
          onClick={() => {
            openModal();
          }}
        >
          <Button type="primary">{i18n.t('dop:new notification')}</Button>
        </div>
      </Tooltip>
      <FormModal
        title={`${isEditing ? i18n.t('dop:edit notification') : i18n.t('dop:new notification')}`}
        visible={modalVisible}
        fieldsList={fieldsList}
        formData={activedData}
        onOk={handleSubmit}
        onCancel={handleCancel}
        modalProps={{ destroyOnClose: true }}
      />
      <Spin spinning={getNotifyConfigsLoading}>
        <Table columns={columns} dataSource={notifyConfigs} rowKey="id" pagination={false} scroll={{ x: 800 }} />
      </Spin>
    </div>
  );
};

export default NotifyConfig;
