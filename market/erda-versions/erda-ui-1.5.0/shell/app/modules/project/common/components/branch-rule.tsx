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
import { useUpdate } from 'common/use-hooks';
import { useEffectOnce } from 'react-use';
import { map } from 'lodash';
import { Button, Table, Popconfirm } from 'antd';
import { FormModal } from 'app/configForm/nusi-form/form-modal';
import branchRuleStore from 'project/stores/branch-rule';
import { WithAuth } from 'user/common';

const envArr = {
  DEV: {
    name: i18n.t('develop'),
    value: 'DEV',
  },
  TEST: {
    name: i18n.t('test'),
    value: 'TEST',
  },
  STAGING: {
    name: i18n.t('staging'),
    value: 'STAGING',
  },
  PROD: {
    name: i18n.t('production'),
    value: 'PROD',
  },
};

interface IProps {
  operationAuth: boolean;
  scopeId: number;
  scopeType: string;
}

const extraFieldsMap = {
  app: [
    {
      label: i18n.t('dop:continuous integration'),
      component: 'switch',
      key: 'isTriggerPipeline',
      defaultValue: false,
      componentProps: {
        checkedChildren: i18n.t('common:yes'),
        unCheckedChildren: i18n.t('common:no'),
      },
      required: true,
      labelTip: i18n.t('dop:code-trigger-CI'),
      type: 'switch',
    },
    {
      label: i18n.t('dop:protected branch'),
      component: 'switch',
      defaultValue: false,
      key: 'isProtect',
      componentProps: {
        checkedChildren: i18n.t('common:yes'),
        unCheckedChildren: i18n.t('common:no'),
      },
      required: true,
      labelTip: i18n.t('dop:protected-branch-form-tip'),
      type: 'switch',
    },
  ],
  project: [
    {
      label: i18n.t('dop:deployment environment'),
      component: 'select',
      key: 'workspace',
      labelTip: i18n.t(
        'dop:Branch binds environment in the platform. The branch code can only be deployed to the environment selected below by CI/CD pipeline.',
      ),
      required: true,
      dataSource: {
        type: 'static',
        static: map(envArr),
      },
      type: 'select',
    },
    {
      label: i18n.t('dop:artifact deployment environment'),
      component: 'select',
      key: 'artifactWorkspace',
      labelTip: i18n.t(
        'dop:The artifact is a release product of pipeline, and the environment selected below can be directly deployed by the artifact of this branch.',
      ),
      required: true,
      componentProps: {
        mode: 'multiple',
      },
      dataSource: {
        type: 'static',
        static: map(envArr),
      },
      type: 'select',
    },
    {
      label: i18n.t('dop:app release confirmation'),
      component: 'switch',
      defaultValue: false,
      key: 'needApproval',
      componentProps: {
        checkedChildren: i18n.t('common:yes'),
        unCheckedChildren: i18n.t('common:no'),
      },
      required: true,
      visible: false,
      labelTip: i18n.t(
        'dop:When enabled, application deployment needs to be reviewed and approved by the project administrator.',
      ),
      type: 'switch',
    },
  ],
};

const extraColumnsMap = {
  app: [
    {
      title: i18n.t('dop:continuous integration'),
      dataIndex: 'isTriggerPipeline',
      render: (val: boolean) => (val ? i18n.t('common:yes') : i18n.t('common:no')),
    },
    {
      title: i18n.t('dop:protected branch'),
      dataIndex: 'isProtect',
      render: (val: boolean) => (val ? i18n.t('common:yes') : i18n.t('common:no')),
    },
  ],
  project: [
    {
      title: i18n.t('dop:deployment environment'),
      dataIndex: 'workspace',
      width: 196,
    },
    {
      title: i18n.t('dop:artifact deployment environment'),
      dataIndex: 'artifactWorkspace',
      width: 244,
    },
    // {
    //   title: i18n.t('dop:app release confirmation'),
    //   dataIndex: 'needApproval',
    //   render: (val: boolean) => (val ? i18n.t('common:yes') : i18n.t('common:no')),
    // },
  ],
};

const BranchRule = (props: IProps) => {
  const { operationAuth, scopeId, scopeType } = props;
  const branchRules = branchRuleStore.useStore((s) => s.branchRules);
  const { addBranchRule, getBranchRules, deleteBranchRule, updateBranchRule, clearBranchRule } = branchRuleStore;
  const isProject = scopeType === 'project';
  const [{ modalVis, editData }, updater, update] = useUpdate({
    modalVis: false,
    editData: undefined as undefined | PROJECT.IBranchRule,
  });

  useEffectOnce(() => {
    getBranchRulesData();
    return () => clearBranchRule();
  });

  const getBranchRulesData = () => {
    getBranchRules({ scopeId, scopeType });
  };

  const fields = [
    {
      label: i18n.t('dop:branch name'),
      component: 'input',
      key: 'rule',
      rules: [
        {
          validator: (val = '') => {
            const valArr = val.split(',');
            const reg = /^[a-zA-Z_]+[\\/\\*\\.\\$@#a-zA-Z0-9_-]*$/;
            let pass = true;
            let tip = '';
            valArr.forEach((item) => {
              if (!reg.test(item)) {
                pass = false;
                tip = i18n.t('separated by comma, start with letters and can contain');
              }
              if (pass && item.includes('*') && item.indexOf('*') !== item.length - 1) {
                pass = false; // 包含*，但*不在末尾
                tip = i18n.t('separated by comma, start with letters and can contain');
              }
            });
            return [pass, tip];
          },
        },
      ],
      componentProps: {
        placeholder: i18n.t('separated by comma, start with letters and can contain'),
      },
      required: true,
      type: 'input',
    },
    ...(extraFieldsMap[scopeType] || []),
    {
      label: i18n.t('description'),
      component: 'textarea',
      key: 'desc',
      rules: [
        {
          max: '50',
          msg: i18n.t('dop:within {num} characters', { num: 50 }),
        },
      ],
      componentProps: {
        placeholder: i18n.t('dop:within {num} characters', { num: 50 }),
      },
      type: 'textarea',
    },
  ];

  const columns = [
    {
      title: i18n.t('dop:branch'),
      dataIndex: 'rule',
      width: 200,
    },
    ...(extraColumnsMap[scopeType] || []),
    {
      title: i18n.t('description'),
      dataIndex: 'desc',
    },
    {
      title: i18n.t('operation'),
      key: 'operation',
      fixed: 'right',
      width: 160,
      align: 'center',
      render: (_: any, record: PROJECT.IBranchRule) => {
        return (
          <div className="table-operations">
            <WithAuth pass={operationAuth}>
              <span
                className="table-operations-btn"
                onClick={() =>
                  update({
                    modalVis: true,
                    editData: {
                      ...record,
                      ...(isProject
                        ? { artifactWorkspace: ((record.artifactWorkspace as string) || '').split(',') }
                        : {}),
                    },
                  })
                }
              >
                {i18n.t('edit')}
              </span>
            </WithAuth>
            <Popconfirm
              title={`${i18n.t('common:confirm to delete')}?`}
              onConfirm={() => {
                deleteBranchRule({ id: record.id }).then(() => {
                  getBranchRulesData();
                });
              }}
            >
              <WithAuth pass={operationAuth}>
                <span className="table-operations-btn">{i18n.t('delete')}</span>
              </WithAuth>
            </Popconfirm>
          </div>
        );
      },
    },
  ];

  const onCancel = () => {
    update({
      modalVis: false,
      editData: undefined,
    });
  };

  const onFinish = (value: PROJECT.IBranchRule, isEdit: boolean) => {
    if (isEdit) {
      const postData = { ...editData, ...value };
      if (isProject) {
        postData.artifactWorkspace = ((postData.artifactWorkspace as string[]) || []).join(',');
      }
      return updateBranchRule(postData).then(() => {
        onCancel();
        getBranchRulesData();
      });
    } else {
      const postData = {
        ...value,
        ...(isProject ? { artifactWorkspace: (value.artifactWorkspace as string[]).join(',') } : {}),
        scopeId,
        scopeType,
      };

      return addBranchRule(postData).then(() => {
        onCancel();
        getBranchRulesData();
      });
    }
  };
  return (
    <div>
      <div className="mb-3">
        <WithAuth pass={operationAuth}>
          <Button ghost type="primary" onClick={() => updater.modalVis(true)}>
            {i18n.t('dop:new branch rule')}
          </Button>
        </WithAuth>
      </div>
      <Table rowKey="id" dataSource={branchRules} columns={columns} scroll={{ x: 900 }} />
      <FormModal
        name={i18n.t('dop:branch rule')}
        onCancel={onCancel}
        onOk={onFinish}
        visible={modalVis}
        fieldList={fields}
        formData={editData}
      />
    </div>
  );
};

export default BranchRule;
