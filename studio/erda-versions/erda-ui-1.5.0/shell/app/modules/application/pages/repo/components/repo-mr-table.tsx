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
import { Spin, Tooltip } from 'antd';
import { LoadMore, EmptyListHolder, Avatar, Icon as CustomIcon, CustomFilter, MemberSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import { get } from 'lodash';
import { useEffectOnce } from 'react-use';
import { goTo, fromNow } from 'common/utils';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import { useUserMap } from 'core/stores/userMap';
import './repo-mr-table.scss';
import repoStore from 'application/stores/repo';

interface IProps {
  type: REPOSITORY.MrType;
}

interface IState {
  authorId: undefined | number;
  assigneeId: undefined | number;
}

const RepoMrTable = ({ type }: IProps) => {
  const [mrList, mrPaging] = repoStore.useStore((s) => [s.mrList, s.mrPaging]);
  const userMap = useUserMap();

  const [state, , update] = useUpdate({
    authorId: undefined,
    assigneeId: undefined,
  } as IState);
  const { getMrList } = repoStore.effects;
  const { clearMrList: resetMrPaging } = repoStore.reducers;
  const [isFetching] = useLoading(repoStore, ['getMrList']);
  const handlePaging = () => {
    return getMrList({
      state: type,
      pageNo: mrPaging.pageNo + 1,
      ...state,
    });
  };
  useEffectOnce(() => {
    getMrList({
      state: type,
    });
    return () => {
      resetMrPaging();
    };
  });

  const Holder = ({ children }: any) => (isFetching || mrList.length ? children : <EmptyListHolder />);

  const filterConfig = React.useMemo(
    () => [
      {
        type: MemberSelector,
        name: 'authorId',
        customProps: {
          placeholder: i18n.t('please choose {name}', { name: i18n.t('default:submitter') }),
          scopeType: 'app',
        },
      },
      {
        type: MemberSelector,
        name: 'assigneeId',
        customProps: {
          placeholder: i18n.t('please choose {name}', { name: i18n.t('default:designated person') }),
          scopeType: 'app',
        },
      },
    ],
    [],
  );

  const handleSubmit = (payload: Omit<REPOSITORY.QueryMrs, 'pageNo' | 'state'>) => {
    update(payload);
    getMrList({
      state: type,
      pageNo: 1,
      ...payload,
    });
  };

  const handleReset = (payload: Record<string, any>) => {
    update(payload);
    getMrList({
      state: type,
      pageNo: 1,
    });
  };

  return (
    <React.Fragment>
      <CustomFilter onSubmit={handleSubmit} onReset={handleReset} config={filterConfig} />
      <Spin spinning={isFetching}>
        <Holder>
          <ul className="repo-mr-list">
            {mrList.map((item: any) => {
              const actorMap = {
                open: 'authorId',
                closed: 'closeUserId',
                merged: 'mergeUserId',
              };
              const actionMap = {
                open: i18n.t('submit'),
                closed: i18n.t('close'),
                merged: i18n.t('dop:merged'),
              };
              const updateKeyMap = {
                open: 'createdAt',
                closed: 'closeAt',
                merged: 'mergeAt',
              };
              const curActor = get(userMap, `${item[actorMap[item.state]]}`) || {};
              const assigneeUser = get(userMap, `${item.assigneeId}`) || {};
              const authorUser = get(userMap, `${item.authorId}`) || {};
              return (
                <li key={item.id} className="mr-item hover-active-bg" onClick={() => goTo(`./${item.mergeId}`)}>
                  <div className="title font-bold">
                    {item.title}
                    <span className="text-sm desc ml-3 font-normal">
                      {item.sourceBranch} <CustomIcon type="arrow-right" />
                      {item.targetBranch}
                    </span>
                  </div>
                  <div className="desc">
                    {i18n.t('dop:assigned user')}：<Tooltip title={assigneeUser.name}>{assigneeUser.nick}</Tooltip>
                  </div>
                  <div className="sub-title flex items-center flex-wrap justify-start">
                    <span className="mr-1">#{item.mergeId}</span>
                    <span className="mr-6 flex items-center flex-wrap justify-start">
                      <Tooltip title={curActor.name}>
                        <Avatar className="mb-1 mr-1" showName name={curActor.nick} />
                      </Tooltip>
                      &nbsp;{actionMap[item.state]}&nbsp;{i18n.t('at')} {fromNow(item[updateKeyMap[item.state]])}
                    </span>
                  </div>
                  <div className="desc flex items-center flex-wrap justify-start">
                    <span className="mr-1">
                      <Avatar showName name={<Tooltip title={authorUser.name}>{authorUser.nick}</Tooltip>} />
                    </span>
                    <span>
                      {i18n.t('created at')} {fromNow(item.createdAt)}
                    </span>
                  </div>
                </li>
              );
            })}
          </ul>
        </Holder>
      </Spin>
      <LoadMore key={type} load={handlePaging} hasMore={mrPaging.hasMore} isLoading={isFetching} />
    </React.Fragment>
  );
};

// const RepoMrTableWrapper = RepoMrTable;

export { RepoMrTable };
