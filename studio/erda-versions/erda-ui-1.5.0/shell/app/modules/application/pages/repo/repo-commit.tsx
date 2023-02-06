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

import { map } from 'lodash';
import React from 'react';
import { Spin, Button, Timeline, Input } from 'antd';
import { Copy, LoadMore, Holder, Avatar, IF, ErdaIcon } from 'common';
import { fromNow, goTo, replaceEmoji, setLS } from 'common/utils';
import BranchSelect from './components/branch-select';
import { RepoBreadcrumb } from './components/repo-breadcrumb';
import { renderAsLink, getSplitPathBy, getInfoFromRefName, mergeRepoPathWith } from './util';
import { Link } from 'react-router-dom';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';

import './repo-commit.scss';

const { Item: TimelineItem } = Timeline;

export const renderCommitItem = ({ id, author, commitMessage }: REPOSITORY.ICommit) => {
  return (
    <div key={id} className="commit-item flex justify-between items-center">
      <div className="commit-left">
        <div className="commit-title mb-2 nowrap">
          <Link to={mergeRepoPathWith(`/commit/${id}`)}>
            <span className="text-normal text-base hover-active font-bold">{replaceEmoji(commitMessage)}</span>
          </Link>
        </div>
        <div className="flex items-center justify-start">
          <div className="text-sub">
            <Avatar className="mb-1" showName name={author.name} />
          </div>
          <span className="ml-1">{i18n.t('committed at')}</span>
          <span className="text-sub ml-1">{fromNow(author.when)}</span>
          <span className="cursor-copy commit-sub-sha flex" data-clipboard-text={id} data-clipboard-tip=" commit SHA ">
            <ErdaIcon fill="black-400" size="16" type="commit" />
            <span className="sha-text">{id.slice(0, 6)}</span>
          </span>
          <Copy selector=".cursor-copy" />
        </div>
      </div>
      <div className="commit-right">{renderAsLink('tree', id, <Button>{i18n.t('dop:code')}</Button>)}</div>
    </div>
  );
};

export const CommitList = ({ commits = [] }: { commits: REPOSITORY.ICommit[] }) => {
  return (
    <div className="commit-list">
      <Holder when={!commits.length}>{commits.map(renderCommitItem)}</Holder>
    </div>
  );
};

const RepoCommit = () => {
  const [info, commitPaging, list] = repoStore.useStore((s) => [s.info, s.commitPaging, s.commit]);
  const { getCommitList } = repoStore.effects;
  const { resetCommitPaging, clearListByType } = repoStore.reducers;
  const { appId } = routeInfoStore.useStore((s) => s.params);
  const [isFetching] = useLoading(repoStore, ['getCommitList']);
  const [searchValue, setSearchValue] = React.useState('');

  const branchesStr = JSON.stringify(info?.branches);

  React.useEffect(() => {
    branchesStr && getCommitList({ pageNo: 1 });
  }, [branchesStr, getCommitList]);

  React.useEffect(() => {
    return () => {
      resetCommitPaging();
      clearListByType('commit');
    };
  }, [clearListByType, resetCommitPaging]);
  const { branches = [], tags = [], refName } = info;

  const onBranchChange = (branch: string) => {
    const { before } = getSplitPathBy('commits');
    if (branches.includes(branch)) {
      // save branch info to LS
      setLS(`branch-${appId}`, branch);
    }
    if (tags.includes(branch)) {
      // save branch info to LS
      setLS(`tag-${appId}`, branch);
    }
    goTo(`${before}/${branch}`, { replace: true });
    resetCommitPaging();
    getCommitList({ branch, pageNo: 1 });
    setSearchValue('');
  };
  const load = () => {
    const { after } = getSplitPathBy('commits');
    return getCommitList({ branch: after || info.defaultBranch, pageNo: commitPaging.pageNo + 1 });
  };

  const daySplit = {};
  list.forEach((item) => {
    const day = item.author.when.slice(0, 10);
    daySplit[day] = daySplit[day] || [];
    daySplit[day].push(item);
  });
  const { branch, commitId, tag } = getInfoFromRefName(refName);
  const path = getSplitPathBy(branch.endsWith('/') ? branch : `${branch}/`).after;
  return (
    <div className="repo-commit">
      <div className="commit-nav mb-5">
        <div className="nav-left flex justify-between items-center flex-1">
          <BranchSelect
            className="mr-4"
            {...{ branches, tags, current: branch || tag || '' }}
            onChange={onBranchChange}
          >
            {branch ? (
              <>
                <span>{i18n.t('dop:branch')}:</span>
                <span className="branch-name font-bold nowrap">{branch}</span>
              </>
            ) : tag ? (
              <>
                <span>{i18n.t('tag')}:</span>
                <span className="branch-name font-bold nowrap">{tag}</span>
              </>
            ) : (
              <>
                <span>{i18n.t('commit')}:</span>
                <span className="branch-name font-bold nowrap">{commitId}</span>
              </>
            )}
            <ErdaIcon type="caret-down" size="18px" className="mt-0.5" />
          </BranchSelect>
          <IF check={path && branch}>
            <RepoBreadcrumb splitKey="commits" path={path} />
          </IF>
        </div>
        <Input
          value={searchValue}
          className="search-input"
          placeholder={i18n.t('dop:filter by committed message')}
          onPressEnter={() => {
            getCommitList({
              search: searchValue || undefined,
              branch: getSplitPathBy('commits').after || info.defaultBranch,
              pageNo: 1,
            });
          }}
          onChange={(e) => {
            setSearchValue(e.target.value);
          }}
        />
      </div>
      <Spin spinning={isFetching}>
        <Holder when={!list.length && !isFetching}>
          <Timeline>
            {map(daySplit, (items: [], day) => (
              <TimelineItem key={day}>
                <div className="mb-4 text-normal text-base">{day}</div>
                <div className="commit-list">{items.map(renderCommitItem)}</div>
              </TimelineItem>
            ))}
            <TimelineItem />
          </Timeline>
        </Holder>
      </Spin>
      <LoadMore key={branch || ''} load={load} hasMore={commitPaging.hasMore} isLoading={isFetching} />
    </div>
  );
};

export default RepoCommit;
