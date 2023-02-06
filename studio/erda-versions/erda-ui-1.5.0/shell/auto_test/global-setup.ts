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

import { chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import login, { RoleTypes } from './login.spec';
import config from './auth/config';
// const { chromium } = require('playwright');

async function globalSetup() {
  // const userDataDir = './auto_test/auth';
  // const context = await chromium.launchPersistentContext(userDataDir, { headless: false });
  const browser = await chromium.launch();

  const roles = Object.keys(config.roles) as RoleTypes[];
  const authFiles = fs.readdirSync(path.resolve(__dirname, './auth'));

  const promiseList: Promise<any>[] = [];
  roles.forEach((role) => {
    if (role && !authFiles.includes(`${role}.json`)) {
      promiseList.push(login({ browser, role }));
    }
  });

  await Promise.all(promiseList);
}
export default globalSetup;
