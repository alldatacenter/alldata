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

import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import moment from 'moment';
import { Drawer, Tooltip, Modal } from 'antd';
import React from 'react';
import DeploymentTable from 'runtime/common/components/deployment-table';
import runtimeStore from 'runtime/stores/runtime';
import { useLoading } from 'core/stores/loading';

export default ({ visible, onClose }: { visible: boolean; onClose: (e?: any) => void }) => {
  const [deploymentRecords, paging] = runtimeStore.useStore((s) => [s.deploymentRecords, s.deploymentRecordsPaging]);
  const [loading] = useLoading(runtimeStore, ['getRollbackList']);

  const rollbackRuntime = (id: number) => {
    runtimeStore.rollbackRuntime(id);
    onClose();
  };

  const confirmRollback = (record: any) => {
    const { id, releaseId, createdAt } = record;
    const content = (
      <div className="record-info">
        <div>
          <span className="info-name">ReleaseId：</span>
          <span>{releaseId}</span>
        </div>
        <div>
          <span className="info-name">{i18n.t('runtime:deploy at')}：</span>
          <span>{moment(createdAt).format('YYYY-MM-DD HH:mm:ss')}</span>
        </div>
      </div>
    );
    Modal.confirm({
      title: i18n.t('runtime:confirm rollback to this deploy?'),
      content,
      width: 500,
      onOk: () => rollbackRuntime(id),
    });
  };

  const opsCol = {
    title: i18n.t('operate'),
    width: 70,
    render: (record: any) => {
      const { outdated, status: depStatus } = record;
      let title = i18n.t('runtime:rollback');
      let disabled = false;

      if (depStatus !== 'OK') {
        title = i18n.t('runtime:deployment failed and cannot roll back');
        disabled = true;
      }

      if (outdated) {
        title = i18n.t('runtime:deployment expired and cannot roll back');
        disabled = true;
      }
      return (
        <Tooltip title={title}>
          {disabled ? (
            <CustomIcon className="hover-active not-allowed" type="rollback" />
          ) : (
            <CustomIcon className="hover-active" onClick={() => confirmRollback(record)} type="rollback" />
          )}
        </Tooltip>
      );
    },
  };

  return (
    <Drawer title={i18n.t('runtime:rollback history')} visible={visible} onClose={onClose} width="80%">
      <DeploymentTable
        dataSource={deploymentRecords}
        paging={paging}
        loading={loading}
        opsCol={opsCol}
        onChange={(no: number) => runtimeStore.getRollbackList({ ...paging, pageNo: no })}
      />
    </Drawer>
  );
};
