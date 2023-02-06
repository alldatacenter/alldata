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

import { fromNow } from 'common/utils';
import { Avatar } from 'common';
import React from 'react';
import { Tooltip } from 'antd';
import classnames from 'classnames';
import './comment-box.scss';

interface IProps {
  user?: string;
  action: string;
  time: string;
  content: string;
  className?: string;
}

export const CommentBox = ({ user, action, time, content, className }: IProps) => {
  return (
    <div className={classnames('comment-box', className)}>
      <div className="title">
        <Avatar name={<Tooltip title={user}>{user}</Tooltip>} showName size={28} />
        <span className="mx-1"> {action}</span>
        {fromNow(time)}
      </div>
      <article
        className="md-content"
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{ __html: content }}
      />
    </div>
  );
};
