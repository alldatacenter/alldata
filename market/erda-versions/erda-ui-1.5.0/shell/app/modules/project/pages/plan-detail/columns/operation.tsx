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
import i18n from 'i18n';
import { Modal } from 'antd';
import { TableActions } from 'common';
import testPlanStore from 'project/stores/test-plan';
import testEnvStore from 'project/stores/test-env';
import { StatusToggle } from '../status-toggle';

interface IProps {
  record: TEST_CASE.CaseTableRecord;
  afterDelete: (data: number[]) => void;
}

const Operation = ({ record, afterDelete }: IProps) => {
  const { updateCasesStatus, deleteRelations } = testPlanStore.effects;
  const { openEnvVariable } = testEnvStore;

  const onDelete = (id: number) => {
    Modal.confirm({
      title: i18n.t('remove'),
      content: i18n.t('dop:are you sure to remove the relevant use cases from the current plan?'),
      onOk: () => {
        deleteRelations({ relationIDs: [id], type: 'single' }).then(afterDelete);
      },
    });
  };

  const handleUpdate = (status: string) => {
    updateCasesStatus({ relationIDs: [record.id], execStatus: status });
  };

  return (
    <TableActions>
      <StatusToggle state={record.execStatus} onChange={handleUpdate} />
      <span className="fake-link ml-2" onClick={() => onDelete(record.id)}>
        {i18n.t('remove')}
      </span>
      <span
        className="fake-link ml-2"
        onClick={() => {
          openEnvVariable({ envID: record.testCaseID, envType: 'case' });
        }}
      >
        {i18n.t('runtime:environment variable configs')}
      </span>
    </TableActions>
  );
};

export default Operation;
