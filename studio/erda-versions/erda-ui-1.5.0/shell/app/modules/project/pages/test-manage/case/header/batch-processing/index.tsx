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

import { size } from 'lodash';
import React from 'react';
import i18n from 'i18n';
import { message, Modal } from 'antd';

import { DropdownSelect } from 'common';
import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import testSetStore from 'project/stores/test-set';

import TagModal from '../tag-modal';
import PlanModal from '../plan-modal';
import { MetaModal } from '../../../components';
import TestEnvDrawer from '../../test-env-drawer';
import { TestOperation } from '../../../constants';
import { rootId } from 'project/pages/test-manage/components/case-tree/utils';

interface IProps {
  recycled: boolean;
}

const BatchProcessing = ({ recycled }: IProps) => {
  const [caseTotal, choosenInfo] = testCaseStore.useStore((s) => [s.caseTotal, s.choosenInfo]);
  const { isAll, primaryKeys } = choosenInfo;
  const { openPlanModal } = testPlanStore.reducers;
  const { openTreeModal } = testSetStore.reducers;
  const { openNormalModal } = testCaseStore.reducers;
  const { toggleToRecycle, deleteEntirely } = testCaseStore.effects;

  const checked = isAll || size(primaryKeys);

  const onClick = ({ key }: any) => {
    if (!caseTotal || !checked) {
      message.error(i18n.t('dop:After the use case is selected, the batch operation can be performed.'));
      return;
    }
    switch (key) {
      case TestOperation.delete:
        Modal.confirm({
          title: i18n.t('delete'),
          content: i18n.t('dop:are you sure to delete the currently selected use case?'),
          onOk: () => {
            toggleToRecycle({ testCaseIDs: primaryKeys, recycled: true, moveToTestSetID: rootId });
          },
        });
        break;
      case TestOperation.plan:
        openPlanModal({ type: 'multi' });
        break;
      case TestOperation.copy:
      case TestOperation.move:
      case TestOperation.recover:
        openTreeModal({ type: 'multi', action: key });
        break;
      case TestOperation.tag:
      case TestOperation.priority:
        openNormalModal(key);
        break;
      case TestOperation.deleteEntirely:
        Modal.confirm({
          title: i18n.t('dop:delete completely'),
          content:
            i18n.t('dop:the use case will not be recovered after it is completely deleted, ') +
            i18n.t('is it confirmed?'),
          onOk: () => deleteEntirely(),
        });
        break;
      default:
        break;
    }
  };

  return (
    <>
      <DropdownSelect
        menuList={recycled ? menuItemsMap.recycled : menuItemsMap.normal}
        onClickMenu={onClick}
        buttonText={i18n.t('dop:batch processing')}
        btnProps={{
          type: 'primary',
          ghost: true,
        }}
      />
      <TestEnvDrawer />
      <PlanModal />
      <TagModal />
      <MetaModal />
    </>
  );
};

export default BatchProcessing;

const menuItemsMap = {
  normal: [
    { key: TestOperation.delete, name: i18n.t('delete') },
    {
      key: TestOperation.priority,
      name: i18n.t('dop:update priority'), // children: priorityList.map(v => ({ key: v, name: v })),
    },
    { key: TestOperation.copy, name: i18n.t('dop:copy to') },
    { key: TestOperation.move, name: i18n.t('dop:move to') },
    { key: TestOperation.plan, name: i18n.t('dop:add to test plan') },
  ],
  recycled: [
    { key: TestOperation.recover, name: i18n.t('dop:recover to') },
    { key: TestOperation.deleteEntirely, name: i18n.t('dop:delete completely') },
  ],
};
