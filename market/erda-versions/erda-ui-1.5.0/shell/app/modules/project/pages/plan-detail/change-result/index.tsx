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
import { size } from 'lodash';
import { Button, message } from 'antd';
import testCaseStore from 'project/stores/test-case';
import MetaModal from '../../test-manage/components/meta-modal';
import { TestOperation } from 'project/pages/test-manage/constants';

const ChangeResult = () => {
  const {
    caseTotal,
    choosenInfo: { primaryKeys },
  } = testCaseStore.useStore((s) => s);
  const { openNormalModal } = testCaseStore.reducers;

  const checked = size(primaryKeys);

  const onClick = () => {
    if (!caseTotal || !checked) {
      message.error(i18n.t('dop:After the use case is selected, the batch operation can be performed.'));
      return;
    }
    openNormalModal(TestOperation.testPlanTestCasesExecutionResult);
  };

  return (
    <>
      <Button onClick={onClick}>{i18n.t('dop:change execution result')}</Button>
      <MetaModal />
    </>
  );
};

export default ChangeResult;
