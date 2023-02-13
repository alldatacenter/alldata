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

import { Spin, Tabs } from 'antd';
import React from 'react';
import i18n from 'i18n';
import { CommitList } from '../repo-commit';
import { CommentList } from './mr-comments';
import FileDiff from './file-diff';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';

const { TabPane } = Tabs;

interface IProps {
  hideComment?: boolean;
  disableComment?: boolean;
}

const CompareDetail = ({ hideComment, disableComment = false }: IProps) => {
  const [compareDetail, comments] = repoStore.useStore((s) => [s.compareDetail, s.comments]);
  const { commits = [], diff, from, to } = compareDetail;
  const [isFetching] = useLoading(repoStore, ['getCompareDetail']);

  return (
    <Spin spinning={isFetching}>
      <Tabs className="dice-tab" defaultActiveKey={!hideComment ? 'comment' : 'commit'} tabBarGutter={40}>
        {!hideComment && (
          <TabPane
            key="comment"
            tab={
              <span>
                {i18n.t('comment')}
                <span className="dice-badge">{comments.length}</span>{' '}
              </span>
            }
          >
            <CommentList comments={comments} />
          </TabPane>
        )}
        <TabPane
          key="commit"
          tab={
            <span>
              {i18n.t('commit')}
              <span className="dice-badge">{commits.length}</span>{' '}
            </span>
          }
        >
          <CommitList commits={commits} />
        </TabPane>
        <TabPane
          key="diff"
          tab={
            <span>
              {i18n.t('dop:changed files')}
              <span className="dice-badge">{diff ? diff.filesChanged : '0'}</span>{' '}
            </span>
          }
        >
          <FileDiff
            key={`${from}-${to}`}
            diff={diff}
            from={from}
            to={to}
            comments={comments}
            mode="compare"
            disableComment={disableComment}
          />
        </TabPane>
      </Tabs>
    </Spin>
  );
};

export default CompareDetail;
