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

interface IDDing {
  targetType: 'DingTalk address';
  targets_receiver: string;
  targets_secret: string;
}

interface IExternalApi {
  targetType: 'external api';
  targets: string;
}

interface IExternalUser {
  targetType: 'external user';
}

interface IMember {
  targetType: 'member';
}

interface IMemberRole {
  targetType: 'member role';
}

type IFormData = { name: string } & (IDDing | IMember | IExternalApi | IExternalUser | IMemberRole);

interface IFormNotificationData {
  name: string;
  timing: string[];
  notifyGroup: string;
}

export default class Notify extends Base {
  async openNewGroupModal() {
    await this.clickButton('new Group');
  }

  async fillGroupModal(data: IFormData, isCreate = false) {
    if (isCreate) {
      await this.fillData('name', data);
    }
    await this.page.click('.ant-select-selection-item');
    await this.page.click(`div[role="document"] >> text=${data.targetType}`);
    switch (data.targetType) {
      case 'member':
        await this.page.click('.load-more-selector.member-selector .ant-dropdown-trigger');
        await this.clickLabel('dice');
        await this.page.click('text=group name');
        break;
      case 'DingTalk address':
        await this.fillData('targets_receiver', data);
        await this.fillData('targets_secret', data);
        await this.page.click('text=send test notification');
        break;
      case 'external api':
        await this.fillData('targets', data);
        break;
      case 'external user':
        await this.clickButton('add external user');
        await this.page.fill('.ant-modal >> .ant-table >> :nth-match(input[type="text"], 1)', 'erda-external-user');
        await this.page.fill('.ant-modal >> .ant-table >> :nth-match(input[type="text"], 2)', 'erda@terminus.io');
        await this.page.fill('.ant-modal >> .ant-table >> :nth-match(input[type="text"], 3)', '13100000000');
        break;
      case 'member role':
        await this.page.click('.ant-select-selection-overflow');
        await this.page.click('.ant-modal >> text=应用主管');
        await this.page.click('text=group name');
        break;
    }
    await this.clickButton('ok');
  }

  async deleteRecord(name: string) {
    await this.clickTdOperation(name, 'delete');
    await this.clickButton('OK');
  }

  async openNewNotification() {
    await this.clickButton('new notification');
  }

  async fillNotificationModal(data: IFormNotificationData, isCreate = false) {
    if (isCreate) {
      await this.fillData('name', data);
    }
    await this.page.click(':nth-match(.ant-select .ant-select-selector .ant-select-selection-overflow, 1)');
    const timing = [];
    data.timing.forEach((item) => {
      timing.push(this.page.click(`.ant-select-dropdown >> text=${item}`));
    });
    await Promise.all(timing);
    await this.page.click(':nth-match(.ant-select .ant-select-selector .ant-select-selection-overflow, 1)');
    await this.page.click(':nth-match(.ant-form-item, 3) >> .ant-select');
    await this.page.click(`.ant-select-dropdown >> text=${data.notifyGroup}`);
    await this.page.click(':nth-match(.ant-select .ant-select-selector .ant-select-selection-overflow, 2)');
    await this.page.click(':nth-match(.ant-form-item, 4) >> :nth-match(.ant-select-item-option-content, 1)');
    await this.page.click(':nth-match(.ant-select .ant-select-selector .ant-select-selection-overflow, 2)');
    await this.clickButton('ok');
  }
}
