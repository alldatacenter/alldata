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
  test('sonar-test', async ({ page, wait, expectExist, goTo }) => {
    await goTo('qualityReport');
    await expectExist('text=recent', 0);
    await expectExist('text=detection branch', 0);
    await expectExist('text=submit ID', 0);
    await expectExist('text=code defect6D', 0);
    await expectExist('text=code vulnerability0A', 0);
    await expectExist('text=code smell22A', 0);
    await page.click('text=3dacc1');
    expect(page.url()).toMatch(/\/dop\/projects\/[1-9]\d*\/apps\/[1-9]\d*\/repo\/commit\/[A-Za-z0-9]+$/);
    await wait(1);
    await goTo('qualityReport');
    await page.click('text=coverage');
    await page.click('text=market.tsx');
    await wait(1);
    await page.click('text=repeat rate');
    await expectExist('#rc-tabs-6-panel-duplications >> text=file name', 0);
    await expectExist('#rc-tabs-6-panel-duplications >> text=file path', 0);
    await expectExist('th:has-text("repeat rate")', 1);
    await expectExist('text=repeat number of lines', 1);
    await page.click('text=src/pages/join/config.tsx');
    await wait(1);
    await page.close();
  });
});
