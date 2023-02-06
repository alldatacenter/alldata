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

Role('Admin', () => {
  test('global config page/user configuration', async ({ page, wait, expectExist }) => {
    // Go to https://erda.hkci.terminus.io/-/sysAdmin/orgs
    await page.goto('https://erda.hkci.terminus.io/-/sysAdmin/orgs');

    // Click text=Global config
    await page.click('text=Global config');
    await wait(1);

    await expectExist('text=user configuration');
    await expectExist('text=notify item configuration');

    const [monthValue] = await page.$$eval('text=months', (elHandles: HTMLElement[]) =>
      elHandles.map((el) => parseInt(el.textContent, 10)),
    );
    const times = await page.$$eval('text=times', (elHandles: HTMLElement[]) =>
      elHandles.map((el) => parseInt(el.textContent, 10)),
    );
    const [first, second, tip, third] = times;

    const editForm = async ([one, two, three, four]: string[]) => {
      // Click button:has-text("edit")
      await page.click('button:has-text("edit")');

      // Click [aria-label="Close"]
      await page.click('[aria-label="Close"]');

      // Click button:has-text("edit")
      await page.click('button:has-text("edit")');

      // Click input[type="text"]
      await page.click('input[type="text"]');

      // Fill input[type="text"]
      await page.fill('input[type="text"]', one);

      // Click text=enter password error count - verification prompttimes >> input[type="text"]
      await page.click('text=enter password error count - verification prompttimes >> input[type="text"]');

      // Fill text=enter password error count - verification prompttimes >> input[type="text"]
      await page.fill('text=enter password error count - verification prompttimes >> input[type="text"]', two);

      // Click text=enter password error count - freeze accounttimes >> input[type="text"]
      await page.click('text=enter password error count - freeze accounttimes >> input[type="text"]');

      // Fill text=enter password error count - freeze accounttimes >> input[type="text"]
      await page.fill('text=enter password error count - freeze accounttimes >> input[type="text"]', three);

      // Click text=enter the password error maximum number of times - within 24 hourstimes >> input[type="text"]
      await page.click(
        'text=enter the password error maximum number of times - within 24 hourstimes >> input[type="text"]',
      );

      // Fill text=enter the password error maximum number of times - within 24 hourstimes >> input[type="text"]
      await page.fill(
        'text=enter the password error maximum number of times - within 24 hourstimes >> input[type="text"]',
        four,
      );

      // Click button:has-text("cancel")
      await page.click('button:has-text("ok")');
    };

    await editForm([String(monthValue + 1), String(first + 1), String(second + 1), String(third + 1)]);

    await wait(1);

    await editForm([String(monthValue), String(first), String(second), String(third)]);

    await wait(1);

    await expectExist(`text=${monthValue} months`, 1);
    await expectExist(`text=${first} times`);
    await expectExist(`text=${second} times`);
    await expectExist(`text=${third} times`);

    await page.close();
  });
});
