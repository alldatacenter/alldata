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

import { getFromRepo } from 'application/services/repo';
import { getLatestSonarStatistics, getSonarResults } from '../services/quality';
import routeInfoStore from 'core/stores/route';
import appStore from 'application/stores/application';
import { eventHub } from 'common/utils/event-hub';
import { createStore } from 'core/cube';

const getAppDetail = () =>
  new Promise((resolve) => {
    const { appId } = routeInfoStore.getState((s) => s.params);
    let appDetail = appStore.getState((s) => s.detail);
    const notSameApp = appId && String(appId) !== String(appDetail.id);
    if (!appId || notSameApp) {
      eventHub.once('appStore/getAppDetail', () => {
        appDetail = appStore.getState((s) => s.detail);
        resolve(appDetail);
      });
    } else {
      resolve(appDetail);
    }
  });

interface IBlob {
  content: string;
}

interface IState {
  blob: IBlob;
  sonarStatistics: QA.Stat;
  coverage: QA.Item[];
  duplications: QA.Item[];
}

const initState: IState = {
  blob: {} as IBlob,
  sonarStatistics: {} as QA.Stat,
  coverage: [],
  duplications: [],
};

const quality = createStore({
  name: 'codeQuality',
  state: initState,
  effects: {
    async getLatestSonarStatistics({ call, update, getParams }) {
      const params = getParams();
      const sonarStatistics = await call(getLatestSonarStatistics, { applicationId: params.appId });
      update({ sonarStatistics });
      return sonarStatistics;
    },
    async getSonarResults({ call, update }, payload: { key: string; type: string }) {
      const data = await call(getSonarResults, payload);
      switch (payload.type) {
        case 'coverage':
          update({ coverage: data });
          break;
        case 'duplications':
          update({ duplications: data });
          break;
        default:
          break;
      }
    },
    async getRepoBlob({ select, call, update }, payload: { path: string }) {
      const appDetail = (await getAppDetail()) as any;
      const { branch } = select((state) => state.sonarStatistics) as any;
      const blob = (await call(getFromRepo, {
        type: 'blob',
        repoPrefix: appDetail.gitRepoAbbrev,
        path: `/${branch}/${payload.path}`,
      })) as IBlob;
      update({ blob });
    },
  },
  reducers: {
    clearSonarResults(state) {
      return { ...state, coverage: [], duplications: [], sonarStatistics: {} };
    },
    clearRepoBlob(state) {
      return { ...state, blob: {} };
    },
  },
});

export default quality;
