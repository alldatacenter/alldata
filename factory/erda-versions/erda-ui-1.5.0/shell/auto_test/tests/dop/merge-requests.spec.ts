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

const createRequest = async (page, wait, expectExist, title) => {
  await page.click('text=new merge request');
  await wait(1);
  expect(page.url()).toContain('/open/createMR');

  await page.click('.repo-branch-select:has-text("based on:")');
  expectExist('div[role="tooltip"]', 1);
  await page.click('div[role="tooltip"] >> li:has-text("test-branch")');
  expectExist('text=based on:test-branch', 1);

  await page.click('.repo-branch-select:has-text("compare:")');
  expectExist('div[role="tooltip"]', 1);
  await page.click('div[role="tooltip"] >> li:has-text("hotfix/do-not-delete")');
  expectExist('text=compare:hotfix/do-not-delete', 1);

  await page.click('.repo-mr-form >> input[type="text"]');
  await page.fill('.repo-mr-form >> input[type="text"]', title);
  await page.click('.repo-mr-form >> textarea[name="textarea"]');
  await page.fill('.repo-mr-form >> textarea[name="textarea"]', 'description');
  await page.click('.repo-mr-form >> text=choose self');
  expectExist('.repo-mr-form >> img[alt="e [20x20]"]', 1);
  await page.click('button:has-text("submit")');
  await wait(1);

  expect(page.url()).toContain('/repo/mr/open');
  expectExist(`text=${title}`, 1);
};
const now = Date.now();
Role('Manager', () => {
  test.only('create branch', async ({ page, wait, expectRequestSuccess, goTo }) => {
    await expectRequestSuccess();
    await goTo('branchDevelop');
    await wait(1);
    await page.click('text=new branch');
    await wait(1);

    await page.click('input[type="text"]');
    await page.fill('input[type="text"]', 'hotfix/do-not-delete');
    await page.click('button >> text=ok');
    await wait(1);
  });
  test.only('close requests', async ({ page, expectExist, wait, expectRequestSuccess, goTo }) => {
    await expectRequestSuccess();

    await goTo('mergeRequest');
    await wait(1);

    await createRequest(page, wait, expectExist, `firstTitle${now}`);

    await page.click(`text=firstTitle${now}`);
    await wait(1);
    expectExist('.ant-tabs-tab-active:has-text("comment")', 1);
    await page.click('textarea[name="textarea"]');
    await page.fill('.markdown-editor >> textarea[name="textarea"]', 'test comment');
    await page.click('button:has-text("submit comment")');
    await wait(1);
    expectExist('article >> text=test comment', 1);

    await page.click('.ant-tabs-tab:has-text("commit")');
    await wait(1);
    expectExist('.commit-list', 1);
    await page.click('.ant-tabs-tab:has-text("changed files")');
    await wait(1);
    expectExist('text=single line', 1);
    expectExist('text=side-by-side', 1);

    await page.click('button:has-text("close")');
    await wait(1);
    expect(page.url()).toContain('/repo/mr/open');
    await page.click('.tab-menu-item >> text=closed');
    await wait(1);
    expect(page.url()).toContain('/mr/closed');
    expectExist(`text=firstTitle${now}`, 1);
    await page.click(`text=firstTitle${now}`);
    await wait(1);
    expectExist('text=merge request detail', 1);
  });
  test.only('merge', async ({ page, expectExist, wait, expectRequestSuccess, goTo }) => {
    await expectRequestSuccess();
    await goTo('mergeRequest');
    await wait(1);
    await createRequest(page, wait, expectExist, `secondTitle${now}`);
    await createRequest(page, wait, expectExist, `thirdTitle${now}`);

    expectExist(`text=thirdTitle${now}`, 1);
    await page.click(`text=thirdTitle${now}`);
    await page.click('button:has-text("merge")');
    await wait(1);
    await expectExist('.ant-modal', 1);
    await expectExist('input[type="checkbox"]', 1);
    await page.click('button >> text=ok');
    await wait(1);
    await page.click('.tab-menu-item >> text=merged');
    expectExist(`text=thirdTitle${now}`, 1);
  });
});

Role('Dev', () => {
  test.only('can not edit or merge request', async ({ page, expectExist, expectRequestSuccess, wait, goTo }) => {
    await expectRequestSuccess();
    await goTo('mergeRequest');
    await wait(1);
    await page.click(`:text-matches("secondTitle")`);
    await wait(1);
    expectExist('.disabled >> button:has-text("edit")', 1);
    expectExist('.disabled >> button:has-text("merge")', 1);
  });
});
