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

// playwright-dev-page.ts
import { Page } from '@playwright/test';

export class UserManagePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async createUser(formData) {
    await this.page.click('button:has-text("create user")');

    // Click text=user namepasswordnickcellphoneemail >> input[type="text"]
    await this.page.click('text=user namepasswordnickcellphoneemail >> input[type="text"]');

    // Fill text=user namepasswordnickcellphoneemail >> input[type="text"]
    await this.page.fill('text=user namepasswordnickcellphoneemail >> input[type="text"]', formData.name);

    // Click input[type="password"]
    await this.page.click('input[type="password"]');

    // Fill input[type="password"]
    await this.page.fill('input[type="password"]', formData.password);

    // Click text=user namepasswordnickcellphoneemail >> #nick
    await this.page.click('text=user namepasswordnickcellphoneemail >> #nick');

    // Fill text=user namepasswordnickcellphoneemail >> #nick
    await this.page.fill('text=user namepasswordnickcellphoneemail >> #nick', formData.nick);

    // Press Enter
    await this.page.press('text=user namepasswordnickcellphoneemail >> #nick', 'Enter');

    if (formData.phone) {
      // Click text=user namepasswordnickcellphoneemail >> #phone
      await this.page.click('text=user namepasswordnickcellphoneemail >> #phone');

      // Fill text=user namepasswordnickcellphoneemail >> #phone
      await this.page.fill('text=user namepasswordnickcellphoneemail >> #phone', formData.phone);
    }

    // Click text=user namepasswordnickcellphoneemail >> #email
    await this.page.click('text=user namepasswordnickcellphoneemail >> #email');

    // Fill text=user namepasswordnickcellphoneemail >> #email
    await this.page.fill('text=user namepasswordnickcellphoneemail >> #email', formData.email);

    // Click button:has-text("ok")
    await this.page.click('button:has-text("ok")');
  }

  async filterUser(filterData) {
    await this.clearFilter();

    const list = Object.keys(filterData);
    const ps: Promise<any>[] = [];
    for (let i = 0, len = list.length; i < len; i++) {
      ps.push(this.page.fill(`#${list[i]}`, filterData[list[i]]));
    }
    await Promise.all(ps);
  }

  async clearFilter() {
    await this.page.$$eval('.anticon-close-circle:visible', (elHandles: HTMLElement[]) =>
      elHandles.forEach((el) => el.click()),
    );
  }
}
