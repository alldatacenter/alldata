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
import { Modal, Timeline } from 'antd';
import { get } from 'lodash';
import { DetailsPanel, EmptyHolder, UserInfo } from 'common';
import i18n from 'i18n';
import './index.scss';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import moment from 'moment';

interface IProps {
  visible: boolean;
  dataSource: API_ACCESS.Client;
  onCancel: () => void;
}

const defaultData = {
  client: {} as API_CLIENT.Client,
  contract: {} as API_CLIENT.Contract,
  permission: {} as API_ACCESS.ClientPermission,
} as API_ACCESS.Client;

const { Item: TimeLineItem } = Timeline;

const DetailModal = ({ visible, onCancel, dataSource }: IProps) => {
  const { getOperationRecord } = apiAccessStore.effects;
  const { clearOperationRecord } = apiAccessStore.reducers;
  const [records] = apiAccessStore.useStore((s) => [s.operationRecord]);
  const { client, contract } = { ...defaultData, ...dataSource };
  React.useEffect(() => {
    if (visible) {
      getOperationRecord({ clientID: client.id, contractID: contract.id });
    } else {
      clearOperationRecord();
    }
  }, [clearOperationRecord, client.id, contract.id, getOperationRecord, visible]);
  const fields = [
    {
      label: i18n.t('creator'),
      value: <UserInfo id={get(client, 'creatorID')} />,
    },
    {
      label: i18n.t('client number'),
      value: get(client, 'clientID'),
    },
  ];
  return (
    <Modal
      title={get(client, 'name')}
      visible={visible}
      onCancel={onCancel}
      destroyOnClose
      footer={null}
      className="client-detail-modal"
      width={960}
    >
      <DetailsPanel
        baseInfoConf={{
          title: i18n.t('basic information'),
          panelProps: {
            fields,
          },
        }}
      />
      <div className="p-4 record-list">
        <div className="title text-base text-normal font-medium mb-2">{i18n.t('approval record')}</div>
        {records.length ? (
          <Timeline>
            {records.map(({ createdAt, action, creatorID, id }) => {
              return (
                <TimeLineItem key={id}>
                  <span className="mr-4">{moment(createdAt).format('YYYY-MM-DD HH:mm:ss')}</span>
                  {creatorID ? <span className="mr-4">{<UserInfo id={creatorID} />}</span> : null}
                  <span>{action}</span>
                </TimeLineItem>
              );
            })}
          </Timeline>
        ) : (
          <div className="no-data">
            <EmptyHolder />
          </div>
        )}
      </div>
    </Modal>
  );
};

export default DetailModal;
