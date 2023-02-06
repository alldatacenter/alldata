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

import { Role, test } from '../../fixtures';
import { ProjectManage } from '../pages/project-manage';
import { expect } from '@playwright/test';

const mspProject = {
  type: 'MSP',
  displayName: `auto-msp-${Date.now()}`,
  desc: 'this is a msp project description',
};

const dopProject = {
  type: 'DOP',
  displayName: `auto-dop-${Date.now()}`,
  desc: 'this is a dop project description',
  cpu: '0',
  mem: '0',
};

Role('Manager', () => {
  test.describe('project manage', () => {
    test.beforeEach(async ({ page, goTo }) => {
      await goTo('orgCreateProject');
      await page.waitForEvent('requestfinished');
    });
    test.afterEach(async ({ page }) => {
      await page.close();
    });
    test('msp project', async ({ page, expectExist }) => {
      const projectManage = new ProjectManage(page);
      await projectManage.createProject(mspProject);
      const count = await projectManage.searchProject(mspProject.displayName);
      expect(count).toBe(1);
      await projectManage.jumpProject(mspProject.displayName);
      await expectExist('[aria-label="icon: link1"]', 0);
      mspProject.displayName = `edit-${mspProject.displayName}`;
      await projectManage.editProject(mspProject);
      await projectManage.deleteProject(mspProject.displayName);
      const countAfterDelete = await projectManage.searchProject(mspProject.displayName);
      expect(countAfterDelete).toBe(0);
    });
    test('dop project', async ({ page, expectExist, wait }) => {
      const projectManage = new ProjectManage(page);
      await page.click('text=need to configure project cluster resources');
      await expectExist('text=Configure cluster resources for different environments', 0);
      await page.click('text=need to configure project cluster resources');
      await expectExist('text=Configure cluster resources for different environments', 1);
      await projectManage.createProject(dopProject);
      const count = await projectManage.searchProject(dopProject.displayName);
      expect(count).toBe(1);
      await projectManage.jumpProject(dopProject.displayName);
      await page.waitForSelector('[aria-label="icon: link1"]');
      await expectExist('[aria-label="icon: link1"]', 1);
      await expectExist(`text=${dopProject.cpu} Core`, 1);
      await expectExist(`text=${dopProject.mem} GiB`, 1);
      dopProject.displayName = `edit-${dopProject.displayName}`;
      dopProject.cpu = '0.1';
      dopProject.mem = '0.1';
      await projectManage.editProject(dopProject);
      await wait(2);
      await expectExist(`text=${dopProject.cpu} Core`, 1);
      await expectExist(`text=${dopProject.mem} GiB`, 1);
      await expectExist('text=private project', 1);
      await projectManage.deleteProject(dopProject.displayName);
      const countAfterDelete = await projectManage.searchProject(dopProject.displayName);
      expect(countAfterDelete).toBe(0);
    });
  });
});
