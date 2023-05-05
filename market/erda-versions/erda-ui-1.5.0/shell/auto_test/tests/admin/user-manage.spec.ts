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

import { UserManagePage } from '../pages/user-manage';
import { Role, test } from '../../fixtures';

const name = 'auto_' + Date.now();
const formData = {
  name,
  password: 'auto_test_user',
  nick: name,
  phone: '',
  email: `${name}@erda.cloud`,
};

Role('Admin', () => {
  test('user manage', async ({ page, wait, expectExist, expectRequestSuccess }) => {
    await expectRequestSuccess();

    // Go to https://erda.hkci.terminus.io/-/sysAdmin/orgs
    await page.goto('https://erda.hkci.terminus.io/-/sysAdmin/user-manage');

    // Click [placeholder="user name"]
    await page.click('[placeholder="user name"]');
    // Fill [placeholder="user name"]
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/-/sysAdmin/user-manage?name=auto_123&pageNo=1' }*/),
      page.fill('[placeholder="user name"]', formData.name),
    ]);

    await wait(1);
    expectExist('text=no data', 1);

    const userManagePage = new UserManagePage(page);

    await userManagePage.createUser(formData);
    await wait(2);

    await expectExist(`text=${formData.email}`, 1);

    await userManagePage.filterUser({
      name: 'should not exist',
    });
    await wait(1);
    await expectExist('text=no data', 1);

    await userManagePage.filterUser({
      phone: formData.phone,
      nick: formData.nick,
    });
    await wait(1);
    await expectExist(`text=${formData.email}`, 1);

    await userManagePage.filterUser({
      nick: formData.nick,
    });
    await wait(1);
    await expectExist(`text=${formData.email}`, 1);

    await userManagePage.filterUser({
      email: formData.email,
    });
    await wait(1);
    await expectExist(`text=${formData.email}`, 1);
    await page.close();
  });
});
