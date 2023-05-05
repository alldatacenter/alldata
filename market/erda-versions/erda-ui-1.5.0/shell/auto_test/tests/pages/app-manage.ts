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

import Base from './base';
import { expect } from '@playwright/test';

interface IFormData {
  name: string;
  type: string;
  desc?: string;
  repository: boolean;
  template: string;
  repoConfig_url?: string;
  repoConfig_username?: string;
  repoConfig_password?: string;
  repoConfig_desc?: string;
}

export default class AppManage extends Base {
  async createApp(formData: IFormData) {
    const { type, repository, ...rest } = formData;
    await this.fillData('name', rest);
    await this.fillData('desc', rest);
    await this.clickImg(type);
    if (type === 'mobile app') {
      await this.page.click('text=none');
      await this.page.click(`:nth-match(:text("${formData.template}"), 2)`);
    }
    if (formData.repository) {
      await this.page.click('text="System built-in Git repository"');
      await this.page.click('text="external general Git repository"');
      await this.fillData('repoConfig_url', formData);
      await this.fillData('repoConfig_username', formData);
      await this.fillData('repoConfig_password', formData);
      await this.fillData('repoConfig_desc', formData);
    }
    await this.clickButton('save');
    await this.page.waitForEvent('requestfinished');
  }

  async editApp() {
    await this.clickButton('edit');
    await this.clickLabel('public application');
    this.clickButton('ok');
    await this.page.waitForEvent('requestfinished');
    expect((await this.page.$$('text=public application')).length).toBe(1);
  }

  async deleteApp(appName: string) {
    await this.clickButton('delete current application');
    await this.page.click('[placeholder="please enter app name"]');
    await this.page.fill('[placeholder="please enter app name"]', appName);
    this.clickButton('ok');
    await this.page.waitForEvent('requestfinished');
    await this.page.waitForNavigation();
    expect((await this.page.$$(`text=${appName}`)).length).toBe(0);
  }
}
