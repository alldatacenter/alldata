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

import { includes } from 'lodash';
import React from 'react';
import { Checkbox } from 'antd';
import { getChoosenInfo } from 'project/utils/test-case';
import testCaseStore from 'project/stores/test-case';
import { CheckboxChangeEvent } from 'core/common/interface';

interface IProps {
  id: number | string;
  mode: TEST_CASE.PageScope;
}

const getCaseCheckedStatus = (id: any, { isAll, exclude, primaryKeys }: TEST_CASE.ChoosenInfo) => {
  if (!isAll) {
    // 未全选时
    return includes(primaryKeys, id);
  }
  return !includes(exclude, id);
};

const CaseCheckBox = ({ mode, id }: IProps) => {
  const [choosenInfo, modalChoosenInfo] = testCaseStore.useStore((s) => [s.choosenInfo, s.modalChoosenInfo]);
  const { updateChoosenInfo } = testCaseStore.reducers;
  const info = getChoosenInfo(choosenInfo, modalChoosenInfo, mode);
  const checked = getCaseCheckedStatus(id, info);
  return (
    <Checkbox
      onClick={(e: React.MouseEvent<HTMLElement>) => {
        e.stopPropagation();
      }}
      onChange={(e: CheckboxChangeEvent) => {
        updateChoosenInfo({ id, mode, checked: e.target.checked });
      }}
      checked={checked}
    />
  );
};

export default CaseCheckBox;
