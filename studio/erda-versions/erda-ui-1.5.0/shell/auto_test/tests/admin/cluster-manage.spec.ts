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

Role('Admin', () => {
  test('cluster manage', async ({ page, wait, expectExist }, testInfo) => {
    // Go to https://erda.hkci.terminus.io/-/sysAdmin/orgs
    await page.goto('https://erda.hkci.terminus.io/-/sysAdmin/orgs');

    // Click text=Clusters
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/-/sysAdmin/cluster-manage?pageNo=1' }*/),
      page.click('text=Clusters'),
    ]);

    // Click text=erda-hongkong
    await page.click('text=erda-hongkong');

    // Click text=please choose organization name
    await page.click('text=please choose organization name');

    // Click text=integration
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/-/sysAdmin/cluster-manage?orgName=integration&pageNo=1' }*/),
      page.click('text=integration'),
    ]);

    // Click form span div div:has-text("integration")
    await page.click('form span div div:has-text("integration")');

    // Click text=keyifangshujiazuzhi
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/-/sysAdmin/cluster-manage?orgName=keyifangshujiazuzhi&pageNo=1' }*/),
      page.click('text=keyifangshujiazuzhi'),
    ]);

    // Click input[role="combobox"]
    await page.click('input[role="combobox"]');

    // Click button:has-text("operation history")
    await page.click('button:has-text("operation history")');
    expect(page.url()).toBe(
      'https://erda.hkci.terminus.io/-/sysAdmin/cluster-manage/history?recordType=upgradeEdgeCluster&scope=system',
    );

    await page.close();
  });
});
