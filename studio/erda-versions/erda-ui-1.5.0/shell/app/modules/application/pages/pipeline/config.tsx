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

import i18n from 'i18n';
import { isEmpty, get, compact } from 'lodash';

export const SCOPE_AUTOTEST = 'project-autotest-testcase';
export const SCOPE_AUTOTEST_PLAN = 'project-autotest-testplan';
export const SCOPE_PROJECT = 'project';
export const SCOPE_CONFIG_SHEET = 'project-autotest-configsheet';
export const SCOPE_APP_PIPELINE = 'project-app';

export const scopeMap = {
  autoTest: {
    name: i18n.t('dop:test case'),
    scope: SCOPE_AUTOTEST,
    icon: 'test-case-secondary',
  },
  autoTestPlan: {
    name: i18n.t('dop:test plan'),
    scope: SCOPE_AUTOTEST_PLAN,
    icon: 'imagevector',
  },
  projectPipeline: {
    name: i18n.t('pipeline'),
    scope: SCOPE_PROJECT,
    icon: 'liushuixianmoban',
  },
  appPipeline: {
    name: i18n.t('dop:application pipeline'),
    scope: SCOPE_APP_PIPELINE,
    icon: 'liushuixian2',
  },
  configSheet: {
    name: i18n.t('config sheet'),
    scope: SCOPE_CONFIG_SHEET,
    icon: 'tubiaozhizuomoban',
  },
};

// 根据inode的得到branch、path
export const getBranchPath = (node: TREE.NODE, appId?: string) => {
  if (!node || isEmpty(node)) return { branch: '', pagingYmlNames: [], env: '' };
  const gittarYmlPath = get(node, 'meta.snippetAction.snippet_config.labels.gittarYmlPath');
  const snippetConfigName = get(node, 'meta.snippetAction.snippet_config.name') || '';
  const ymlPathStrArr: string[] = compact((gittarYmlPath.replace(snippetConfigName, '') || '').split('/'));
  let pagingYmlNames = [] as string[];
  let branch = '';
  let env = '';
  if (ymlPathStrArr.length) {
    pagingYmlNames = [`${appId}/${gittarYmlPath.split('/').slice(1).join('/')}`, snippetConfigName];
    branch = ymlPathStrArr.slice(2).join('/');
    env = ymlPathStrArr[1];
  }
  const path = snippetConfigName.startsWith('/') ? snippetConfigName.replace('/', '') : snippetConfigName;
  return { branch, path, pagingYmlNames, env };
};
