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
const title = 'terminus' + Date.now();
const modifyTitle = 'modified' + Date.now();
const testData = {
  title,
  modifyTitle,
  memberName: '吴辉洛',
  manager: '组织管理员',
  image: 'app/images/Erda.png',
  memberBoss: 'erda-ui-team',
};

Role('Manager', () => {
  test('Network blocking, notification group, audit log related', async ({ page, expectExist, goTo }) => {
    goTo('organizationInfo');
    // block network
    await page.click('li:has-text("block network")');
    expect(page.url()).toMatch(/\/orgCenter\/setting\/detail\?tabKey=block-network/);
    await page.click('text=prod environmentoff >> button[role="switch"]');
    await page.click('button:has-text("OK")');
    await expectExist('text=on');
    await page.click('button[role="switch"]:has-text("on")');
    await page.click('button:has-text("OK")');
    await expectExist('text=off');
    // audit log
    await page.click('#main >> text=audit log');
    expect(page.url()).toMatch(/\/orgCenter\/setting\/detail\?tabKey=operation%20log/);
    await page.click('[aria-label="Increase Value"]');
    await page.click('button:has-text("update")');
    expectExist('text=updated successfully', 0);
    // notification group
    await page.click('li:has-text("notification group")');
    expect(page.url()).toMatch(/\/orgCenter\/setting\/detail\?tabKey=notifyGroup/);
    await page.click('button:has-text("new notification group")');
    await page.click('input[type="text"]');
    await page.fill('input[type="text"]', testData.title);
    await page.click('text=group namenotified to >> :nth-match(span, 2)');
    await page.click('div[role="document"] >> text=member');
    await page.click('.ant-dropdown-trigger');
    await page.click('[placeholder="search by keywords"]');
    await page.fill('[placeholder="search by keywords"]', testData.memberName);
    await page.click(`text=${testData.memberName}(${testData.manager})`);
    await page.click('text=new Group');
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.memberName}`);
  });
});
