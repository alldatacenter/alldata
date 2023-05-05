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
import { usePerm, WithAuth } from 'user/common';
import { IWithAuth } from 'user/common/with-auth';
import userStore from 'user/stores';
import { get } from 'lodash';
import classNames from 'classnames';
import { projectPerm as ProjectPerm } from 'user/stores/_perm-project';
import { appPerm as AppPerm } from 'user/stores/_perm-app';

type IApiManage = typeof ProjectPerm.apiManage | typeof AppPerm.apiManage;
type IScope = Omit<IApiManage, 'name'>;
type IPath = ValueOf<
  { [k in keyof IScope]: [k, keyof Omit<IScope[k], 'name'>] },
  keyof { [k in keyof IScope]: [k, keyof Omit<IScope[k], 'name'>] }
>;

interface IProps extends IWithAuth {
  userID: string;
  children: JSX.Element;
  path: IPath;
  wrap?: boolean;
}

const isCreator = (creatorID: string) => {
  const currentLoginUser = userStore.getState((s) => s.loginUser.id);
  return creatorID === currentLoginUser;
};

const UnityAuthWrap = ({ userID, children, path, wrap = true, className, ...props }: IProps) => {
  const pathStr = path.join('.');
  const [projectPerm, appPerm, canEditAsOrgManage] = usePerm((s) => [
    s.project.apiManage,
    s.app.apiManage,
    s.org.apiAssetEdit.pass,
  ]);
  const hasAuth =
    canEditAsOrgManage ||
    isCreator(userID) ||
    get(projectPerm, `${pathStr}.pass`, false) ||
    get(appPerm, `${pathStr}.pass`, false);
  if (!wrap) {
    return hasAuth
      ? React.cloneElement(children, {
          className: classNames(children.props.className, className),
        })
      : null;
  }
  return (
    <WithAuth pass={hasAuth} {...props}>
      {children}
    </WithAuth>
  );
};

const hasAuth = (userID: string, path: IPath) => {
  const pathStr = path.join('.');
  const [projectPerm, appPerm] = permStore.getState((s) => [s.project.apiManage, s.app.apiManage]);
  return isCreator(userID) || get(projectPerm, `${pathStr}.pass`, false) || get(appPerm, `${pathStr}.pass`, false);
};

export { UnityAuthWrap, isCreator, hasAuth };
