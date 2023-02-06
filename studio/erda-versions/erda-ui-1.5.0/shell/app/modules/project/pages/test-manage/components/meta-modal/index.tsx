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

import { find, get, isEmpty } from 'lodash';
import React, { useEffect } from 'react';
import { FormComponentProps } from 'core/common/interface';

import { filterOption } from 'common/utils';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';
import { TestOperation } from 'project/pages/test-manage/constants';
import { FormModal } from 'common';

interface IProps extends FormComponentProps {
  onOk?: (values: any) => void;
  labelName?: string;
}

const updateEventMap = {
  [TestOperation.priority]: 'updatePriority',
  [TestOperation.testPlanTestCasesExecutionResult]: 'changeExecutionResult',
};

const noop = () => {
  throw new Error('function is required');
};

const MetaModal = ({ labelName: oldLabelName, onOk: oldOnOk }: IProps) => {
  const { getFields } = testCaseStore.effects;
  const { closeNormalModal } = testCaseStore.reducers;
  const [oldCaseAction, metaFields] = testCaseStore.useStore((s) => [s.caseAction, s.metaFields]);
  const [confirmLoading] = useLoading(testCaseStore, [`${updateEventMap[oldCaseAction]}`]);
  const caseAction = (defaultMetaActions.includes(oldCaseAction) ? oldCaseAction : '') as string;
  const meta = find(metaFields, ({ uniqueName }) => uniqueName === caseAction);
  const list = get(meta, 'enums', emptyList) as TEST_CASE.MetaEnum[];
  const labelName = oldLabelName || get(meta, 'showName', '');

  useEffect(() => {
    if (isEmpty(list) && caseAction) {
      getFields();
    }
  }, [caseAction, getFields, list]);

  const onOk = (data: any) => {
    const func = oldOnOk || testCaseStore.effects[updateEventMap[oldCaseAction]] || noop;
    func(data[caseAction]).finally(() => {
      closeNormalModal();
    });
  };

  const fieldList = [
    {
      label: labelName,
      name: caseAction || 'temp',
      type: 'select',
      options: list.map(({ showName, value }) => ({ value, name: showName })),
      itemProps: {
        showSearch: true,
        filterOption,
      },
    },
  ];

  return (
    <FormModal
      title={labelName}
      fieldsList={fieldList}
      visible={!!caseAction}
      onCancel={closeNormalModal}
      onOk={onOk}
      modalProps={{
        confirmLoading,
      }}
    />
  );
};

const defaultMetaActions = [TestOperation.priority, TestOperation.testPlanTestCasesExecutionResult];
const emptyList: TEST_CASE.MetaEnum[] = [];

export default MetaModal;
