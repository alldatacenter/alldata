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

Role('Manager', () => {
  test('unit-test list', async ({ page, wait, expectExist, goTo }) => {
    await goTo('testDetail');
    await expectExist('text=name', 0);
    await expectExist('th:has-text("branch")', 0);
    await expectExist('text=creator', 0);
    await expectExist('text=create time', 0);
    await expectExist('text=type', 0);
    await expectExist('text=time consuming', 0);
    await expectExist('text=execute result', 0);
    // Click text=ut-13a193
    await page.click('text=ut-be5d95');
    expect(page.url()).toMatch(/\/dop\/projects\/[1-9]\d*\/apps\/[1-9]\d*\/test\/[1-9]\d*/);
    await wait(1);
    await expectExist('text=test detail', 1);
    await page.close();
  });
});
