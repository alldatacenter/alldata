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
import BuildDetail from './build-detail';
import fileTreeStore from 'common/stores/file-tree';
import { EmptyHolder } from 'common';
import { getBranchPath } from 'application/pages/pipeline/config';
import routeInfoStore from 'core/stores/route';
import appStore from 'application/stores/application';

interface IProps {
  deployAuth: { hasAuth: boolean; authTip?: string };
  isMobileInit?: boolean;
}

const RunDetail = (props: IProps) => {
  const [caseDetail] = fileTreeStore.useStore((s) => [s.curNodeDetail]);
  const appDetail = appStore.useStore((s) => s.detail);
  const fileName = caseDetail.name;
  const { isMobileInit } = props;
  const { appId } = routeInfoStore.useStore((s) => s.params);
  let branch;
  let pagingYmlNames;
  if (isMobileInit) {
    pagingYmlNames = appDetail.projectName ? [`${appDetail.projectName}_${appDetail.name}_pipeline.yml`] : undefined;
  } else {
    const res = getBranchPath(caseDetail, appId);
    branch = res.branch;
    pagingYmlNames = res.pagingYmlNames;
  }

  return (isMobileInit ? pagingYmlNames : branch) ? (
    <BuildDetail
      {...props}
      ymlName={fileName}
      source={'dice'}
      branch={branch as string}
      pagingYmlNames={pagingYmlNames}
    />
  ) : (
    <EmptyHolder relative />
  );
};

export default RunDetail;
