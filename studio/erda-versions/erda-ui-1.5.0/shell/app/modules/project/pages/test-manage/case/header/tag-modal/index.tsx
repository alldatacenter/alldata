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

import { map } from 'lodash';
import i18n from 'i18n';
import React, { useEffect } from 'react';
import { filterOption } from 'common/utils';
import { useLoading } from 'core/stores/loading';
import projectLabelStore from 'project/stores/label';
import testCaseStore from 'project/stores/test-case';
import { colors, labelType, TestOperation } from 'project/pages/test-manage/constants';
import { FormModal } from 'common';

const labelColor = (): string => {
  const index = Math.floor(Math.random() * 6);
  return colors[index];
};

const TagModal = () => {
  const list = projectLabelStore.useStore((s) => s.list);
  const { getLabels, createLabel } = projectLabelStore.effects;
  const oldCaseAction = testCaseStore.useStore((s) => s.caseAction);
  // const { updateTags } = testCaseStore.effects;
  const { closeNormalModal } = testCaseStore.reducers;
  const [confirmLoading] = useLoading(testCaseStore, ['updateTags']);
  const caseAction = oldCaseAction === TestOperation.tag ? oldCaseAction : '';
  // const allLabelNames = React.useMemo(() => map(list, t => t.name), [list]);

  useEffect(() => {
    getLabels({ type: labelType } as any);
  }, [caseAction, getLabels]);

  const onOk = (values: any) => {
    // const { labelIds } = values;
    // 3.18 未做
    // const labels = list.filter(({ id }) => labelIds.includes(`${id}`));
  };
  // const handleSelectLabels = (labelNames:string[]) => {
  //   const labelName = cloneDeep(labelNames).pop() as string;
  //   if (isEmpty(labelNames) || includes(allLabelNames, labelName)) {
  //     // update('labels', labelNames);
  //   } else {
  //     // 创建标签
  //     createLabel({
  //       color: labelColor(),
  //       name: labelName,
  //       type: labelType,
  //     }).then(() => {
  //       getLabels({ type: labelType });
  //     });
  //   }
  // };

  const fieldsList = [
    {
      label: i18n.t('dop:search label under project'),
      name: 'labelIds',
      type: 'select',
      options: map(list, ({ id: value, name }) => ({ value, name })),
      itemProps: {
        placeholder: i18n.t('dop:search label under project'),
        filterOption,
        mode: 'tag',
      },
    },
  ];

  return (
    <FormModal
      visible={!!caseAction}
      title={i18n.t('dop:change label')}
      onCancel={closeNormalModal}
      onOk={onOk}
      fieldsList={fieldsList}
      confirmLoading={confirmLoading}
      destroyOnClose
    />
  );
};

export default TagModal;
