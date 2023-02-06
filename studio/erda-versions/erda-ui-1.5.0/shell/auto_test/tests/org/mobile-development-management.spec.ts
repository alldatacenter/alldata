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

import { Role, test, expect } from '../../fixtures';
import Base from '../pages/base';
const title = 'terminus' + Date.now();
const testData = {
  image: 'app/images/Erda.png',
};

Role('Manager', () => {
  test('Mobile development management', async ({ page, expectExist, goTo }) => {
    // Publisher information editing
    const base = new Base(page);
    await goTo('projectManagement');
    await page.click('text=Mobile development management');
    expect(page.url()).toMatch(/\/orgCenter\/market\/publisher\/setting/);
    await page.click('button:has-text("edit")');
    await page.click('textarea');
    await page.fill('textarea', title);
    await page.click('.ant-upload');
    await base.uploadFile(testData.image, '[type="file"]');
    await page.click('button:has-text("ok")');
    await expectExist(`text=${title}`, 1);

    // Add certificate
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/orgCenter/market/publisher/certificate?pageNo=1' }*/),
      page.click('text=certificate'),
    ]);
    await page.click('button:has-text("add certificate")');
    await page.click('text=namedescriptiontypeplease select >> input[type="text"]');
    await page.fill('text=namedescriptiontypeplease select >> input[type="text"]', title);
    await page.click('#desc');
    await page.fill('#desc', title);
    await page.click('input[role="combobox"]');
    await page.click(':nth-match(:text("Android"), 2)');
    await page.fill(':nth-match(input[type="radio"], 2)', 'false');
    await page.click('text=auto create');
    await page.click('#androidInfo_autoInfo_debugKeyStore_alias');
    await page.fill('#androidInfo_autoInfo_debugKeyStore_alias', title);
    await page.click('[placeholder="length is 6~30"]');
    await page.fill('[placeholder="length is 6~30"]', title);
    await page.click('#androidInfo_autoInfo_debugKeyStore_storePassword');
    await page.fill('#androidInfo_autoInfo_debugKeyStore_storePassword', title);
    await page.click('#androidInfo_autoInfo_releaseKeyStore_alias');
    await page.fill('#androidInfo_autoInfo_releaseKeyStore_alias', title);
    await page.click('#androidInfo_autoInfo_releaseKeyStore_keyPassword');
    await page.fill('#androidInfo_autoInfo_releaseKeyStore_keyPassword', title);
    await page.click('#androidInfo_autoInfo_releaseKeyStore_storePassword');
    await page.fill('#androidInfo_autoInfo_releaseKeyStore_storePassword', title);
    await page.click('#androidInfo_autoInfo_name');
    await page.fill('#androidInfo_autoInfo_name', title);
    await page.click('#androidInfo_autoInfo_ou');
    await page.fill('#androidInfo_autoInfo_ou', title);
    await page.click('#androidInfo_autoInfo_org');
    await page.fill('#androidInfo_autoInfo_org', title);
    await page.click('#androidInfo_autoInfo_city');
    await page.fill('#androidInfo_autoInfo_city', title);
    await page.click('#androidInfo_autoInfo_province');
    await page.fill('#androidInfo_autoInfo_province', title);
    await page.click('#androidInfo_autoInfo_state');
    await page.fill('#androidInfo_autoInfo_state', title);
    await page.click('button:has-text("ok")');
    await expectExist(`text=${title}`);
    await page.click('[placeholder="search by name"]');
    await expectExist(`text=${title}`);
  });
});
