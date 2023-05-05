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
import Notify from '../../pages/notify';
import { expect } from '@playwright/test';

Role('Manager', () => {
  const notifyGroupNames = [];
  test.describe('notify-group', async () => {
    test.beforeEach(async ({ goTo }) => {
      await goTo('appNotifyGroup');
    });
    test.afterEach(async ({ page }) => {
      await page.close();
    });
    test('notify-group-dding', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewGroupModal();
      const name = `dingTalk-address-${Date.now()}`;
      notifyGroupNames.push(name);
      await notify.fillGroupModal(
        {
          name,
          targetType: 'DingTalk address',
          targets_receiver: 'https://oapi.dingtalk.com/robot/send',
          targets_secret: 'secret',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      await notify.deleteRecord(name);
    });
    test('notify-group-member-role', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewGroupModal();
      const name = `member-role-${Date.now()}`;
      notifyGroupNames.push(name);
      await notify.fillGroupModal(
        {
          name,
          targetType: 'member role',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      await notify.deleteRecord(name);
    });
    test('notify-group-member', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewGroupModal();
      const name = `member-${Date.now()}`;
      notifyGroupNames.push(name);
      await notify.fillGroupModal(
        {
          name,
          targetType: 'member',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      await notify.deleteRecord(name);
    });
    test('notify-group-external-api', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewGroupModal();
      const name = `external-api-${Date.now()}`;
      notifyGroupNames.push(name);
      await notify.fillGroupModal(
        {
          name,
          targetType: 'external api',
          targets: '1',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      await notify.deleteRecord(name);
    });
    test('notify-group-external-user', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewGroupModal();
      const name = `external-user-${Date.now()}`;
      notifyGroupNames.push(name);
      await notify.fillGroupModal(
        {
          name,
          targetType: 'external user',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      await notify.deleteRecord(name);
    });
  });
  test.describe('notify-config', async () => {
    test.beforeEach(async ({ goTo }) => {
      await goTo('appNotifyConfig');
    });
    test.afterEach(async ({ page }) => {
      await page.close();
    });
    test('notification', async ({ page, wait, expectExist }) => {
      const notify = new Notify(page);
      await notify.openNewNotification();
      const name = `notification-${Date.now()}`;
      await notify.fillNotificationModal(
        {
          name,
          timing: ['Git Push'],
          notifyGroup: 'member',
        },
        true,
      );
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 1);
      const switchBtnSelector = `tr:has-text("${name}") >> button[role="switch"]`;
      const [switchBtn] = await page.$$(switchBtnSelector);
      const checkedAttr = await switchBtn.getAttribute('aria-checked');
      expect(checkedAttr).toBe('false');
      await page.click(switchBtnSelector);
      await wait(1);
      const [switchBtnAfterChange] = await page.$$(switchBtnSelector);
      expect(await switchBtnAfterChange.getAttribute('aria-checked')).toBe('true');
      await notify.clickTdOperation(name, 'edit');
      await notify.fillNotificationModal({
        name,
        timing: ['Pipeline Running', 'Create comment'],
        notifyGroup: 'externalApi',
      });
      await wait(1);
      await notify.deleteRecord(name);
      await wait(1);
      await expectExist(`td:has-text("${name}")`, 0);
    });
  });
});
