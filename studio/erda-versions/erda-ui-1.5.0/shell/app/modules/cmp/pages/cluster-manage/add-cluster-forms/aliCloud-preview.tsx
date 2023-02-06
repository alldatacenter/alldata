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
import { find, get, map } from 'lodash';
import { Modal, Button, Table, Checkbox } from 'antd';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { chargeTypeMap, chargePeriodMap } from '../config';

const AliCloudPreview = ({
  visible,
  onClose,
  onOk,
  dataSource,
}: {
  visible: boolean;
  onClose: Function;
  onOk: Function;
  dataSource: any[];
}) => {
  const [{ checkedRead }, updater] = useUpdate({
    checkedRead: false,
  });

  const columns = [
    {
      title: i18n.t('cmp:product type'),
      dataIndex: 'resourceType',
      width: 180,
    },
    {
      title: i18n.t('cmp:product configuration'),
      dataIndex: 'resourceProfile',
      render: (value: string[]) => {
        return map(value, (item) => <div key={item}>{item}</div>);
      },
    },
    {
      title: i18n.t('cmp:number of products'),
      dataIndex: 'resourceNum',
      width: 100,
    },
    {
      title: i18n.t('cmp:billing method'),
      dataIndex: 'chargeType',
      width: 100,
      render: (v: string) => get(chargeTypeMap, `${v}.name`),
    },
    {
      title: i18n.t('cmp:purchase time'),
      dataIndex: 'chargePeriod',
      width: 100,
      render: (v: string) => get(find(chargePeriodMap, { value: Number(v) }), 'name', '-'),
    },
  ];

  const cancelConfirm = () => {
    updater.checkedRead(false);
    onClose();
  };

  const handleConfirm = () => {
    onOk();
    cancelConfirm();
  };

  return (
    <Modal
      closable={false}
      maskClosable={false}
      width={1000}
      destroyOnClose
      title={i18n.t('cmp:confirm configuration')}
      visible={visible}
      footer={[
        <Button key="cancel" onClick={cancelConfirm}>
          {i18n.t('cancel')}
        </Button>,
        <Button key="submit" type="primary" disabled={!checkedRead} onClick={handleConfirm}>
          {i18n.t('ok')}
        </Button>,
      ]}
    >
      <Table
        rowKey={'resourceType'}
        pagination={false}
        columns={columns}
        dataSource={dataSource}
        scroll={{ x: '100%' }}
      />
      <Checkbox className="mt-3" onChange={() => updater.checkedRead(!checkedRead)} checked={checkedRead} />{' '}
      {i18n.t('cmp:i have confirmed')}
    </Modal>
  );
};

export default AliCloudPreview;
