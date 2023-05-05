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

import React from 'react';
import { message, Tooltip, Spin, Dropdown, Menu, Button } from 'antd';
import { FormModal, IF, ErdaIcon } from 'common';
import { goTo, setLS, removeLS } from 'common/utils';
import BranchSelect from './branch-select';
import { getInfoFromRefName, getSplitPathBy } from '../util';
import { RepoBreadcrumb } from './repo-breadcrumb';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';
import { find, get } from 'lodash';
import appStore from 'application/stores/application';
import { isInDiceDirectory } from 'application/common/yml-flow-util';
import { usePerm } from 'user/common';

const PureRepoNavOperation = () => {
  const [modalVisible, setModalVisible] = React.useState(false);
  const [info, tree, mode] = repoStore.useStore((s) => [s.info, s.tree, s.mode]);
  const branchInfo = appStore.useStore((s) => s.branchInfo);
  const branchAuthObj = usePerm((s) => s.app.repo.branch);
  const { changeMode } = repoStore.reducers;
  const { getRepoTree, commit } = repoStore.effects;
  const { isLocked } = info;
  const toggleModal = (visible: boolean) => {
    setModalVisible(visible);
  };

  const isRootPath = tree && tree.path === ''; // 是根目录
  // 当前已存在pipeline.yml
  const hasPipeline = find(get(tree, 'entries') || [], (item) => item.name === 'pipeline.yml');
  const getFieldsList = () => {
    const initialBranch = getInfoFromRefName(info.refName).branch;
    const fieldsList = [
      {
        label: i18n.t('dop:file directory'),
        name: 'dirName',
        rules: [{ max: 255, message: i18n.t('dop:Up to 255 characters for directory name') }],
      },
      {
        label: i18n.t('dop:commit message'),
        name: 'message',
        type: 'textArea',
        itemProps: {
          maxLength: 200,
          autoSize: { minRows: 3, maxRows: 7 },
        },
        initialValue: 'Add new directory',
      },
      {
        label: i18n.t('dop:branch to commit'),
        name: 'branch',
        type: 'select',
        initialValue: initialBranch,
        options: (info.branches || []).map((a: string) => ({ name: a, value: a })),
        itemProps: {
          disabled: true,
        },
      },
    ];
    return fieldsList;
  };

  const handleAdd = ({ dirName, ...values }: { dirName: string; message: string; branch: string }) => {
    const path = `${tree.path ? `${tree.path}/` : tree.path}${dirName}`;
    commit({
      ...values,
      actions: [
        {
          action: 'add',
          path,
          pathType: 'tree',
        },
      ],
    }).then((res) => {
      toggleModal(false);
      if (res.success) {
        getRepoTree();
        message.success(i18n.t('dop:folder is created successfully'));
      }
    });
  };

  const isEditing = mode.addFile || mode.editFile;
  const showOps = tree.type === 'tree' && !isEditing;
  if (!showOps) {
    return null;
  }

  const { branch, tag } = getInfoFromRefName(tree.refName);
  const isBranchTree = branch; // 只能在branch下进行操作
  const isProtectBranch = get(find(branchInfo, { name: branch }), 'isProtect');
  const branchAuth = isProtectBranch ? branchAuthObj.writeProtected.pass : branchAuthObj.writeNormal.pass;

  const disabledTips = branchAuth
    ? [
        i18n.t('dop:creating file is only allowed under the branch'),
        i18n.t('dop:creating folder is only allowed under the branch'),
      ]
    : [
        i18n.t('dop:branch is protected, you have no permission yet'),
        i18n.t('dop:branch is protected, you have no permission yet'),
      ];

  const curBranch = branch || tag || info.defaultBranch;
  const inIndexPage = window.location.pathname.match(/apps\/\d+\/repo$/);

  const addMenu = (
    <Menu
      onClick={(e: any) => {
        if (e.key === 'file') {
          changeMode({ addFile: true });
        } else if (e.key === 'folder') {
          toggleModal(true);
        }
      }}
    >
      <Menu.Item disabled={isLocked} key="file">
        {i18n.t('dop:create new file')}
      </Menu.Item>
      <Menu.Item disabled={isLocked} key="folder">
        {i18n.t('dop:create folder')}
      </Menu.Item>
    </Menu>
  );

  return (
    <div className="repo-operation flex">
      <IF check={isBranchTree && branchAuth}>
        <IF check={isInDiceDirectory(tree.path)}>
          <Button onClick={() => changeMode({ addFile: true, addFileName: 'pipelineYml' })}>
            {i18n.t('dop:add pipeline')}
          </Button>
        </IF>
        <IF check={isRootPath}>
          {hasPipeline ? (
            <Button
              disabled={isLocked}
              onClick={() => {
                if (inIndexPage) {
                  goTo(`./tree/${curBranch}/pipeline.yml?editPipeline=true`, { forbidRepeat: true });
                } else {
                  goTo('./pipeline.yml?editPipeline=true', { forbidRepeat: true });
                }
              }}
            >
              {i18n.t('edit {name}', { name: i18n.t('pipeline') })}
            </Button>
          ) : (
            <Button disabled={isLocked} onClick={() => changeMode({ addFile: true, addFileName: 'pipelineYml' })}>
              {i18n.t('dop:add pipeline')}
            </Button>
          )}
        </IF>
        <Dropdown overlay={addMenu}>
          <Button className="ml-2 flex items-center">
            {i18n.t('add')}
            <ErdaIcon type="caret-down" size="18" className="ml-1 hover" color="black-400" />
          </Button>
        </Dropdown>
        <IF.ELSE />
        <IF check={isRootPath && !hasPipeline}>
          <Tooltip title={disabledTips[0]}>
            <Button disabled>{i18n.t('dop:add pipeline')}</Button>
          </Tooltip>
        </IF>
        <Tooltip title={disabledTips[0]}>
          <Button className="ml-2 flex items-center" disabled>
            {i18n.t('add')}
            <ErdaIcon type="caret-down" size="18" className="ml-1 hover" color="black-400" />
          </Button>
        </Tooltip>
      </IF>

      <FormModal
        width={620}
        title={i18n.t('dop:create new folder')}
        fieldsList={getFieldsList()}
        visible={modalVisible}
        onOk={handleAdd}
        onCancel={() => toggleModal(false)}
      />
    </div>
  );
};

export const RepoNavOperation = React.memo(PureRepoNavOperation);

interface IProps {
  info: REPOSITORY.IInfo;
  tree: REPOSITORY.ITree;
  isFetchingInfo: boolean;
  appId: number;
}

export const RepoNav = React.forwardRef(({ info, tree, isFetchingInfo, appId }: IProps, ref) => {
  const { before } = getSplitPathBy('tree');

  const { refName, branches = [], tags = [], defaultBranch } = info;

  const { branch, commitId, tag } = getInfoFromRefName(refName);
  const { branch: treeBranch, commitId: treeCommitId } = getInfoFromRefName(tree.refName);
  const curBranch = treeCommitId || treeBranch || commitId || branch || tag || defaultBranch;
  const isTag = !!tag;
  const selected = {
    type: isTag ? 'tag' : treeCommitId ? 'commitId' : 'branch',
    current: curBranch,
  };

  React.useImperativeHandle(
    ref,
    () => {
      return {
        target: selected,
      };
    },
    [selected],
  );

  const changeBranch = React.useCallback(
    (selectedBranch: string) => {
      if (branches.includes(selectedBranch)) {
        // save branch info to LS
        setLS(`branch-${appId}`, selectedBranch);
        removeLS(`tag-${appId}`);
      }
      if (tags.includes(selectedBranch)) {
        // save tag info to LS
        setLS(`tag-${appId}`, selectedBranch);
        removeLS(`branch-${appId}`);
      }
      goTo(`${before}/${selectedBranch}${tree.path ? `/${tree.path}` : ''}`, { append: false });
    },
    [appId, before, branches, tags, tree.path],
  );

  return (
    <Spin spinning={isFetchingInfo}>
      <div className="nav-block">
        <BranchSelect {...{ branches, commitId: treeCommitId, tags, current: curBranch }} onChange={changeBranch}>
          <span>{isTag ? i18n.t('tag') : treeCommitId ? i18n.t('commit') : i18n.t('dop:branch')}:</span>
          <span className="branch-name font-bold nowrap">{curBranch}</span>
          <ErdaIcon type="caret-down" className="mt-0.5" size="22" />
        </BranchSelect>
        <RepoBreadcrumb path={tree.path}>
          <RepoNavOperation />
        </RepoBreadcrumb>
      </div>
    </Spin>
  );
});
