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

import React, { useEffect } from 'react';
import { Row, Col, Spin, Popover } from 'antd';
import FileContainer from 'application/common/components/file-container';
import { Avatar, Icon as CustomIcon, FileEditor, Copy } from 'common';
import { fromNow, goTo } from 'common/utils';
import i18n from 'i18n';

import './repo-blame.scss';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';

const LINE_HEIGHT = 21; // px

interface IProps {
  blob: REPOSITORY.IBlob;
  blame: REPOSITORY.IBlame[];
  name: string;
  className: string;
  path: string;
  ops?: React.ElementType | null;
  params: any;
  isFetchingRepoBlame: boolean;
  getRepoBlame: (mode: object) => Promise<any>;
}

interface IRepoBlameCommitItem {
  style: {};
  commitId: string;
  commitMessage: string;
  author: REPOSITORY.ICommitter;
  params: any;
}

const RepoBlameCommitItem = ({ style, commitId, commitMessage, author, params }: IRepoBlameCommitItem) => {
  return (
    <div className="blame-commit-item" style={style}>
      <div className="blame-commit-info flex justify-between items-center">
        <div className="info-left nowrap mr-4">
          <Avatar className="mr-1" size={18} name={author.name} />
          <Popover
            overlayClassName="blame-commit-item-popover"
            placement="topLeft"
            content={
              <div className="commit-info">
                <div className="main-info mb-2">
                  <span className="commit-msg font-bold">{commitMessage}</span>
                </div>
                <div className="sub-info">
                  <Avatar className="mr-2" name={author.name} />
                  <span className="commit-when mr-4">
                    {author.name} {i18n.t('dop:submitted in')} {fromNow(author.when)}
                  </span>
                  <span
                    className="cursor-copy commit-sha hover-text"
                    data-clipboard-text={commitId}
                    data-clipboard-tip=" commit SHA "
                  >
                    <CustomIcon type="commit" />
                    <span className="sha-text">{commitId.slice(0, 6)}</span>
                  </span>
                  <Copy selector=".cursor-copy" />
                </div>
              </div>
            }
          >
            <span className="commit-msg hover-text" onClick={() => goTo(goTo.pages.commit, { ...params, commitId })}>
              {commitMessage}
            </span>
          </Popover>
        </div>
        <span className="info-right">
          {i18n.t('dop:submitted in')} {fromNow(author.when)}
        </span>
      </div>
    </div>
  );
};

export const RepoBlame = ({ name, className, path, ops }: IProps) => {
  let content = null;
  const [blob, blame] = repoStore.useStore((s) => [s.blob, s.blame]);
  const { getRepoBlame } = repoStore.effects;
  const params = routeInfoStore.useStore((e) => e.params);
  const [isFetchingRepoBlame] = useLoading(repoStore, ['getRepoBlame']);
  const fileExtension = name.split('.').pop();
  const { content: blobContent = '' } = blob;
  const blobLength = blobContent.split(/\r\n|\r|\n/).length;

  useEffect(() => {
    getRepoBlame({ path });
  }, [getRepoBlame, path]);

  if (typeof blobContent !== 'string') {
    content = null;
  } else {
    content = (
      <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
        <Row className="blame-content h-full">
          <Col span={8}>
            <Spin spinning={isFetchingRepoBlame}>
              <div className="blame-commits-content">
                {blame.map(({ commit: { commitMessage, author, id }, startLineNo, endLineNo }, key) => {
                  const blameLineNum =
                    blame.length === key + 1 ? blobLength - startLineNo : endLineNo - startLineNo + 1;
                  return (
                    <RepoBlameCommitItem
                      key={startLineNo}
                      style={{ height: `${blameLineNum * LINE_HEIGHT}px` }}
                      commitId={id}
                      commitMessage={commitMessage}
                      author={author}
                      params={params}
                    />
                  );
                })}
              </div>
            </Spin>
          </Col>
          <Col span={16} className="h-full">
            <FileEditor
              name={name}
              fileExtension={fileExtension || 'text'}
              value={blobContent}
              readOnly
              setOptions={{
                showFoldWidgets: false,
              }}
              style={{ lineHeight: `${LINE_HEIGHT}px` }}
              maxLines={blobLength}
            />
          </Col>
        </Row>
      </FileContainer>
    );
  }
  return content;
};
