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
import { ossImg } from 'common/utils';
import classnames from 'classnames';
import { Icon as CustomIcon } from 'common';
import userStore from 'app/user/stores';

import './index.scss';

interface IProps {
  url?: string;
  useLoginUser?: boolean; // 使用当前登录用户信息
  showName?: boolean | 'tooltip'; // 展示名称
  name?: string | JSX.Element;
  size?: number;
  className?: string;
  wrapClassName?: string;
}

const Avatar = (props: IProps) => {
  const { url, showName = false, useLoginUser = false, name, size = 24, className = '', wrapClassName = '' } = props;
  const isOccupiedShowName = showName === true;
  const loginUser = userStore.useStore((s) => s.loginUser);
  const cls = classnames('dice-avatar', isOccupiedShowName ? 'mr4' : false, className);

  let userName = useLoginUser ? loginUser.nick : name;
  userName = userName || '';
  const userAvatar = useLoginUser ? loginUser.avatar : url;
  const style = { width: `${size}px`, height: `${size}px` };

  let wrapName = (child: any) => child;
  if (isOccupiedShowName) {
    wrapName = (child) => (
      <span className={`dice-avatar-wrap ${wrapClassName}`}>
        {child}
        <Tooltip title={userName}>
          <span className="nowrap flex-1">{userName}</span>
        </Tooltip>
      </span>
    );
  }

  if (showName === 'tooltip') {
    wrapName = (child) => (
      <Tooltip title={userName}>
        <span>{child}</span>
      </Tooltip>
    );
  }

  if (userAvatar) {
    return wrapName(
      <img className={cls} style={style} src={ossImg(userAvatar, { w: Math.floor(size * 1.2) })} alt="user-avatar" />,
    );
  }

  let asciiSum = 0;
  for (let index = 0; index < userName.length; index++) {
    asciiSum += userName.charCodeAt(index);
  }
  const iconIndex = String(asciiSum).slice(-1);
  return wrapName(<CustomIcon color className={cls} style={style} type={`head${iconIndex}`} />);
};

export default Avatar;
