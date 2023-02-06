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

import { goTo } from 'common/utils';
import React from 'react';
import { getSplitPathBy, getInfoFromRefName } from '../util';
import repoStore from 'application/stores/repo';
import appStore from 'application/stores/application';

import './repo-breadcrumb.scss';

interface IProps {
  path: string;
  children?: React.ReactNode;
  splitKey?: string;
}

const PureRepoBreadcrumb = ({ path, children, splitKey = 'tree' }: IProps) => {
  const list = [];
  const appDetail = appStore.useStore((s) => s.detail);
  const [info, mode] = repoStore.useStore((s) => [s.info, s.mode]);
  const basePath = getSplitPathBy(splitKey).before;
  const { branch, tag } = getInfoFromRefName(info.refName);
  const curBranch = branch || tag || info.defaultBranch;
  if (appDetail.name) {
    list.push({
      text: appDetail.name,
      href: `${basePath}/${curBranch}`,
    });
  }
  if (path) {
    const paths = path.split('/');

    let prevPath = `${basePath}/${curBranch}`;
    paths.forEach((p) => {
      prevPath = `${prevPath}/${p}`;
      list.push({
        text: p,
        href: prevPath,
      });
    });
  }

  const isEditing = mode.addFile || mode.editFile;
  return (
    <div className="repo-breadcrumb-container">
      <ul className="repo-breadcrumb">
        {list.map((item) =>
          isEditing ? (
            <li key={item.text + item.href} className="disable">
              {item.text}
            </li>
          ) : (
            <li key={item.text + item.href}>
              <span onClick={() => goTo(item.href)}>{item.text}</span>
            </li>
          ),
        )}
      </ul>
      {children}
    </div>
  );
};

export { PureRepoBreadcrumb as RepoBreadcrumb };
