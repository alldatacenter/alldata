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

import { Page } from '@playwright/test';

/**
 * @description shell 模块路径 erda-ui/shell/
 */
const MODULE_PATH: string = `${process.cwd()}/`;

export default class Base {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  private async clickEle(el: keyof HTMLElementTagNameMap, text: string) {
    await this.page.click(`${el}:has-text("${text}")`);
  }

  async fillData<T extends {}>(key: string, data: T) {
    await this.page.click(`#${key}`);
    await this.page.fill(`#${key}`, data?.[key] ?? '');
  }

  /**
   * @description upload file
   * @param filePath {string} relative path, based on erda-ui/shell/
   * @param selector {string} input selector
   */
  async uploadFile(filePath: string, selector = '[type="file"]') {
    await this.page.setInputFiles(selector, `${MODULE_PATH}${filePath}`);
  }

  async search(selector: string, keyword: string) {
    await this.page.click(selector);
    await this.page.fill(selector, keyword);
    await this.page.press(selector, 'Enter');
  }

  async clickAnchor(text: string) {
    await this.clickEle('a', text);
  }

  async clickButton(text: string) {
    await this.clickEle('button', text);
  }

  async clickLabel(text: string) {
    await this.clickEle('label', text);
  }

  async clickImg(alt: string) {
    await this.page.click(`img[alt="${alt}"]`);
  }

  async clickTdOperation(rowName: string, operationName: string) {
    await this.page.click(`tr:has-text("${rowName}") >> text=${operationName}`);
  }

  async clickById(id: string) {
    await this.page.click(`[id=${id}]`);
  }

  async toggleSwitch(selector: string, checked: boolean) {
    const [switchBtn] = await this.page.$$(selector);
    const checkedAttr = await switchBtn.getAttribute('aria-checked');
    if (checkedAttr !== `${checked}`) {
      await this.page.click(selector);
    }
  }
}
