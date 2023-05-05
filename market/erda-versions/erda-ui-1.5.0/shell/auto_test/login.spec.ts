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

import { Browser } from '@playwright/test';
import authConfig from './auth/config';

export type RoleTypes = keyof typeof authConfig.roles;
export default async ({ browser, role }: { browser: Browser; role: RoleTypes }) => {
  const page = await browser.newPage();

  // Go to https://erda.hkci.terminus.io/
  await page.goto(authConfig.url);

  // Click input[type="text"]
  await page.click('input[type="text"]');

  // Fill input[type="text"]
  await page.fill('input[type="text"]', authConfig.roles[role].userName);

  // Click input[type="password"]
  await page.click('input[type="password"]');

  // Fill input[type="password"]
  await page.fill('input[type="password"]', authConfig.roles[role].password);

  // Click button:has-text("立即登录")
  await Promise.all([page.waitForNavigation(), page.click('button:has-text("立即登录")')]);

  // Go to https://erda.hkci.terminus.io/
  await page.goto(authConfig.url);

  // Save storage state into the file.
  await page.context().storageState({ path: `auto_test/auth/${role}.json` });
  console.log('Login as:', authConfig.roles[role]);

  await page.close();
};
