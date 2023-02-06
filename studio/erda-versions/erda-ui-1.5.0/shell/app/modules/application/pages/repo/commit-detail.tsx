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

import { Spin } from 'antd';
import { groupBy, map } from 'lodash';
import React from 'react';
import { CommitBlock } from './common';
import FileDiff from './components/file-diff';

import './repo-commit.scss';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';

const CommitDetail = () => {
  const [commitDetail, sonarMessage] = repoStore.useStore((s) => [s.commitDetail, s.sonarMessage]);
  const { getCommitDetail } = repoStore.effects;
  const { clearCommitDetail, clearSonarMessage } = repoStore.reducers;
  const [isFetching] = useLoading(repoStore, ['getCommitDetail']);
  React.useEffect(() => {
    getCommitDetail();
    return () => {
      clearSonarMessage();
      clearCommitDetail();
    };
  }, [clearSonarMessage, getCommitDetail, clearCommitDetail]);
  const { commit, diff = {} } = commitDetail;
  const fileMap = {};
  if (diff && diff.files) {
    diff.files.forEach((f: REPOSITORY.IFile) => {
      fileMap[f.name] = f;
    });
    if (sonarMessage.issues) {
      const issueGroup = groupBy(sonarMessage.issues, 'path');
      map(issueGroup, (issues, path) => {
        if (fileMap[path]) {
          fileMap[path].issues = issues;
        }
      });
    }
  }
  return (
    <Spin spinning={isFetching}>
      <CommitBlock commit={commit} />
      <FileDiff diff={diff} disableComment mode="commit" />
    </Spin>
  );
};
export default CommitDetail;
