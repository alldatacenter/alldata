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

import { Spin, Button, Tooltip, Dropdown, Menu, Alert, Input } from 'antd';
import { EmptyHolder, Avatar, DeleteConfirm, IF, ErdaIcon } from 'common';
import React from 'react';
import { fromNow, replaceEmoji, goTo } from 'common/utils';
import { mergeRepoPathWith } from './util';
import GotoCommit, { getCommitPath } from 'application/common/components/goto-commit';
import { Link } from 'react-router-dom';
import { get, find, debounce } from 'lodash';
import { WithAuth, usePerm } from 'app/user/common';
import i18n from 'i18n';
import './repo-branch.scss';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';
import appStore from 'application/stores/application';
import DOMPurify from 'dompurify';

const { Search } = Input;

export const BRANCH_TABS = [
  {
    key: 'branches',
    name: i18n.t('dop:branch'),
  },
  {
    key: 'tags',
    name: i18n.t('tag'),
  },
];

const RepoBranch = () => {
  const permMap = usePerm((s) => s.app.repo.branch);
  const [info, list] = repoStore.useStore((s) => [s.info, s.branch]);
  const branchInfo = appStore.useStore((s) => s.branchInfo);
  const { deleteBranch, getListByType, setDefaultBranch } = repoStore.effects;
  const { clearListByType } = repoStore.reducers;
  const [isFetching] = useLoading(repoStore, ['getListByType']);
  React.useEffect(() => {
    getListByType({ type: 'branch' });
    return () => {
      clearListByType('branch');
    };
  }, [getListByType, clearListByType]);
  const goToCompare = (branch: string) => {
    goTo(`./compare/${info.defaultBranch}...${encodeURIComponent(branch)}`);
  };
  const getList = debounce((branch: string) => {
    getListByType({ type: 'branch', findBranch: branch });
  }, 300);

  const handleChangeBranchName = (e: React.ChangeEvent<HTMLInputElement>) => {
    getList(e.target.value);
  };

  return (
    <Spin spinning={isFetching}>
      <Search
        className="repo-branch-search-input mb-4"
        placeholder={i18n.t('common:search by {name}', { name: i18n.t('dop:branch') })}
        onChange={handleChangeBranchName}
      />
      <IF check={info.isLocked}>
        <Alert message={i18n.t('lock-repository-tip')} type="error" />
      </IF>
      <IF check={list.length}>
        <div className="repo-branch-list">
          {list.map((item) => {
            const { name, id, commit, isProtect, isDefault, isMerged } = item;
            const { commitMessage, author = {} } = commit || {};
            const { name: committerName, when } = author as any;
            const isProtectBranch = get(find(branchInfo, { name }), 'isProtect');
            const curAuth = isProtectBranch ? permMap.writeProtected.pass : permMap.writeNormal.pass;
            return (
              <div key={name} className="branch-item flex justify-between items-center">
                <div className="branch-item-left">
                  <div className="font-medium flex items-center text-base mb-3">
                    {isProtect ? (
                      <Tooltip title={i18n.t('protected branch')}>
                        <ErdaIcon size="22" type="baohu" />
                      </Tooltip>
                    ) : (
                      <ErdaIcon fill="black-800" size="22" type="fz" />
                    )}
                    <Link to={mergeRepoPathWith(`/tree/${name}`)}>
                      <span className="text-normal hover-active">{name}</span>
                    </Link>
                    {isDefault && <span className="tag-primary">{i18n.t('default')}</span>}
                    {isMerged && <span className="tag-success">{i18n.t('dop:Merged')}</span>}
                  </div>
                  <div className="flex items-center text-sub">
                    <span className="inline-flex items-center">
                      <Avatar showName name={committerName} />
                      &nbsp;{i18n.t('committed at')}
                    </span>
                    <span className="ml-1">{fromNow(when)}</span>
                    <span className="ml-6 text-desc nowrap flex branch-item-commit">
                      <GotoCommit length={6} commitId={id} />
                      &nbsp;·&nbsp;
                      <Tooltip title={commitMessage.length > 50 ? commitMessage : null}>
                        <Link className="text-desc nowrap hover-active" to={getCommitPath(id)}>
                          {replaceEmoji(commitMessage)}
                        </Link>
                      </Tooltip>
                    </span>
                  </div>
                </div>
                <div className="branch-item-right flex">
                  <Button className="mr-3" disabled={info.isLocked} onClick={() => goToCompare(name)}>
                    {i18n.t('compare')}
                  </Button>
                  <DeleteConfirm
                    title={i18n.t('common:confirm to delete {name} ?', {
                      name: `${name} ${i18n.t('dop:branch')}`,
                      interpolation: { escape: (str) => DOMPurify.sanitize(str) },
                    })}
                    onConfirm={() => {
                      deleteBranch({ branch: name });
                    }}
                  >
                    <WithAuth pass={curAuth}>
                      <Button disabled={info.isLocked || isDefault} className="mr-3" danger>
                        {i18n.t('delete')}
                      </Button>
                    </WithAuth>
                  </DeleteConfirm>
                  <Dropdown
                    overlay={
                      <Menu
                        onClick={(e) => {
                          switch (e.key) {
                            case 'setDefault':
                              setDefaultBranch(name);
                              break;
                            default:
                              break;
                          }
                        }}
                      >
                        <Menu.Item key="setDefault" disabled={!curAuth || info.isLocked || isDefault}>
                          {i18n.t('dop:set as default')}
                        </Menu.Item>
                      </Menu>
                    }
                  >
                    <Button>
                      <ErdaIcon className="hover mt-1" fill="black-800" size="16" type="more" />
                    </Button>
                  </Dropdown>
                </div>
              </div>
            );
          })}
        </div>
        <IF.ELSE />
        <EmptyHolder relative style={{ justifyContent: 'start' }} />
      </IF>
    </Spin>
  );
};

export default RepoBranch;
