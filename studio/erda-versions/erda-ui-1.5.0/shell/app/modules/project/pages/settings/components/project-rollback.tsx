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
import { Table } from 'antd';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import i18n from 'i18n';
import projectStore from 'project/stores/project';
import { WORKSPACE_LIST } from 'common/constants';

interface IProps {
  hasEditAuth: boolean;
}

const workSpaceMap = {
  DEV: i18n.t('dev environment'),
  TEST: i18n.t('test environment'),
  STAGING: i18n.t('staging environment'),
  PROD: i18n.t('prod environment'),
};
export default ({ hasEditAuth }: IProps) => {
  const info = projectStore.useStore((s) => s.info);
  const { updateProject } = projectStore.effects;
  const { rollbackConfig } = info;

  const configData = {};
  const tableData: object[] = [];
  const fieldsList: object[] = [];
  const sortBy = WORKSPACE_LIST;
  sortBy.forEach((workspace) => {
    const name = workspace.toUpperCase();
    const point = rollbackConfig?.[workspace];

    tableData.push({ workspace, point });
    configData[`${name}`] = point || 5;
    fieldsList.push({
      label: workSpaceMap[name] || name,
      name: ['rollbackConfig', name],
      type: 'inputNumber',
      itemProps: {
        max: 1000,
        min: 1,
        precision: 0,
      },
    });
  });

  const readonlyForm = (
    <Table
      rowKey="workspace"
      dataSource={tableData}
      columns={[
        {
          key: 'workspace',
          title: i18n.t('dop:environments'),
          width: '200',
          dataIndex: 'workspace',
          render: (val: string) => workSpaceMap[val] || val,
        },
        {
          title: i18n.t('dop:rollback point'),
          width: '400',
          dataIndex: 'point',
          align: 'left',
        },
      ]}
      pagination={false}
      scroll={{ x: '100%' }}
    />
  );

  return (
    <SectionInfoEdit
      hasAuth={hasEditAuth}
      data={{ rollbackConfig: configData }}
      readonlyForm={readonlyForm}
      fieldsList={fieldsList}
      updateInfo={updateProject}
      name={i18n.t('dop:rollback setting')}
      // desc={i18n.t('')}
      formName={i18n.t('dop:rollback setting')}
    />
  );
};
