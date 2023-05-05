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

import { Role, test, expect } from '../../fixtures';
import Base from '../pages/base';
const title = 'terminus' + Date.now();
const modifyTitle = 'modify' + Date.now();
const testData = {
  image: 'app/images/Erda.png',
  mockNumber: '1',
};

Role('Manager', () => {
  test('management-center', async ({ page, expectExist, goTo }) => {
    const base = new Base(page);
    await goTo('projectManagement');
    // add project
    await page.click('button:has-text("add project")');
    expect(page.url()).toMatch(/\/orgCenter\/projects\/createProject/);
    await page.click('[placeholder="The name displayed on the Erda platform, supports Chinese naming"]');
    await page.fill('[placeholder="The name displayed on the Erda platform, supports Chinese naming"]', title);
    await page.click(
      '[placeholder="you can only use lowercase letters or numbers to begin and end. The hyphen can use -"]',
    );
    await page.fill(
      '[placeholder="you can only use lowercase letters or numbers to begin and end. The hyphen can use -"]',
      title,
    );
    await page.click('.ant-checkbox');
    await page.click('.ant-upload');
    await base.uploadFile(testData.image, '[type="file"]');
    await page.click('textarea');
    await page.fill('textarea', title);
    await page.click('button:has-text("save")');
    await expectExist(`text=${title}`);
    // edit project info
    await page.click(`text=${title}`);
    expect(page.url()).toMatch(/\/orgCenter\/projects\/\d+\/setting/);
    await page.click('button:has-text("edit")');
    await page.click('#displayName');
    await page.fill('#displayName', modifyTitle);
    await page.click('.ant-upload');
    await base.uploadFile(testData.image, '[type="file"]');
    await page.click('textarea');
    await page.fill('textarea', modifyTitle);
    await expectExist(`text=${modifyTitle}`, 1);
    await page.click('text=rollback setting');
    expect(page.url()).toBe(/\/orgCenter\/projects\/\d+\/setting\?tabKey=rollbackSetting/);
    await page.click('button:has-text("edit")');
    await page.click('input[role="spinbutton"]');
    await page.fill('input[role="spinbutton"]', testData.mockNumber);
    await page.click('#rollbackConfig_TEST');
    await page.fill('#rollbackConfig_TEST', testData.mockNumber);
    await page.click('#rollbackConfig_STAGING');
    await page.fill('#rollbackConfig_STAGING', testData.mockNumber);
    await page.click('#rollbackConfig_PROD');
    await page.fill('#rollbackConfig_PROD', testData.mockNumber);
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.mockNumber}`);
    // search project and enter project market
    await page.click('text=Projects');
    expect(page.url()).toMatch(/\/orgCenter\/projects/);
    await page.click('[placeholder="search by project name"]');
    await page.fill('[placeholder="search by project name"]', title);
    await expectExist(`text=${title}`);
    await page.click('.ant-table-row td:nth-child(6)');
    await expectExist('text=statistics');
  });
});
