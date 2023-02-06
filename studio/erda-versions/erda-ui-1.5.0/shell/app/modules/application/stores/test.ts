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

import * as TestServices from '../services/test';
import layoutStore from 'layout/stores/layout';
import i18n from 'i18n';
import { createStore } from 'core/cube';
import { getDefaultPaging, goTo } from 'common/utils';

interface IParams {
  projectId: string;
  appId: string;
}

const getSubList = (params: IParams) => {
  return [
    {
      text: i18n.t('dop:quality reports'),
      href: goTo.resolve.appCodeQualityReports(params),
    },
    {
      text: i18n.t('dop:issues'),
      href: goTo.resolve.appCodeQualityIssueOpen(params),
      prefix: `${goTo.resolve.appCodeQualityIssue(params)}/`,
    },
    {
      text: i18n.t('dop:lists'),
      href: goTo.resolve.appCodeQuality(params),
    },
  ];
};

const initState = {
  testTypes: [],
  list: [],
  testListQueryParams: {},
  testDetail: {},
  testDetailCache: {},
  taskList: [],
  activeItemId: '',
  testListPaging: getDefaultPaging(),
};

const test = createStore({
  name: 'applicationTest',
  subscriptions({ listenRoute }: IStoreSubs) {
    const setSubList = (params: IParams) =>
      layoutStore.reducers.setSubSiderSubList({
        test: getSubList(params),
      });
    listenRoute(({ isIn, params }: IRouteInfo) => {
      if (isIn('application')) {
        setSubList(params as any);
      }
    });
  },
  state: initState,
  effects: {
    async getTestTypes({ call, update }) {
      const testTypes = await call(TestServices.getTestTypes);
      update({ testTypes });
    },
    async getTestList({ select, call, update, getParams }, payload: { pageNo?: number }) {
      const { appId: applicationId } = getParams();
      const { list, total } = await call(
        TestServices.getTestList,
        { applicationId, ...payload },
        { paging: { key: 'testListPaging' } },
      );
      const preList = select((state) => state.list);
      const newList = preList.concat(list || []);

      update({
        list: newList,
        testListQueryParams: { applicationId, ...payload },
      });

      return {
        list: list || [],
        total,
      };
    },
    async getTestDetail({ call, select, update, getParams }) {
      test.reducers.clearTestDetail();
      const { testId } = getParams();
      const testDetailCache = select((state) => state.testDetailCache);
      let testDetail = testDetailCache[testId];
      if (testDetail) {
        update({ testDetail: { ...testDetail } });
        return;
      }
      testId && (testDetail = await call(TestServices.getTestDetail, testId));
      update({ testDetail, testDetailCache: { ...testDetailCache, [testId]: testDetail } });
    },
  },
  reducers: {
    clearTestDetail(state) {
      return { ...state, testDetail: {} };
    },
    clearApplicationTest(state) {
      const forClear = {
        testTypes: {},
        testPaging: {
          list: [],
          total: 0,
        },
        testDetail: {},
        activeItemId: '',
      };
      return { ...state, ...forClear };
    },
    clearTestList(state) {
      return { ...state, list: [] };
    },
  },
});

export default test;
