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
import permStore from 'app/user/stores/permission';
import classnames from 'classnames';
import { get, intersection } from 'lodash';
import { Tooltip } from 'antd';
import userStore from 'app/user/stores';
import i18n from 'i18n';
import { permState } from 'user/stores/_perm-state';

const defaultTip = i18n.t('common:no permission to operate');

/**
 * 权限校验，仅限于组件外或类组件使用，需手动依赖permStore变化
 *
 * @param {string} {action,scope}
 * 暂时未用到 hasAuth, 先注释
 */
// const hasAuth = ({ action, scope }: { action: string, scope?: string }) => {
//   const permObj = permStore.getState(s => s);
//   const pass = get(permObj, `${scope ? `${scope}.` : ''}${action}.pass`);
//   return pass;
// };

interface IPermObj {
  pass: boolean;
  role: string[];
  name: string;
}

export const isCreator = (creatorId: string) => {
  const loginUserId = userStore.getState((s) => s.loginUser.id);
  return `${loginUserId}` === `${creatorId}` ? 'Creator' : '';
};

export const isAssignee = (assigneeId: string) => {
  const loginUserId = userStore.getState((s) => s.loginUser.id);
  return `${loginUserId}` === `${assigneeId}` ? 'Assignee' : '';
};

export const getAuth = (permObj: IPermObj, checkRole: string[]) => {
  if (permObj && permObj.pass) return permObj.pass;
  if (permObj && checkRole) {
    const l = intersection(permObj.role, checkRole);
    if (l.length) return true;
  }
  return false;
};

export interface IWithAuth {
  [k: string]: any;
  children: any;
  pass?: boolean | { pass: boolean; role: string[]; name: string };
  disableMode?: boolean;
  noAuthTip?: string | JSX.Element;
  tipProps?: any;
  customRole?: string;
  use?: (s: typeof permState) => boolean;
}

/**
 * 权限校验
 *
 * @param {string} action // 使用action查询
 * @param {string} pass // 直接使用store状态
 * @param {ReactNode} children
 * @param {boolean} disableMode
 * @param {string} noAuthTip
 *
 * @example
 *
 */

export const WithAuth = ({
  children,
  pass,
  disableMode = true,
  use,
  customRole,
  noAuthTip = defaultTip,
  tipProps = {},
  ...rest
}: IWithAuth) => {
  const checked = usePerm(use || (() => false));
  let authPass = pass !== undefined ? pass : checked;
  if (typeof authPass === 'object' && authPass.pass !== undefined) {
    // 兼容传入的不是布尔值
    const _pass = authPass.pass;
    if (!_pass && customRole) {
      // 匹配自定义角色
      authPass = authPass.role.includes(customRole);
    } else {
      authPass = _pass;
    }
  }
  if (!authPass) {
    if (disableMode && React.Children.count(children) === 1) {
      // 如果有placement和title属性，认为是Tooltip或Popconfirm或Popover，取出内部节点
      const removeWrapper = children.props.placement && children.props.title;
      const _children = removeWrapper ? children.props.children : children;

      return (
        <Tooltip title={noAuthTip} {...tipProps}>
          {React.cloneElement(_children, {
            ...rest,
            disabled: true,
            className: classnames(_children.props.className, 'disabled not-allowed'),
            onClick: (e: any) => e && e.stopPropagation && e.stopPropagation(),
          })}
        </Tooltip>
      );
    }
    return null;
  }

  const OwnOnClick = get(children, 'props.onClick');
  const reOnClick = rest.onClick;
  const onClick = (...arg: any) => {
    OwnOnClick && OwnOnClick(...arg);
    reOnClick && reOnClick(...arg);
  };
  return React.Children.count(children) === 1
    ? React.cloneElement(children, { ...rest, ...children.props, onClick })
    : children;
};

export const usePerm = permStore.useStore;
