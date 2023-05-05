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
  test('Audit-log', async ({ page, goTo, expectExist }) => {
    goTo('projectManagement');
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/orgCenter/safety?endAt=2021-10-28%2014%3A17%3A51&pageNo=1&startAt=2021-10-28%2013%3A17%3A51' }*/),
      page.click('text=Audit log'),
    ]);
    const [download] = await Promise.all([
      page.waitForEvent('popup'),
      page.waitForEvent('download'),
      page.click('button:has-text("export")'),
    ]);
    const path = await download.path();
    await expectExist(path);
    await download.close();
  });
});
