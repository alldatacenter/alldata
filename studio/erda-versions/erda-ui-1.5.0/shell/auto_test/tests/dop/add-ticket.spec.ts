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
const title = 'auto_' + Date.now();

const testData = {
  title,
  image: 'app/images/Erda.png',
  svg: 'app/images/zx.svg',
};

Role('Manager', () => {
  test('add-ticket', async ({ page, expectExist, goTo }) => {
    const base = new Base(page);
    await goTo('qualityReport');
    // Click text=Issues
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/ticket/open?pageNo=1' }*/),
      page.click('text=Issues'),
    ]);

    // add ticket
    await page.click('button:has-text("add ticket")');
    await page.click('button:has-text("ok")');
    await page.fill('text=ticket titleplease input ticket title >> input[type="text"]', testData.title);
    await page.click('textarea[name="textarea"]');
    await page.fill('textarea[name="textarea"]', testData.title);
    await page.click('.button.button-type-annex');
    await page.click('span[role="button"]:has-text("image upload")');
    await base.uploadFile(testData.image, '[type="file"]');
    await base.uploadFile(testData.svg, '[type="file"]');
    await page.click('text=ticket typeplease select ticket type >> input[role="combobox"]');
    await page.click('div[role="document"] >> text=code defect');
    await page.click('span:has-text("code defect")');
    await page.click('.rc-virtual-list-holder-inner div:nth-child(2)');
    await page.click('text=vulnerabilitycode vulnerabilitybugvulnerabilitycodeSmellcode defectcode vulnerab >> div');
    await page.click('div[role="document"] >> text=code smell');
    await page.click('div[role="document"] >> text=medium');
    await page.click('div[role="document"] >> text=high');
    await page.click('div[role="document"] >> text=low');
    await page.click('button:has-text("ok")');
    await expectExist(`text=${testData.title}`, 0);

    // find about ticket
    await page.click('input[role="combobox"]');
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/ticket/open?pageNo=1&type=codeSmell' }*/),
      page.click('text=code smell'),
    ]);
    await page.click('text=no-labelfilter by priority >> input[role="combobox"]');
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/ticket/open?pageNo=1&priority=low&type=codeSmell' }*/),
      page.click('form >> :nth-match(:text("low"), 2)'),
    ]);
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/ticket/open?pageNo=1&q=aaaaaa&type=bug' }*/),
      page.fill('[placeholder="filter by title"]', testData.title),
    ]);
    await expectExist(`text=${testData.title}`);
    await page.click(`text=${testData.title}`);
    expect(page.url()).toMatch(/\/dop\/projects\/[1-9]\d*\/apps\/[1-9]\d*\/ticket\/open\/[1-9]\d*/);
    await page.click('textarea[name="textarea"]');
    await page.fill('textarea[name="textarea"]', testData.title);
    await page.click('.button.button-type-annex');
    await page.click('span[role="button"]:has-text("image upload")');
    await base.uploadFile(testData.image, '[type="file"]');
    await base.uploadFile(testData.svg, '[type="file"]');
    await page.click('button:has-text("submit comments")');
    await expectExist(`text=${testData.title}`);
    await page.click('button:has-text("close")');
    await Promise.all([
      page.waitForNavigation(/*{ url: 'https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/ticket/open?pageNo=1' }*/),
      page.click(':nth-match(:text("issues"), 2)'),
    ]);
    await page.click('text=closed');
    await page.click(`text=${testData.title}`);
    expect(page.url()).toMatch(/\/dop\/projects\/[1-9]\d*\/apps\/[1-9]\d*\/ticket\/closed\/[1-9]\d*/);
    await expectExist(`text=${testData.title}`);
    await page.close();
  });
});
