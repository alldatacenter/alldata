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

import base, { expect } from '@playwright/test';
import fs from 'fs';
import { RoleTypes } from './login.spec';
import authConfig from '../auto_test/auth/config';

interface TestFixtures {
  version: string;
  wait: (seconds?: number) => void;
  expectExist: (seconds: string, count?: number) => boolean;
  expectRequestSuccess: () => () => void;
  logFailedRequest: () => void;
  goTo: (key: keyof typeof gotoMap) => void;
}

const gotoMap = {
  root: '',
  deploy: '/erda/dop/projects/123/apps/788/deploy',
  appList: '/erda/dop/projects/568/apps',
  createApp: `/erda/dop/projects/568/apps/createApp`,
  appSetting: '/erda/dop/projects/568/apps/840/setting',
  appBranchRule: '/erda/dop/projects/568/apps/840/setting?tabKey=branchRule',
  appDeployParameter: '/erda/dop/projects/568/apps/840/setting?tabKey=appConfig',
  appPipelineParameter: '/erda/dop/projects/568/apps/840/setting?tabKey=privateConfig',
  appNotifyConfig: '/erda/dop/projects/568/apps/840/setting?tabKey=notifyConfig',
  appNotifyGroup: '/erda/dop/projects/568/apps/840/setting?tabKey=notifyGroup',
  orgProjectList: '/erda/orgCenter/projects',
  orgCreateProject: '/erda/orgCenter/projects/createProject',
  pipeline: '/erda/dop/projects/123/apps/788/pipeline',
  branches: '/erda/dop/projects/123/apps/788/repo/branches',
  mergeRequest: '/erda/dop/projects/123/apps/788/repo/mr/open',
  branchDevelop: '/erda/dop/projects/123/apps/788/repo/tree/develop',
  qualityReport: '/erda/dop/projects/1/apps/16/test/quality',
  testDetail: '/erda/dop/projects/1/apps/16/test',
  organizationInfo: '/erda/orgCenter/setting/detail?tabKey=orgInfo',
  projectManagement: '/erda/orgCenter/projects',
};

// Extend base test with our fixtures.
const test = base.extend<TestFixtures>({
  // This fixture is a constant, so we can just provide the value.
  version: '1.0', // provide different value by project.use config

  wait: async ({ page }, use) => {
    await use(async (seconds) => {
      if (seconds !== undefined) {
        await Promise.all([
          await page.waitForLoadState('networkidle'),
          new Promise((re) => setTimeout(re, seconds * 1000)),
        ]);
      } else {
        await page.waitForLoadState('networkidle');
      }
    });
  },

  expectRequestSuccess: async ({ page }, use) => {
    await use(async () => {
      page.on('response', (response) => {
        const firstNumber = String(response.status()).slice(0, 1);
        if (response.url().startsWith('/api')) {
          expect(firstNumber).toBe('2');
        }
      });
    });
  },

  expectExist: async ({ page }, use) => {
    // Use the fixture value in the test.
    await use(async (selector, count) => {
      const total = (await page.$$(selector)).length;
      return count === undefined ? expect(total).toBeGreaterThan(0) : expect(total).toBe(count);
    });

    // Clean up the fixture. Nothing to cleanup in this example.
  },

  logFailedRequest: [
    async ({ page }, use, testInfo) => {
      const logs = [];
      page.on('response', async (response) => {
        const firstNumber = String(response.status()).slice(0, 1);
        if (response.url().includes('/api/')) {
          const content = await response.body();
          if (firstNumber !== '2') {
            logs.push(`[${response.status()}] ${response.url()}`, content, '\n');
          }
        }
      });
      await use(() => {});

      if (logs.length) {
        fs.writeFileSync(testInfo.outputPath('logs.txt'), logs.join('\n'), 'utf8');
      }
    },
    { auto: true },
  ], // pass "auto" to starts fixture automatically for every test.

  goTo: async ({ page }, use) => {
    await use(async (key: keyof typeof gotoMap) => {
      await page.goto(`${gotoMap[key]}`);
    });
  },
});

const Role = (role: RoleTypes, fn: () => void) => {
  test.describe(`[${role}]`, () => {
    test.use({
      baseURL: authConfig.url,
      storageState: `auto_test/auth/${role}.json`,
    });

    fn();
  });
};

export { test, expect, Role };
