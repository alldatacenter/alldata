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

import { Role, test } from '../../fixtures';

// based on https://erda.cloud/erda/dop/projects/387/testPlan/manual/1472?eventKey=0-111362-111363-111385-111439&pageNo=1&recycled=false&testSetID=111439
Role('Manager', () => {
  test('test', async ({ page, expectExist, wait, goTo }) => {
    // Go to https://erda.hkci.terminus.io/integration/dop/projects/123/apps/788/deploy
    await goTo('deploy');

    if (page.$$('.sm-more-icon svg')) {
      // already has deploy exist
      // Click operation
      await page.click('.sm-more-icon svg');
      await expectExist('text=update', 1); // check menu exists
      await expectExist('text=delete', 1);
      await expectExist('text=restart', 1);

      // Click text=update
      await page.click('text=update');
      await expectExist('text=Quickly update from artifacts', 1); // show modal
      await page.click('button:has-text("cancel")');

      // click delete
      await page.click('.sm-more-icon svg');
      await page.click('text=delete');
      await expectExist('text=confirm deletion？', 1); // confirm modal
      await page.click('button:has-text("no")');

      // Click .branch
      await page.click('.branch');

      await wait(3);
      // service plugin exist
      await expectExist('.addon-card');
      // open detail
      await page.click('text=spa-site');
      // has service details tab
      await expectExist('span:has-text("service details")', 1);

      // Click text=container monitor
      await page.click('text=container monitor');

      // has drawer shown
      await expectExist(':nth-match(:text("container monitor"), 2)', 1);

      // Click [aria-label="Close"]  close drawer
      await page.click('[aria-label="Close"]');

      // Click text=pod detail  navigate to pod detail
      await page.click('text=pod detail');
      // has pod IP column
      await expectExist('text=pod IP', 1);

      // Click [aria-label="icon: more"] click more button
      await page.click('[aria-label="icon: more"]');
      // Click text=history
      await page.click('text=history');
      await expectExist(':nth-match(:text("history"), 2)', 1);
      await expectExist(':nth-match(:text("host address"), 2)', 1);
      await page.click('[aria-label="Close"]');

      // test scale out
      await page.click('[aria-label="icon: more"]');
      await page.click('text=scale out');
      await expectExist('text=resource adjust', 1);
      await expectExist('label:has-text("Scale")', 1);
      await page.click('button:has-text("cancel")');

      // test internal address
      await page.click('[aria-label="icon: more"]');
      await page.click('text=internal address');
      await expectExist('button:has-text("close")', 1);
      await page.click('button:has-text("close")');

      // test manage domain
      await page.click('[aria-label="icon: more"]');
      await page.click('text=manage domain');
      await expectExist('text=custom domain name:', 1); // show modal
      await page.click('button:has-text("Cancel")');

      // Click button:has-text("deployment operation") check deployment operation
      await page.click('button:has-text("deployment operation")');
      await expectExist('text=restart', 1); // check menu exists
      await expectExist('text=rollback', 1);
    } else {
      // create new instance
      await page.click(
        'text=test environmentTest@font-face{font-family:feedback-iconfont;src:url(//at. >> :nth-match(div, 5)',
      );
      // Click input[role="combobox"]
      await page.click('input[role="combobox"]');
      // Click :nth-match(:text("develop"), 4)
      await page.click(':nth-match(:text("develop"), 4)');
      // Click text=branchdevelopdevelopdevelophotfix/testdevelophotfix/testmaster >> div
      await page.click('text=branchdevelopdevelopdevelophotfix/testdevelophotfix/testmaster >> div');
      // Click .ant-form-item-control-input-content .load-more-selector .ant-dropdown-trigger .values
      await page.click('.ant-form-item-control-input-content .load-more-selector .ant-dropdown-trigger .values');
      // select first artifact to deploy
      await page.click('.load-more-selector-container .list .load-more-list-item:nth-child(1)');
      // submit
      await page.click('button:has-text("ok")');

      await expectExist('.branch', 1); // instance created
    }
  });
});
