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
import { Collapse, message } from 'antd';
import blackListStore from '../../stores/blacklist';
import eraseListStore from '../../stores/erase';
import { WithAuth, usePerm } from 'user/common';
import { CRUDTable, Copy } from 'common';
import i18n from 'i18n';
import moment from 'moment';

const { Panel } = Collapse;

interface IProps {
  artifacts: PUBLISHER.IArtifacts;
}
interface IListProps {
  artifactId: string;
}

const EraseStatus = {
  success: i18n.t('succeed'),
  failure: i18n.t('publisher:failure'),
  erasing: i18n.t('publisher:erasing'),
};
const BlackList = ({ artifactId }: IListProps) => {
  const publishOperationAuth = usePerm((s) => s.org.publisher.operation.pass);

  const getColumns = ({ deleteItem }: any, { reloadList }: any) => [
    {
      title: i18n.t('user ID'),
      dataIndex: 'userId',
      render: (val: string) => (
        <span className="cursor-copy" data-clipboard-tip={i18n.t('user ID')} data-clipboard-text={val}>
          {val}
        </span>
      ),
    },
    {
      title: i18n.t('user name'),
      dataIndex: 'userName',
      render: (val: string) => (
        <span className="cursor-copy" data-clipboard-tip={i18n.t('user name')} data-clipboard-text={val}>
          {val}
        </span>
      ),
    },
    {
      title: i18n.t('device ID'),
      dataIndex: 'deviceNo',
      render: (val: string) => (
        <span className="cursor-copy" data-clipboard-tip={i18n.t('device ID')} data-clipboard-text={val}>
          {val}
        </span>
      ),
    },
    {
      title: i18n.t('create time'),
      dataIndex: 'createdAt',
      width: 200,
      render: (v: string) => (v ? moment(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'op',
      width: 120,
      render: (_v: any, record: PUBLISHER.IBlackList) => {
        return (
          <div className="table-operations">
            <WithAuth pass={publishOperationAuth}>
              <span
                className="table-operations-btn"
                onClick={() => {
                  deleteItem({ blacklistId: record.id, artifactId }).then(() => {
                    reloadList();
                  });
                }}
              >
                {i18n.t('remove')}
              </span>
            </WithAuth>
          </div>
        );
      },
    },
  ];
  const getFieldsList = () => {
    const fieldsList = [
      {
        label: i18n.t('user ID'),
        name: 'userId',
        required: false,
        itemProps: {
          maxLength: 100,
        },
      },
      {
        label: i18n.t('device ID'),
        name: 'deviceNo',
        required: false,
        itemProps: {
          maxLength: 100,
        },
      },
    ];
    return fieldsList;
  };
  return (
    <CRUDTable.StoreTable<PUBLISHER.IBlackList>
      name={i18n.t('publisher:blacklist')}
      rowKey={(r) => r.userId + r.deviceNo}
      getColumns={getColumns}
      store={blackListStore}
      getFieldsList={getFieldsList}
      hasAddAuth={publishOperationAuth}
      extraQuery={{ artifactId }}
      handleFormSubmit={(data, { addItem }) => {
        if (!data.userId && !data.deviceNo) {
          message.warn(i18n.t('publisher:User id and device id cannot be empty at the same time'));
          return Promise.reject();
        }
        return addItem({ ...data, artifactId });
      }}
    />
  );
};

const EraseList = ({ artifactId }: IListProps) => {
  const publishOperationAuth = usePerm((s) => s.org.publisher.operation.pass);

  const getColumns = () => [
    {
      title: i18n.t('device ID'),
      dataIndex: 'deviceNo',
      render: (val: string) => (
        <span className="cursor-copy" data-clipboard-tip={i18n.t('device ID')} data-clipboard-text={val}>
          {val}
        </span>
      ),
    },
    {
      title: i18n.t('create time'),
      dataIndex: 'createdAt',
      render: (v: string) => (v ? moment(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: i18n.t('status'),
      dataIndex: 'eraseStatus',
      width: 120,
      render: (val: string) => EraseStatus[val],
    },
  ];

  const getFieldsList = () => {
    const fieldsList = [
      {
        label: i18n.t('device ID'),
        name: 'deviceNo',
        itemProps: {
          maxLength: 100,
        },
      },
    ];
    return fieldsList;
  };
  return (
    <CRUDTable.StoreTable<PUBLISHER.IErase>
      name={i18n.t('publisher:data erase')}
      rowKey="deviceNo"
      getColumns={getColumns}
      store={eraseListStore}
      getFieldsList={getFieldsList}
      hasAddAuth={publishOperationAuth}
      extraQuery={{ artifactId }}
      handleFormSubmit={(data, { addItem }) => {
        return addItem({ ...data, artifactId });
      }}
    />
  );
};

const SafetyManage = (props: IProps) => {
  const { artifacts } = props;
  const artifactId = artifacts.id;
  return (
    <div className="safety-manage">
      <Collapse defaultActiveKey="blackList">
        <Panel header={i18n.t('publisher:blacklist')} key="blackList">
          <BlackList artifactId={artifactId} />
        </Panel>
        <Panel header={i18n.t('publisher:erase data')} key="eraseData">
          <EraseList artifactId={artifactId} />
        </Panel>
      </Collapse>
      <Copy selector=".cursor-copy" />
    </div>
  );
};

export default SafetyManage;
