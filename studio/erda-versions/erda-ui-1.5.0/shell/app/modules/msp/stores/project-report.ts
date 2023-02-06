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

import * as ProjectReportService from 'msp/services/project-report';
import i18n from 'i18n';
import { createStore } from 'core/cube';

interface IState {
  projectReportsPaging: IPaging;
  reportSeeting: PROJECT_REPORT.Config;
}

const initState: IState = {
  projectReportsPaging: {} as IPaging,
  reportSeeting: {} as PROJECT_REPORT.Config,
};
const projectReport = createStore({
  name: 'projectReport',
  state: initState,
  effects: {
    async getProjectReport(
      { call, getParams },
      payload: Omit<PROJECT_REPORT.QueryReport, keyof PROJECT_REPORT.QueryConfig>,
    ) {
      const { projectId, env } = getParams();
      const res = await call(
        ProjectReportService.getProjectReport,
        { projectId, workspace: env, ...payload },
        { paging: { key: 'projectReportsPaging' } },
      );
      return res.list;
    },

    async getProjectReportDetail(
      { call, getParams },
      payload: Omit<PROJECT_REPORT.GetReportDetail, keyof PROJECT_REPORT.QueryConfig>,
    ) {
      const { projectId, env } = getParams();
      const res = await call(ProjectReportService.getProjectReportDetail, { projectId, workspace: env, ...payload });
      return res;
    },

    async getProjectReportSetting({ call, update, getParams }) {
      const { projectId, env } = getParams();
      const reportSeeting = await call(ProjectReportService.getProjectReportSetting, { projectId, workspace: env });
      update({ reportSeeting });
    },

    async setProjectReportSetting(
      { call, getParams },
      payload: PROJECT_REPORT.DailySetting | PROJECT_REPORT.WeeklySetting,
    ) {
      const { projectId, env } = getParams();
      await call(
        ProjectReportService.setProjectReportSetting,
        { projectId, workspace: env, setting: payload },
        { successMsg: i18n.t('updated successfully') },
      );
      await projectReport.effects.getProjectReportSetting();
    },
  },
  reducers: {
    clearProjectReport(state) {
      state.projectReportsPaging = {} as IPaging;
    },
  },
});
export default projectReport;
