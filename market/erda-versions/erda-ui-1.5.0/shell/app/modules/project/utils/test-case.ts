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

import { forEach, isEqual } from 'lodash';
import i18n from 'i18n';

export const getChoosenInfo = (
  choosenInfo: TEST_CASE.ChoosenInfo,
  modalChoosenInfo: TEST_CASE.ChoosenInfo,
  scope?: TEST_CASE.PageScope,
): TEST_CASE.ChoosenInfo => (scope === 'caseModal' ? modalChoosenInfo : choosenInfo);
export const getChoosenName = (scope?: TEST_CASE.PageScope) =>
  scope === 'caseModal' ? 'modalChoosenInfo' : 'choosenInfo';
export const getCaseListName = (scope?: TEST_CASE.PageScope) => (scope === 'caseModal' ? 'modalCaseList' : 'caseList');

const filterList = [
  'pageNo',
  'pageSize',
  'orderBy',
  'orderRule',
  'projectId',
  'selectProjectId',
  'testPlanId',
  'testSetId',
];
const getCompareObject = (query: object) => {
  const temp = {};
  forEach(query, (value, key) => {
    if (filterList.includes(key)) {
      return;
    }
    temp[key] = value;
  });
  return temp;
};
// 当筛选器发生变化时，需要清空选中的ids
export const checkNeedEmptyChoosenIds = (oldQuery: object, newQuery: object) =>
  !isEqual(getCompareObject(oldQuery), getCompareObject(newQuery));

export const colorMap = {
  [i18n.t('dop:pass')]: '#25ca64',
  [i18n.t('dop:not passed')]: '#ff4946',
  [i18n.t('dop:not performed')]: '#bbbbbb',
  [i18n.t('dop:blocking')]: '#ffc11f',
};

export const caseIDMap: { [k in TEST_CASE.PageScope]: string } = {
  caseModal: 'testCaseID',
  testPlan: 'planRelationCaseID',
  testCase: 'planRelationCaseID',
};

const orderMap = {
  updatedId: {
    ASC: 'orderByUpdaterIDAsc',
    DESC: 'orderByUpdaterIDDesc',
  },
  updatedAt: {
    ASC: 'orderByUpdatedAtAsc',
    DESC: 'orderByUpdatedAtDesc',
  },
  priority: {
    ASC: 'orderByPriorityAsc',
    DESC: 'orderByPriorityDesc',
  },
  id: {
    ASC: 'orderByIDAsc',
    DESC: 'orderByIDDesc',
  },
};

export const formatQuery = ({
  orderBy,
  orderRule,
  ...rest
}: Record<string, any>): Merge<TEST_CASE.QueryCase, TEST_CASE.QueryCaseSort> => {
  const data = { ...rest } as TEST_CASE.QueryCase;
  if (orderRule && orderBy) {
    const orderType = orderMap[orderBy] && orderMap[orderBy][orderRule];
    if (orderType) {
      data[orderType] = true;
      data.orderField = data.orderField || [];
      data.orderField.push(orderBy);
    }
  }
  return data;
};
