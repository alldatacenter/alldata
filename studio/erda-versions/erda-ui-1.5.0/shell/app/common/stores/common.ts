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
import { uploadFile, fetchLog, getRenderPageLayout } from '../services';
import { has } from 'lodash';

interface State {
  logsMap: Obj<COMMON.LOG>;
  slidePanelComps: COMMON.SlideComp[];
}

const initState: State = {
  logsMap: {},
  slidePanelComps: [],
};

const defaultLogPeriod = 3000;

const common = createStore({
  name: 'common',
  state: initState,
  effects: {
    async uploadFile({ call }, file: FormData) {
      return call(uploadFile, file);
    },
    async getRenderPageLayout({ call }, payload: CONFIG_PAGE.RenderConfig) {
      const res = await call(getRenderPageLayout, payload);
      return res;
    },
    async fetchLog({ call }, { logKey, ...query }: { [k: string]: any; logKey: string }) {
      let lines = [] as COMMON.LogItem[];
      if (has(query, 'fetchApi') && !query.fetchApi) {
        return;
      }
      try {
        const response = await call(fetchLog, query);
        lines = response.lines;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
        lines = [];
      }
      const meta = { logKey, ...query };
      if (Array.isArray(lines)) {
        common.reducers.queryLogSuccess(lines, meta);
      }
    },
  },
  reducers: {
    queryLogSuccess(state, lines: COMMON.LogItem[], meta: { logKey: string; count?: number }) {
      const { logKey, count = 0 } = meta;
      const { logsMap } = state;
      const oldLog = logsMap[logKey] || {};
      let { emptyTimes = 0, fetchPeriod = defaultLogPeriod } = oldLog;
      const oldLines = oldLog.content || [];
      let newLines: COMMON.LogItem[] = [];
      if (lines.length) {
        emptyTimes = 0;
        fetchPeriod = defaultLogPeriod;
        newLines = count > 0 ? oldLines.concat(lines) : lines.concat(oldLines);
      } else {
        newLines = oldLines;
        emptyTimes += 1;
      }
      const LIMIT = 5000;
      if (newLines.length > LIMIT) {
        newLines = count > 0 ? newLines.slice(-LIMIT) : newLines.slice(0, LIMIT);
      }
      // 空的次数大于2次后，开始延长拉取周期，最长1分钟
      if (emptyTimes > 2 && fetchPeriod < 60 * 1000) {
        fetchPeriod += defaultLogPeriod;
      }
      state.logsMap[logKey] = { content: newLines, emptyTimes, fetchPeriod };
    },
    clearLog(state, logKey?: string) {
      if (logKey) {
        state.logsMap[logKey] = { content: [], emptyTimes: 0, fetchPeriod: defaultLogPeriod };
      } else {
        state.logsMap = {};
      }
    },
    pushSlideComp(state, payload: COMMON.SlideComp) {
      state.slidePanelComps.push(payload);
    },
    popSlideComp(state) {
      state.slidePanelComps.pop();
    },
    clearSlideComp(state) {
      state.slidePanelComps = [];
    },
  },
});

export default common;
