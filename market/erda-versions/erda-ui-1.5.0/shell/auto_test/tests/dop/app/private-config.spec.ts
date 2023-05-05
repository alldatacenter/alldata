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

import { Role, test } from '../../../fixtures';
import { expect } from '@playwright/test';
import VariableConfig from '../../pages/variable-config';

Role('Manager', () => {
  test('app config text value', async ({ page, wait, expectExist, goTo }) => {
    const config = new VariableConfig(page);
    await goTo('appPipelineParameter');
    await page.click('text=default config');
    await page.waitForSelector('button:has-text("add variable")');
    const data = {
      type: 'value',
      key: `name-${Date.now()}`,
      value: `value-${Date.now()}`,
      encrypt: false,
    };
    await config.addVariable(data);
    await wait(1);
    await expectExist(`td:has-text("${data.key}")`, 1);
    await expectExist(`td:has-text("${data.value}")`, 1);
    await config.editVariable(data.key, { ...data, value: 'new value' });
    await wait(1);
    await expectExist(`tr:has-text("${data.key}") >> text=new value`, 1);
    await config.editVariable(data.key, { ...data, value: 'new value', encrypt: true });
    await wait(1);
    await expectExist(`tr:has-text("${data.key}") >> text=******`, 1);
    await config.deleteVariable(data.key);
    await wait(1);
    await expectExist(`td:has-text("${data.key}")`, 0);
    await page.close();
  });
  test('app config file value', async ({ page, wait, expectExist, goTo }) => {
    const config = new VariableConfig(page);
    await goTo('appPipelineParameter');
    await page.click('text=default config');
    await page.waitForSelector('button:has-text("add variable")');
    const data = {
      type: 'file',
      key: `name-${Date.now()}`,
      file: 'Erda.png',
      encrypt: false,
    };
    await config.addVariable(data);
    await wait(1);
    await expectExist(`td:has-text("${data.key}")`, 1);
    await config.downVariable(data.key);
    await config.editVariable(data.key, {
      ...data,
      encrypt: true,
    });
    await wait(1);
    const cls = await page.$eval(`tr:has-text("${data.key}") >> text=download`, (el) => el.className);
    expect(cls).toContain('disabled');
    await config.deleteVariable(data.key);
    await wait(1);
    await expectExist(`td:has-text("${data.key}")`, 0);
    await page.close();
  });
});
