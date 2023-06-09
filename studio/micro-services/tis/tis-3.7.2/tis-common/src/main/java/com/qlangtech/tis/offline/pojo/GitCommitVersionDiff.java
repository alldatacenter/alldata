/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.offline.pojo;

import java.util.List;

/**
 * git仓库两个版本的diff
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class GitCommitVersionDiff {

    private GitRepositoryCommitPojo commit;

    private List<GitRepositoryCommitPojo> commits;

    private List<GitFileDiff> diffs;

    private boolean compareTimeout;

    private boolean compareSameRef;

    public GitCommitVersionDiff() {
    }

    public GitRepositoryCommitPojo getCommit() {
        return commit;
    }

    public void setCommit(GitRepositoryCommitPojo commit) {
        this.commit = commit;
    }

    public List<GitRepositoryCommitPojo> getCommits() {
        return commits;
    }

    public void setCommits(List<GitRepositoryCommitPojo> commits) {
        this.commits = commits;
    }

    public List<GitFileDiff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<GitFileDiff> diffs) {
        this.diffs = diffs;
    }

    public boolean isCompareTimeout() {
        return compareTimeout;
    }

    public void setCompareTimeout(boolean compareTimeout) {
        this.compareTimeout = compareTimeout;
    }

    public boolean isCompareSameRef() {
        return compareSameRef;
    }

    public void setCompareSameRef(boolean compareSameRef) {
        this.compareSameRef = compareSameRef;
    }
}
