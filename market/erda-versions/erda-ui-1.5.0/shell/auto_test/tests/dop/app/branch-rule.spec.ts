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

import { expect, Page } from '@playwright/test';
import { Role, test } from '../../../fixtures';
import Base from '../../pages/base';

const formData = {
  rule: `feature/name-${Date.now()}`,
  continuous: false,
  protected: true,
  desc: `description-${Date.now()}`,
};

const formController = async (base: Base, data: typeof formData) => {
  await base.fillData('rule', data);
  await base.toggleSwitch(':nth-match(button[role="switch"], 1)', data.continuous);
  await base.toggleSwitch(':nth-match(button[role="switch"], 2)', data.protected);
  await base.fillData('desc', data);
  await base.clickButton('ok');
};

const assertText = async (page: Page, name: string, index: number, isOpen: boolean) => {
  const text = isOpen ? 'yes' : 'no';
  const inner = await page.innerText(`tr:has-text("${name}") >> :nth-match(td, ${index})`);
  expect(inner).toBe(text);
};

Role('Manager', () => {
  test('branch rules', async ({ page, wait, expectExist, goTo }) => {
    await goTo('appBranchRule');
    const base = new Base(page);
    await base.clickButton('new branch rule');
    await formController(base, formData);
    await wait(1);
    await expectExist(`td:has-text("${formData.rule}")`, 1);
    await assertText(page, formData.rule, 2, formData.continuous);
    await assertText(page, formData.rule, 3, formData.protected);
    await page.click(`tr:has-text("${formData.rule}") >> text=edit`);
    const editFormData = {
      rule: `edit-${formData.rule}`,
      continuous: true,
      protected: false,
      desc: `edit-${formData.desc}`,
    };
    await formController(base, editFormData);
    await wait(1);
    await expectExist(`td:has-text("${editFormData.rule}")`, 1);
    await assertText(page, editFormData.rule, 2, editFormData.continuous);
    await assertText(page, editFormData.rule, 3, editFormData.protected);
    await page.click(`tr:has-text("${formData.rule}") >> text=delete`);
    await base.clickButton('OK');
    await wait(1);
    await expectExist(`td:has-text("${editFormData.rule}")`, 0);
  });
});
