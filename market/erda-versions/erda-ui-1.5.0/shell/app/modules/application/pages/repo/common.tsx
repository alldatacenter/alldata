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
import { Tooltip } from 'antd';
import { Copy, Avatar } from 'common';
import { fromNow, replaceEmoji } from 'common/utils';
import { renderAsLink } from './util';
import i18n from 'i18n';

import './common.scss';

interface ICommit {
  id: string;
  author: REPOSITORY.ICommitter;
  commitMessage: string;
}

export const CommitBlock = ({ commit }: { commit?: ICommit }) => {
  if (!commit) {
    return null;
  }
  const { id, author, commitMessage } = commit;
  const msg = replaceEmoji(commitMessage);
  return (
    <div className="repo-commit-block flex justify-between items-center">
      <div className="commit-left">
        <Avatar name={author.name} showName />
        <span className="commit-content font-bold nowrap">
          <Tooltip title={msg}>{renderAsLink('commit', id, msg)}</Tooltip>
        </span>
      </div>
      <div className="commit-right">
        {i18n.t('submitted')}&nbsp;
        <Copy className="cursor-copy font-bold" data-clipboard-tip="commit SHA" data-clipboard-text={id}>
          {id.slice(0, 6)}
        </Copy>
        &nbsp;{i18n.t('at')}&nbsp;
        <span> {fromNow(author.when)}</span>
      </div>
    </div>
  );
};
