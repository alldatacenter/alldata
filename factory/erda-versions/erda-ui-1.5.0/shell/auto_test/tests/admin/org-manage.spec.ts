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

const orgData = {
  name: 'auto-ui-test-org',
  id: 'auto-ui-test-org',
  desc: 'auto ui test org',
};

Role('Admin', () => {
  test('org manage/org list', async ({ page, wait, expectExist }) => {
    // Go to https://erda.hkci.terminus.io/-/sysAdmin/orgs
    await page.goto('https://erda.hkci.terminus.io/-/sysAdmin/orgs');

    // Click [placeholder="filter"]
    await page.click('[placeholder="filter"]');
    // Fill [placeholder="filter"]
    await page.fill('[placeholder="filter"]', orgData.name);
    await wait(1);
    await expectExist('text=total 0 items', 1);

    // Click [placeholder="filter"]
    await page.click('[placeholder="filter"]');
    // Fill [placeholder="filter"]
    await page.fill('[placeholder="filter"]', '');
    await wait(1);

    // Click text=org personnel list
    await expectExist('text=org personnel list', 1);
    // Click text=org cluster list
    await expectExist('text=org cluster list', 1);

    // Click button:has-text("add org")
    await page.click('button:has-text("add org")');
    // Click id=displayName
    await page.click('#displayName');
    // Fill id=displayName
    await page.fill('#displayName', orgData.name);
    // Click input[type="textarea"]
    await page.click('input[type="textarea"]');
    // Fill input[type="textarea"]
    await page.fill('input[type="textarea"]', orgData.desc);
    // Click input[role="combobox"]
    await page.click('input[role="combobox"]');
    // Fill input[role="combobox"]
    await page.fill('input[role="combobox"]', 'erda');
    // Click div[role="document"] >> text=erda 前端
    await page.click('div[role="document"] >> text=erda 前端');
    await wait(1);
    // Click button:has-text("ok")
    await page.click('button:has-text("ok")');
    await wait(1);

    // Click [placeholder="filter"]
    await page.click('[placeholder="filter"]');
    // Fill [placeholder="filter"]
    await page.fill('[placeholder="filter"]', orgData.name);
    await expectExist('text=total 0 items', 0);

    await page.close();
  });
});
