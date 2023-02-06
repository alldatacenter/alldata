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
import BranchCompare from './components/branch-compare';
import RepoCompareDetail from './components/compare-detail';
import { Spin } from 'antd';
import { useLoading } from 'core/stores/loading';
import repoStore from 'application/stores/repo';

const BranchCompareDetail = () => {
  const [isFetching] = useLoading(repoStore, ['getCommitDetail']);
  return (
    <div className="branch-cp-detail">
      <BranchCompare />
      <div className="compare-diff mt-5">
        <Spin spinning={isFetching}>
          <RepoCompareDetail hideComment disableComment />
        </Spin>
      </div>
    </div>
  );
};

export { BranchCompareDetail };
