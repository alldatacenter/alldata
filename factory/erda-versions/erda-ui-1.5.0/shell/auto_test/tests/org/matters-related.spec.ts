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
  test('Related to collaborative matters', async ({ page, expectExist, goTo }) => {
    goTo('organizationInfo');
    await page.click('text=issue custom fields');
    expect(page.url()).toMatch(/\/orgCenter\/setting\/detail\?tabKey=issueField/);
    await page.click('button:has-text("add")');
    await page.click('[placeholder="please enter field name"]');
    await page.fill('[placeholder="please enter field name"]', testData.title);
    await page.click('span:has-text("yes")');
    await page.click('input[role="combobox"]');
    await page.click(':nth-match(:text("Text"), 2)');
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.title}`, 0);
    await page.click(`text=${testData.title}yesTexteditdelete >> :nth-match(span, 2)`);
    await page.click('[placeholder="please enter field name"]');
    await page.fill('[placeholder="please enter field name"]', testData.modifyTitle);
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.modifyTitle}`, 0);
    await page.click('text=joint issue type');
    await page.click('text=requirement');
    await page.click('input[role="combobox"]');
    await page.click(':nth-match(:text("test"), 2)');
    await page.click('button:has-text("reference")');
    await page.click('text=testTextremovemove upmove down >> span');
    await page.click('button:has-text("OK")');
  });
});
