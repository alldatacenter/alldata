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

declare namespace TEST {
  interface Statuses {
    failed: number;
    error: number;
    passed: number;
    skipped: number;
  }

  interface RunTestItem {
    id: number;
    createdAt: string;
    updatedAt: string;
    applicationId: number;
    projectId: number;
    buildId: number;
    name: string;
    uuid: string;
    applicationName: string;
    output: string;
    desc: string;
    operatorId: string;
    operatorName: string;
    commitId: string;
    branch: string;
    gitRepo: string;
    caseDir: string;
    application: string;
    type: string;
    totals: {
      tests: number;
      duration: number;
      statuses: Statuses;
    };
    parserType: string;
    envs: null;
    workspace: string;
    suites: null;
  }
}
