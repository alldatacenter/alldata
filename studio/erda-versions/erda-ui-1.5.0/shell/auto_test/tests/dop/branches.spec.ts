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
  test('branches list page', async ({ page, expectExist, wait, expectRequestSuccess, goTo }) => {
    await expectRequestSuccess();
    await goTo('branches');
    await wait(1);
    await page.click('.branch-item:has-text("develop") >> iconpark-icon[name="more"]');
    await wait(1);
    expectExist('text=set as default', 1);
    await page.click('text=set as default');
    await wait(1);
    expectExist('.branch-item:has-text("develop") >> .tag-primary >> text=default', 1);

    await page.click('.branch-item:has-text("hotfix/do-not-delete") >> button:has-text("compare")');
    await wait(1);
    expectExist('text=branch comparison', 1);
    expectExist('.repo-branch-select >> text=based on source:develop', 1);
    expectExist('.repo-branch-select >> text=compare:hotfix/do-not-delete', 1);

    expectExist('.commit-list', 1);
    await page.click('text=changed files');
    await wait(1);
    expectExist('.commit-summary', 1);
  });

  test('branches tags page', async ({ page, expectExist, wait, expectRequestSuccess, goTo }) => {
    await expectRequestSuccess();
    await goTo('branches');
    await wait(1);
    await page.click('text=tag');
    await wait(1);
    expect(page.url()).toContain('/repo/tags');
    await page.click('text=add label');

    await wait(1);

    expectExist('.ant-modal', 1);
    expectExist('.ant-radio-button-wrapper-checked >> text=Branch');

    await page.click('label >> text=commit SHA');
    expectExist('.ant-radio-button-wrapper-checked >> text=commit SHA');

    await page.click('label >> text=Branch');

    await page.click('input[type="search"]');
    await wait(1);
    await page.click('.ant-select-item >> text=develop');

    await page.click('input[id="tag"]');
    await page.fill('input[id="tag"]', '@');
    await wait(1);
    const now = Date.now();
    expectExist('text=must be composed of English, numbers, underscores, underscores, dots', 1);
    await page.fill('input[id="tag"]', `label1${now}`);
    await wait(1);
    expectExist('text=must be composed of English, numbers, underscores, underscores, dots', 0);

    await page.click('textarea');
    await page.fill('textarea', 'description');

    await page.click('button >> text=ok');
    await wait(1);

    expectExist(`a >> text=label1${now}`);

    await page.click(`.branch-item:has-text("label1${now}") >> button >> text=delete`);
    await wait(1);
    await expectExist('.ant-modal-confirm-title >> text=confirm deletion？', 1);
    await page.click('button >> text=yes');
    await wait(1);
    await expectExist(`a >> text=label1${now}`, 0);
  });
});

Role('Dev', () => {
  test('can not set default branch', async ({ page, expectExist, expectRequestSuccess, wait, goTo }) => {
    await expectRequestSuccess();
    await goTo('branches');
    await wait(1);
    await expectExist('text=develop', 1);
    await page.click('.branch-item:has-text("develop") >> iconpark-icon[name="more"]');
    await wait(1);
    await expectExist('.ant-dropdown-menu-item-disabled >> text=set as default', 1);
  });
});
