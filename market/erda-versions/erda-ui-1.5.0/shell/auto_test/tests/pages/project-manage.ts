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

export class ProjectManage extends Base {
  async fillDatas(formData) {
    await this.page.click('[id="displayName"]');
    await this.page.fill('[id="displayName"]', formData.displayName ?? '');
    await this.page.click('[id="desc"]');
    await this.page.fill('[id="desc"]', formData.desc ?? '');
  }

  private async selectCluster(env: string) {
    const selector = `.ant-select:right-of(span:has-text("${env}")) >> nth=0`;
    await this.page.click(`${selector} >> .ant-select-selection-item`);
    await this.page.click(`${selector} >> .ant-select-item >> nth=0`);
  }

  async createProject(formData) {
    switch (formData.type) {
      case 'DOP':
        await this.selectCluster('DEV');
        await this.selectCluster('TEST');
        await this.selectCluster('STAGING');
        await this.selectCluster('PROD');
        await this.page.fill('text=CPUCore >> input[type="text"]', formData.cpu);
        await this.page.fill('text=MEMGiB >> input[type="text"]', formData.mem);
        break;
      case 'MSP':
        await this.page.click('text=microservice governance project');
        break;
    }
    await this.fillDatas(formData);
    await this.clickButton('save');
    await this.page.waitForEvent('requestfinished');
  }

  async searchProject(projectName: string) {
    await this.search('[placeholder="search by project name"]', projectName);
    const total = (await this.page.$$(`td:has-text("${projectName}")`)).length;
    return total;
  }

  async jumpProject(projectName: string) {
    await this.page.click(`text=${projectName}`);
    await this.page.waitForEvent('requestfinished');
  }

  async editProject(formData) {
    await this.clickButton('edit');
    if (formData.type === 'DOP') {
      await this.page.click('label:has-text("private project")');
      await this.page.fill('text=CPUCore >> input[type="text"]', formData.cpu);
      await this.page.fill('text=MEMGiB >> input[type="text"]', formData.mem);
    }
    await this.fillDatas(formData);
    await this.clickButton('ok');
    await this.page.waitForEvent('requestfinished');
    await this.page.waitForLoadState('networkidle');
  }

  async deleteProject(projectName: string) {
    await this.clickButton('delete current project');
    await this.page.click('[placeholder="please enter project name"]');
    await this.page.fill('[placeholder="please enter project name"]', projectName);
    await this.clickButton('ok');
    await this.page.waitForEvent('requestfinished');
  }

  async clearFilter() {
    await this.page.$$eval('.anticon-close-circle:visible', (elHandles: HTMLElement[]) =>
      elHandles.forEach((el) => el.click()),
    );
  }
}
