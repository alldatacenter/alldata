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

import { PlaywrightTestConfig, selectors } from '@playwright/test';

// const { selectors, firefox } = require('playwright');  // Or 'chromium' or 'webkit'.

// selectors.register('notExist', () => ({
//   // Returns the first element matching given selector in the root's subtree.
//   query(root, selector) {
//     return root.querySelector(selector);
//   },

//   // Returns all elements matching given selector in the root's subtree.
//   queryAll(root, selector) {
//     return Array.from(root.querySelectorAll(selector));
//   }
// });

const config: PlaywrightTestConfig<{ version: string }> = {
  // Look for test files in the "tests" directory, relative to this configuration file
  testDir: 'tests',
  testMatch: '**/*.spec.ts',
  outputDir: 'results', // under tests
  preserveOutput: 'always',

  // Each test is given 30 seconds
  timeout: 30000,

  // Forbid test.only on CI
  forbidOnly: !!process.env.CI,

  // Two retries for each test
  retries: 2,

  // Limit the number of workers on CI, use default locally
  workers: process.env.CI ? 2 : undefined,

  globalSetup: './global-setup',

  use: {
    // Browser options
    headless: !!process.env.CI,
    slowMo: 50,

    locale: 'en-GB',

    // Context options
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,

    // Artifacts
    screenshot: 'on',
    video: 'retry-with-video',
  },

  projects: [
    {
      name: 'all',
      testMatch: '**/*.spec.ts',
      use: {
        // version: '1.1'
      },
    },
    {
      name: 'admin',
      testMatch: 'admin/*.spec.ts',
      use: {
        // version: '1.1'
      },
    },
    {
      name: 'dop',
      testMatch: 'dop/*.spec.ts',
      use: {},
    },
    {
      name: 'cmp',
      testMatch: 'cmp/*.spec.ts',
      use: {},
    },
  ],
};
export default config;
