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

import { Role, test } from '../../../fixtures';
import { expect } from '@playwright/test';
import AppManage from '../../pages/app-manage';

const formData = {
  type: 'business app',
  name: `auto-business-app-${Date.now()}`,
  desc: `auto-app-description-${Date.now()}`,
  repository: false,
  template: 'none',
};

Role('Manager', () => {
  test.describe('create app', () => {
    test.beforeEach(async ({ page, goTo }) => {
      await goTo('createApp');
    });
    test.afterEach(async ({ page }) => {
      await page.close();
    });
    test('business app manage', async ({ page }) => {
      const app = new AppManage(page);
      await app.createApp(formData);
      await page.click(`text=${formData.name}`);
      await page.waitForNavigation();
      expect(page.url()).toMatch(/\d+\/repo$/);
      await page.click('text=Settings');
      await app.editApp();
      await app.deleteApp(formData.name);
    });
    test('mobile app manage', async ({ page }) => {
      const app = new AppManage(page);
      const name = `auto-mobile-app-${Date.now()}`;
      await app.createApp({
        ...formData,
        name,
        type: 'mobile app',
      });
      await page.click(`text=${name}`);
      await page.waitForNavigation();
      expect(page.url()).toMatch(/\d+\/repo$/);
      await page.click('text=Settings');
      await app.editApp();
      await app.deleteApp(name);
    });
    test('project level app manage', async ({ page }) => {
      const app = new AppManage(page);
      const name = `auto-project-level-app-${Date.now()}`;
      await app.createApp({
        ...formData,
        name,
        type: 'project level app',
      });
      await page.click(`text=${name}`);
      await page.waitForNavigation();
      expect(page.url()).toMatch(/\d+\/repo$/);
      await page.click('text=Settings');
      await app.editApp();
      await app.deleteApp(name);
    });
    test('library/module app manage', async ({ page }) => {
      const app = new AppManage(page);
      const name = `auto-library-module-app-${Date.now()}`;
      await app.createApp({
        ...formData,
        name,
        type: 'library/module',
      });
      await page.click(`text=${name}`);
      await page.waitForNavigation();
      expect(page.url()).toMatch(/\d+\/repo$/);
      await page.click('text=Settings');
      await app.editApp();
      await app.deleteApp(name);
    });
  });
});
