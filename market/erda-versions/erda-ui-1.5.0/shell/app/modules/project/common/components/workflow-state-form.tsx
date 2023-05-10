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
import routeInfoStore from 'core/stores/route';
import { Form } from 'dop/pages/form-editor/index';
import { issueMainStateMap } from 'project/common/components/issue/issue-state';
import { map } from 'lodash';
import { Select } from 'antd';
import issueWorkflowStore from 'project/stores/issue-workflow';

const { Option } = Select;
interface IWorkflowFormProps {
  issueType: ISSUE_WORKFLOW.IIssueType;
  onOk: () => void;
  onCancel: () => void;
}

const WorkflowStateForm = React.forwardRef(({ issueType, onOk, onCancel }: IWorkflowFormProps) => {
  const { projectId: projectID } = routeInfoStore.getState((s) => s.params);
  const { addIssueState } = issueWorkflowStore._effects;

  const formRef = React.useRef(null as any);

  const onAdd = () => {
    const curForm = formRef && formRef.current;
    if (curForm) {
      curForm.onSubmit((values: any) => {
        addIssueState({
          ...values,
          issueType,
          projectID: +projectID,
        }).then(() => {
          onOk();
        });
        onCancel();
      });
    }
  };

  const fields = React.useMemo(
    () => [
      {
        label: '',
        component: 'input',
        key: 'stateName',
        required: true,
        componentProps: {
          style: { width: '320px' },
          placeholder: i18n.t('please enter {name}', { name: i18n.t('dop:state display name') }),
        },
        rules: [{ min: 1, max: 7, msg: i18n.t('length is {min}~{max}', { min: 1, max: 7 }) }],
      },
      {
        label: '',
        component: 'select',
        key: 'stateBelong',
        required: true,
        componentProps: {
          style: { width: '320px' },
          placeholder: i18n.t('please select {name}', { name: i18n.t('dop:owned status') }),
        },
        dataSource: {
          type: 'static',
          static: () => {
            const optionMap = issueMainStateMap[issueType];
            return map(Object.keys(optionMap), (value) => {
              return (
                <Option key={value} value={value}>
                  {optionMap[value]?.stateName}
                </Option>
              );
            });
          },
        },
        type: 'select',
      },
    ],
    [issueType],
  );

  return (
    <div className={'backlog-issue-form flex justify-start items-start'}>
      <div className={'backlog-issue-form-box'}>
        <Form fields={fields} formRef={formRef} formProps={{ layout: 'inline', className: 'backlog-issue-add' }} />
      </div>
      <div className="table-operations ml-2 mt-2">
        <span className="table-operations-btn" onClick={onAdd}>
          {i18n.t('save')}
        </span>
        <span className="table-operations-btn" onClick={onCancel}>
          {i18n.t('cancel')}
        </span>
      </div>
    </div>
  );
});

export default React.memo(WorkflowStateForm);
