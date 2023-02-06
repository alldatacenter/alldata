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

Role('Manager', () => {
  test('have full platforms entry', async ({ page, expectExist }) => {
    // Go to https://erda.hkci.terminus.io/integration
    await page.goto('https://erda.hkci.terminus.io/integration');

    // Click [aria-label="icon: appstore"]
    await page.click('[name="appstore"]');

    await expectExist('a:has-text("DevOps platform")', 1);
    await expectExist('a:has-text("Microservice platform")', 1);
    await expectExist('a:has-text("cloud management")', 1);
    await expectExist('a:has-text("edge computing")', 1);
    await expectExist('a:has-text("Fast data")', 1);
    await expectExist('a:has-text("OrgCenter")', 1);
  });
});

Role('Dev', () => {
  test('have part platforms entry', async ({ page, expectExist }) => {
    // Go to https://erda.hkci.terminus.io/integration
    await page.goto('https://erda.hkci.terminus.io/integration');

    // Click [aria-label="icon: appstore"]
    await page.click('[aria-label="icon: appstore"]');

    await expectExist('a:has-text("DevOps platform")', 1);
    await expectExist('a:has-text("Microservice platform")', 1);
    await expectExist('a:has-text("cloud management")', 0);
    await expectExist('a:has-text("edge computing")', 0);
    await expectExist('a:has-text("Fast data")', 0);
    await expectExist('a:has-text("OrgCenter")', 0);
  });
});
