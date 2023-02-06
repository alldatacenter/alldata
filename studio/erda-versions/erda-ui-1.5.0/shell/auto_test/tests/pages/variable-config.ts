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
const ASSETS_PATH: string = `${process.cwd()}/app/images/`;

export default class VariableConfig extends Base {
  async addVariable(data) {
    await this.page.waitForSelector('button:has-text("add variable")');
    await this.clickButton('add variable');
    await this.page.fill('.ant-form-item-control-input-content #key', data.key);
    if (data.type === 'file') {
      await this.page.click(".ant-modal >> span:has-text('value')");
      await this.page.click('text=valuefile >> text=file');
      await this.clickButton('upload');
      await this.page.setInputFiles('[type="file"]', `${ASSETS_PATH}${data.file}`);
      await this.page.waitForLoadState('networkidle');
      await this.page.waitForSelector(`text=selected ${data.file}`, { state: 'visible' });
    } else if (data.type === 'value') {
      await this.fillData('value', data);
    }
    await this.fillData('comment', data);
    if (data.encrypt) {
      await this.clickById('encrypt');
    }
    await this.clickButton('ok');
  }

  async editVariable(key: string, data) {
    await this.page.click(`tr:has-text("${key}") >> text=edit`);
    if (data.type === 'value') {
      await this.fillData('value', data);
    }
    const [switchBtn] = await this.page.$$('#encrypt');
    const checked = await switchBtn.getAttribute('aria-checked');
    if (checked !== `${data.encrypt}`) {
      await this.clickById('encrypt');
    }
    await this.fillData('comment', data);
    await this.clickButton('ok');
  }

  async deleteVariable(key: string) {
    await this.page.click(`tr:has-text("${key}") >> text=delete`);
    await this.clickButton('OK');
  }

  async downVariable(key: string) {
    await this.page.click(`tr:has-text("${key}") >> text=download`);
    await this.page.waitForEvent('download');
  }

  async export(index: number) {
    await this.page.click(`:nth-match(button:has-text("export"), ${index})`);
  }
}
