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

import autoTestStore from './stores/auto-test-case';
import branchStore from './stores/branch-rule';
import customStore from './stores/custom-addon';
import issueWorkflowStore from './stores/issue-workflow';
import issuesStore from './stores/issues';
import iterationStore from './stores/iteration';
import labelStore from './stores/label';
import projectStore from './stores/project';
import resourceStore from './stores/resource';
import scanRuleStore from './stores/scan-rule';
import testCaseStore from './stores/test-case';
import testEnvStore from './stores/test-env';
import testSetStore from './stores/test-set';
import testPlanStore from './stores/test-plan';

export default (registerModule) => {
  return registerModule({
    key: 'project',
    stores: [
      autoTestStore,
      branchStore,
      customStore,
      issueWorkflowStore,
      issuesStore,
      iterationStore,
      labelStore,
      projectStore,
      resourceStore,
      scanRuleStore,
      testCaseStore,
      testEnvStore,
      testSetStore,
      testPlanStore,
    ],
  });
};
