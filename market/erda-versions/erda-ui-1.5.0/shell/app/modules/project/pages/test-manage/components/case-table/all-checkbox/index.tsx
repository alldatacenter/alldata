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

import { flatMapDeep } from 'lodash';
import React from 'react';
import { Checkbox } from 'antd';
import { getChoosenInfo } from 'project/utils/test-case';
import testCaseStore from 'project/stores/test-case';
import './index.scss';

interface IProps {
  mode: TEST_CASE.PageScope;
}

const AllCheckBox = ({ mode }: IProps) => {
  const [choosenInfo, modalChoosenInfo, caseList, modalCaseList] = testCaseStore.useStore((s) => [
    s.choosenInfo,
    s.modalChoosenInfo,
    s.caseList,
    s.modalCaseList,
  ]);
  const { triggerChoosenAll } = testCaseStore.reducers;
  let currentList = caseList;
  if (mode === 'caseModal') {
    currentList = modalCaseList;
  }

  const currentPageCaseCount = React.useMemo(
    () => flatMapDeep(currentList, ({ testCases }) => flatMapDeep(testCases, 'id')).length,
    [currentList],
  );
  const info = getChoosenInfo(choosenInfo, modalChoosenInfo, mode);
  const { isAll, primaryKeys } = info;
  const checked = !!primaryKeys.length && primaryKeys.length === currentPageCaseCount;
  const indeterminate = !!primaryKeys.length && primaryKeys.length < currentPageCaseCount;
  return (
    <Checkbox
      onChange={() => triggerChoosenAll({ isAll: !isAll, scope: mode })}
      checked={checked}
      indeterminate={indeterminate}
      className={'test-manage-all-checkbox'}
    />
  );
};

export default AllCheckBox;
