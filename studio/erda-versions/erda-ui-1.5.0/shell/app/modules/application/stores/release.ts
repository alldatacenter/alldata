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

import { createStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import { getReleaseList, updateInfo, getReleaseDetail, getDiceYml } from '../services/release';
import i18n from 'i18n';

interface IState {
  list: RELEASE.detail[];
  paging: IPaging;
  detail: RELEASE.detail;
}

const initState: IState = {
  list: [],
  paging: getDefaultPaging(),
  detail: {} as RELEASE.detail,
};

const release = createStore({
  name: 'release',
  state: initState,
  effects: {
    async getReleaseList({ call, update, getParams, getQuery }, payload: RELEASE.ListQuery) {
      const params = getParams();
      const query = getQuery();
      const { list = [], total } = await call(
        getReleaseList,
        { applicationId: params.appId, q: query.q, ...payload },
        { paging: { key: 'paging' } },
      );
      update({ list });
      return { list, total };
    },
    async updateInfo({ call }, payload: RELEASE.UpdateBody) {
      await call(updateInfo, payload, { successMsg: i18n.t('dop:modified successfully') });
      const params = { pageNo: 1, pageSize: 10 }; // 修改后的记录会被排序到第一条，故重新请求第一页
      await release.effects.getReleaseList(params);
    },
    async getReleaseDetail({ call, update }, releaseId: string) {
      const detail = await call(getReleaseDetail, releaseId);
      update({ detail });
    },
    async getDiceYml({ call }, releaseId: string) {
      return call(getDiceYml, releaseId, { fullResult: true });
    },
  },
  reducers: {
    clearReleaseDetail(state) {
      state.detail = {} as RELEASE.detail;
    },
    clearReleaseList(state) {
      state.list = [];
    },
  },
});

export default release;
