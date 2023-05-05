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
  test('Organization information related', async ({ page, expectExist, goTo }) => {
    const base = new Base(page);
    goTo('organizationInfo');
    await expectExist('text=organization identifier');
    await expectExist('text=org name');
    await expectExist('text=notice language');
    await expectExist('text=org description');
    await page.click('button:has-text("edit")');
    await expectExist('text=edit org info', 1);
    await expectExist('.ant-input-disabled', 1);
    await page.click('#displayName');
    await page.fill('#displayName', testData.title);
    await page.click('span:has-text("Chinese")');
    await page.click('text=English');
    await page.click('text=English');
    await page.click('div[role="document"] >> text=Chinese');
    await page.click('text=private org');
    await page.click('span:has-text("public org")');
    await page.click('.ant-upload');
    await base.uploadFile(testData.image, '[type="file"]');
    await page.click('input[type="textarea"]');
    await page.fill('input[type="textarea"]', testData.title);
    await page.click('button:has-text("ok")');
    await page.click('button:has-text("exit current org")');
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda' }*/),
      page.click('button:has-text("ok")'),
    ]);
    expect(page.url()).toMatch(/\/erda/);
    await page.close();
  });
});
