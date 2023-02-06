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

const TEXT = `auto_text_${Date.now()}`;
const pipelineDir = '.dice/pipelines';

Role('Manager', () => {
  test('repo', async ({ page, expectExist, wait, goTo }) => {
    // Go to https://erda.hkci.terminus.io/erda/dop/projects/1/apps/16/repo/tree/master
    test.setTimeout(240000);
    await goTo('repo');
    await wait(2);

    // view doc
    await page.click('.icon-help');
    await wait(2);
    await expectExist('.repo-start-tip', 1);
    await wait(2);
    await page.click('.ant-modal-close-x');

    // test repo
    await page.click('.repo-clone-btn');
    await wait(2);
    await expectExist('.clone-content', 1);
    await page.click('.clone-content .addr .copy-btn');
    await wait(2);
    await page.click('.ant-popover-inner-content .copy-btn >> nth=1');
    await wait(2);
    await page.click('.ant-popover-inner-content .copy-btn >> nth=2');
    await wait(2);
    await page.click('.download-btn-group .ant-btn >> nth=0');
    await wait(2);

    // add branch
    await page.click('.top-button-group button >> text="new branch"');
    await wait(3);
    await page.fill('.ant-modal-body .ant-input >> nth=0', TEXT);
    await wait(2);
    await page.click('.ant-modal-footer span >> text="ok"');
    await wait(5);

    await page.click('.repo-branch-select');
    await wait(2);
    await page.click('.list-wrap .branch-item span >> text="master"');
    await wait(3);
    // change branch
    await page.click('.repo-branch-select');
    await wait(2);
    await page.click(`.list-wrap .branch-item span >> text=${TEXT}`);
    await wait(2);

    // edit branch
    await expectExist('.commit-left .commit-content', 1); // content-commit
    await page.click('.commit-right .cursor-copy'); // content-date
    await wait(3);
    await page.click('.commit-content .as-link'); // content-date
    await wait(3);
    await expectExist('.erda-header-title-text span >> text="commit details"');

    // get branch detail
    await goTo('repo');
    await wait(5);
    await page.click('.ant-table-row >> nth=0');
    await wait(3);

    await goTo('repo');
    await wait(3);

    // >> create folder
    await page.hover(`.repo-operation >> text="add"`);
    await page.click('.ant-dropdown-menu-item >> text="create folder"');
    await wait(3);
    await page.click('.ant-modal .ant-form-item-control-input-content #dirName');
    await page.fill('.ant-modal .ant-form-item-control-input-content #dirName', TEXT);
    await wait(2);
    await page.click('.ant-modal-footer button span >> text="ok"');
    await wait(2);
    await expectExist('.ant-message-success', 1);

    // >> add file
    await page.click(`.column-name >> text=${TEXT}`);
    await wait(3);
    await page.hover(`.repo-operation >> text=add`);
    await page.click('.ant-dropdown-menu-item >> text="create new file"');
    await wait(2);
    await page.click('[placeholder="file name"]');
    await page.fill('[placeholder="file name"]', TEXT);
    await page.click('.ant-form .ant-btn-primary');

    // >> edit file
    await wait(2);
    await page.click(`.column-name >> text=${TEXT}`);
    await wait(2);
    await page.click(`.file-ops div >> nth=1`);
    await wait(2);
    await page.click(`.ace_editor`);
    await page.fill('.ace_editor textarea', TEXT);
    await page.fill('.ant-form textarea', TEXT);
    await wait(2);
    await page.click(`.ant-form .ant-btn-primary`);
    await wait(2);
    await expectExist('.ant-message-success', 1);

    // >> delete
    await wait(2);
    await page.click(`.file-ops iconpark-icon >> nth=1`);
    await wait(2);
    await page.click('.ant-modal-footer .ant-btn-primary');
    await wait(2);
    await expectExist('.ant-message-success', 1);

    // text pipeline
    await goTo('repo');
    await wait(2);
    await page.click(`.repo-tree .ant-table-row .ant-table-cell >> text=${pipelineDir}`);
    await wait(2);
    await page.click('.repo-operation >> text="add pipeline"');
    await wait(2);
    await expectExist('.repo-add-pipelineyml', 1);
    await expectExist('.add-pipeline-file-container', 1);
    await wait(2);

    await page.click('.add-line >> nth=0');
    await expectExist('.yml-node-drawer', 1);
    await page.click('.ant-collapse-content .dice-yml-actions >> nth=0'); // code clone
    await wait(2);
    await page.click('.yml-node-drawer .ant-btn-primary span >> text="save"');
    await wait(2);

    await page.click('.add-line >> nth=1');
    await page.click('.ant-collapse-content .dice-yml-actions >> nth=0'); // code clone
    await page.click('.ant-drawer-body .ant-form input >> nth=0');
    await page.fill('.ant-drawer-body .ant-form input >> nth=0', 'git-checkout-2'); // add second node
    await wait(2);
    await page.click('.yml-node-drawer .ant-btn-primary span >> text="save"');
    await wait(2);

    await page.click('.add-line >> nth=2');
    await page.click('.ant-collapse-content .dice-yml-actions >> nth=0');
    await page.fill('.ant-drawer-body .ant-form input >> nth=0', 'git-checkout-insert'); // insert node in middle
    await page.click('.yml-node-drawer .ant-btn-primary span >> text="save"');
    await wait(2);

    await page.click('.ant-dropdown-trigger >> nth=1');
    await wait(2);
    await page.click('.ant-dropdown-menu-item-only-child >> text="delete"');
    await wait(2);
    await expectExist('.ant-message-success', 1);

    // edit node
    await page.click('.yml-chart-node >> nth=2');
    await wait(2);
    await page.click('.ant-drawer-body .ant-form input >> nth=0');
    await page.fill('.ant-drawer-body .ant-form input >> nth=0', 'git-checkout-change');
    await wait(2);
    await page.click('.yml-node-drawer .ant-btn-primary span >> text="save"');
    await wait(2);

    // change template
    await page.click('.pipeline-template-item >> nth=1');
    await wait(3);
    await expectExist('.pipeline-node', 4);

    await page.click('.file-ops .icon-zhongzhi');
    await wait(2);

    await page.click('.file-ops .icon-html1');
    await wait(3);

    await page.click('.file-container .file-title input');
    await page.fill('.file-container .file-title input', TEXT);
    await wait(3);
    await page.click('.file-container .file-content .ant-form .ant-btn-primary span');
    await wait(3);
    await page.click(`.column-name >> text="${TEXT}.yml"`);
    await wait(3);
    await page.click(`.editor-ops iconpark-icon >> nth=1`);
    await wait(2);
    await page.click('.ant-modal-footer button span >> text="ok"');
    await wait(2);
    await expectExist(`.column-name >> text="${TEXT}.yml"`, 0);

    await page.close();
  });
});
