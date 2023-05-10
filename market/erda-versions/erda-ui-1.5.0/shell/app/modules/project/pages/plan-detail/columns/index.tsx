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
import { Progress } from 'antd';
import { commonColumns } from '../../test-manage/case/columns';
import Operation from './operation';
import { ColumnProps } from 'core/common/interface';
import { UserInfo } from 'common';

const planDetailColumns = [...commonColumns];
// 放在更新人前面
planDetailColumns.splice(
  4,
  0,
  {
    title: i18n.t('dop:interface pass rate'),
    dataIndex: 'apiPassedCount',
    key: 'apiPass',
    width: 140,
    render: (_text: any, record: TEST_CASE.CaseTableRecord) => {
      const { total, passed } = record.apiCount || {};
      const percent = record.apiCount ? ((passed || 0) * 100) / total : 0;
      return (
        record.id && (
          <div className="mr-6">
            <Progress percent={Math.round(percent)} format={() => `${passed || 0}/${total || 0}`} />
          </div>
        )
      );
    },
  },
  {
    title: i18n.t('dop:executor'),
    dataIndex: 'executorID',
    key: 'executorID',
    width: 100,
    render: (text) => <UserInfo id={text} render={(data) => data.nick || data.name} />,
  },
);

export const getColumns = ({ afterDelete }: { afterDelete: (data: number[]) => void }) =>
  [
    ...planDetailColumns,
    {
      title: i18n.t('operation'),
      dataIndex: 'operation',
      key: 'operation',
      width: 280,
      fixed: 'right',
      render: (_text: any, record: TEST_CASE.CaseTableRecord) =>
        record.id && <Operation afterDelete={afterDelete} record={record} />,
    },
  ] as Array<ColumnProps<TEST_CASE.CaseTableRecord>>;
