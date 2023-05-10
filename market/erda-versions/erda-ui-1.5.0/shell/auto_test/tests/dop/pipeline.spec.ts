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

// based on https://erda.cloud/erda/dop/projects/387/testPlan/manual/1472?eventKey=0-111362-111363-111385-111426&pageNo=1&recycled=false&testSetID=111426

const addPipelineName = 'playwright____test____.yml';

Role('Manager', () => {
  test('application pipeline test', async ({ page, expectExist, wait, goTo }) => {
    // Go to https://erda.hkci.terminus.io/integration/dop/projects/123/apps/788/pipeline
    await goTo('pipeline');

    // switch to pipeline execute detail, execute pipeline
    await page.click('text=execute detail');
    await page.click('.split-page-right .ant-tabs-extra-content button >> text=add pipeline');
    await wait(3);
    await expectExist('.build-operator >> [name=play1]', 1);
    await page.click('[name="play1"]');

    await wait(3);
    await expectExist('[name="pause"]', 1);

    // add pipeline
    await page.hover('.ant-tree-title >> nth=0');
    await page.hover('.tree-node-action >> nth=0');
    await page.click('.action-btn >> text=add pipeline');
    await page.click('[placeholder="please enter pipeline name"]');
    await page.fill('[placeholder="please enter pipeline name"]', addPipelineName);
    await page.click('.ant-modal-footer >> text=ok');
    await wait(3);
    await expectExist(`.file-tree-title >> text=${addPipelineName}`, 1);

    // delete pipeline
    await page.hover(`.ant-tree-title:has-text("${addPipelineName}")`);
    await page.hover(`.ant-tree-title:has-text("${addPipelineName}") .tree-node-action`);
    await page.click('.action-btn >> text=delete');
    await page.click('.ant-popover-inner-footer >> text=OK');
    await wait(3);
    await expectExist(`.file-tree-title >> text=${addPipelineName}`, 0);

    await page.close();
  });
});
