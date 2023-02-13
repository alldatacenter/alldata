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
import { expect } from '@playwright/test';

const branchName = 'master';
const commit = 'feat';

Role('Manager', () => {
  test('commit history', async ({ page, wait, expectExist, goTo }) => {
    // Go to https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/repo/commits/master
    await goTo('commitHistory');

    // switch to pipeline execute detail, execute pipeline
    await page.click('.repo-branch-select');
    await wait(1);

    await page.click('[placeholder="enter branch or tag name to filter"]');
    await page.fill('[placeholder="enter branch or tag name to filter"]', branchName);
    await wait(1);

    await page.click('.branch-item >> nth=0');
    await wait(1);

    await page.click('[placeholder="filter by committed message"]');
    await page.fill('[placeholder="filter by committed message"]', commit);
    await page.press('[placeholder="filter by committed message"]', 'Enter');
    await wait(1);

    await page.click('.commit-title >> nth=0');
    expect(page.url()).toMatch(/repo\/commit/);

    await page.click('.cursor-copy >> nth=0');
    await wait(2);
    await expectExist('.ant-message-success', 1);

    await page.click('.commit-right >> nth=0');
    expect(page.url()).toMatch(/repo\/tree/);
    await wait(1);

    await page.close();
  });
});
