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

import { Link } from 'react-router-dom';
import React from 'react';

// 替换 repo/xxx/... -> repo/replace/commitHash
export const mergeRepoPathWith = (afterPath: string) => {
  const [before] = window.location.pathname.split('repo');
  const link = [before, afterPath].join('repo');
  return link;
};

export const renderAsLink = (
  replace: string,
  commitHash: string,
  text: string | React.ReactElement,
  className = '',
) => {
  const link = mergeRepoPathWith(`/${replace}/${commitHash}`);
  return (
    <Link className={`as-link ${className}`} to={link} onClick={(e) => e.stopPropagation()} title={text}>
      {text}
    </Link>
  );
};

// 例如：tree -> /projects/5/apps/3/repo/tree/feature/jf-build/addon-common
// before: "/projects/5/apps/3/repo/tree"
// after: "/feature/jf-build/addon-common"
export const getSplitPathBy = (key: string) => {
  const [before, ...after] = window.location.pathname.split(key);
  return {
    before: before.endsWith('/') ? `${before}${key}` : `${before}/${key}`,
    after: after.join(key),
  };
};

export const getInfoFromRefName = (refName: string) => {
  const branchPrefix = 'refs/heads/';
  const commitPrefix = 'sha/';
  const tagPrefix = 'refs/tags/';
  let branch = '';
  let commitId = null;
  let tag = null;
  if (refName) {
    refName.startsWith(branchPrefix) && (branch = refName.slice(branchPrefix.length));
    refName.startsWith(commitPrefix) && (commitId = refName.slice(commitPrefix.length));
    refName.startsWith(tagPrefix) && (tag = refName.slice(tagPrefix.length));
  }
  return { branch, commitId, tag };
};
