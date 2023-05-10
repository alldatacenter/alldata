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

import i18n from 'i18n';
import React from 'react';
import { Modal } from 'antd';

import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import testEnvStore from 'project/stores/test-env';
import testSetStore from 'project/stores/test-set';
import { TestOperation } from '../../../constants';
import { TableActions } from 'common';
import { rootId } from 'project/pages/test-manage/components/case-tree/utils';

interface IProps {
  record: TEST_CASE.CaseTableRecord;
}

const MoreOperation = ({ record }: IProps) => {
  const { openPlanModal } = testPlanStore.reducers;
  const { openEnvVariable } = testEnvStore;
  const { openTreeModal } = testSetStore.reducers;
  const { toggleToRecycle, deleteEntirely } = testCaseStore.effects;

  const onClick = ({ key, domEvent }: any) => {
    domEvent.stopPropagation();
    switch (key) {
      case TestOperation.delete:
        Modal.confirm({
          title: i18n.t('delete'),
          content: i18n.t('dop:are you sure to delete the current use case?'),
          onOk: () => {
            toggleToRecycle({ testCaseIDs: [record.id], recycled: true, moveToTestSetID: rootId });
          },
        });
        break;
      case TestOperation.deleteEntirely:
        Modal.confirm({
          title: i18n.t('dop:delete completely'),
          content:
            i18n.t('dop:the use case will not be recovered after it is completely deleted, ') +
            i18n.t('is it confirmed?'),
          onOk: () => {
            deleteEntirely(record.id);
          },
        });
        break;
      case TestOperation.plan:
        openPlanModal({ ids: [record.id], type: 'case', selectProjectId: record.projectID });
        break;
      case TestOperation.env:
        openEnvVariable({ envID: +record.id, envType: 'case' });
        break;
      case TestOperation.copy:
      case TestOperation.move:
      case TestOperation.recover:
        openTreeModal({
          ids: [record.id],
          action: key,
          type: 'case',
          extra: {
            projectId: record.projectID,
          },
        });
        break;
      default:
        break;
    }
  };

  const getMenu = () => {
    const { recycled } = record;
    let list = [];
    if (recycled) {
      list = menuItemsMap.recycled;
    } else {
      list = menuItemsMap.normal;
    }
    return (
      // <Menu onClick={onClick}>
      //   {list.map(({ key, name }) => (
      //     <Menu.Item key={key}>
      //       <span>{name}</span>
      //     </Menu.Item>
      //   ))}
      // </Menu>
      <TableActions>
        {list.map(({ key, name }) => (
          <span
            onClick={(e) => {
              onClick({ key, domEvent: e });
            }}
            key={key}
          >
            {name}
          </span>
        ))}
      </TableActions>
    );
  };

  return getMenu();
};

export default MoreOperation;

const referenceMenus = [
  { key: TestOperation.delete, name: i18n.t('delete') },
  { key: TestOperation.copy, name: i18n.t('dop:copy to') },
  { key: TestOperation.move, name: i18n.t('dop:move to') },
];

const menuItemsMap = {
  normal: [
    ...referenceMenus,
    { key: TestOperation.plan, name: i18n.t('dop:add to test plan') },
    { key: TestOperation.env, name: i18n.t('runtime:environment variable configs') },
  ],
  reference: referenceMenus,
  recycled: [
    { key: TestOperation.deleteEntirely, name: i18n.t('dop:delete completely') },
    { key: TestOperation.recover, name: i18n.t('dop:recover to') },
  ],
};
