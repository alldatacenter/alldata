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

import { DropdownSelect } from 'common';
import testCaseStore from 'project/stores/test-case';
import { message } from 'antd';

interface IProps {
  afterExport?: () => void;
}

const ExportFile = ({ afterExport }: IProps) => {
  const { exportFile } = testCaseStore.effects;

  const onExport = (e: any) => {
    exportFile(e.key).then(() => {
      message.success(i18n.t('dop:The export task has been created, please check the progress in the record'), 4);
      afterExport?.();
    });
  };

  return (
    <DropdownSelect
      menuList={[
        { key: 'excel', name: i18n.t('dop:export Excel') },
        { key: 'xmind', name: i18n.t('dop:export Xmind') },
      ]}
      onClickMenu={onExport}
      buttonText={i18n.t('export')}
      btnProps={{
        type: 'primary',
        ghost: true,
      }}
    />
  );
};

export default ExportFile;
