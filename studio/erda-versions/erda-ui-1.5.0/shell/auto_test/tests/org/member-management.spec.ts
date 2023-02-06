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
  test('Organization member related', async ({ page, expectExist, goTo }) => {
    await goTo('organizationInfo');
    await page.click('text=org member');
    expect(page.url()).toMatch(/\/orgCenter\/setting\/detail\?tabKey=orgMember/);
    await page.click('button:has-text("add member")');
    await page.click('.ant-select.w-full .ant-select-selector .ant-select-selection-overflow');
    await page.fill('text=member Please enter a member name to search >> input[role="combobox"]', testData.memberName);
    await page.click('.ant-select-item-option-content');
    await page.click('text=select member labelOutsourcePartneroutsourcepartner >> div');
    await page.click(':nth-match(:text("outsource"), 2)');
    await page.click('text=labeloutsource OutsourcePartneroutsourcepartner >> div');
    await page.click(`div[role="document"] >> text=${testData.manager}`);
    await page.click('div:nth-child(2) .ant-modal .ant-modal-content .ant-modal-body');
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.memberName}`, 2);
    await page.click('button:has-text("invite")');
    await page.click('text=url address');
    await page.click('text=verification code');
    await page.click('button:has-text("copy")');
    await page.click('[aria-label="Close"]');
    await page.click('[placeholder="search by nickname, username, email or mobile number"]');
    await page.fill('[placeholder="search by nickname, username, email or mobile number"]', testData.memberName);
    await page.click('input[role="combobox"]');
    await page.click(`text=${testData.manager}`);
    await expectExist(`text=${testData.memberName}`, 2);
    await expectExist('text=edit', 2);
    await expectExist('text=exit', 0);
    await page.fill('[placeholder="search by nickname, username, email or mobile number"]', testData.memberName);
    await expectExist('text=edit', 2);
    await expectExist('text=remove');
    await page.click('text=remove');
    await page.click('text=OK');
    await expectExist('text=no data', 0);
  });
});
