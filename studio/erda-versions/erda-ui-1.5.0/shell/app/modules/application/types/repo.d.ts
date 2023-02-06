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

declare namespace REPOSITORY {
  interface ITree {
    path: string;
    type: string;
    refName: string;
    readmeFile: string;
    commit: ICommit;
    entry: {
      name: string;
    };
    entries: Array<{
      id: string;
      name: string;
      size: number;
      type: 'blob' | 'tree';
      commit: ICommit;
    }>;
  }

  interface GetInfo {
    repoPrefix: string;
    branch: string;
  }

  interface GetFromRepo {
    path?: string;
    comment?: boolean;
    repoPrefix?: string;
    type: 'blame' | 'raw' | 'blob' | 'tree';
  }

  interface IInfo {
    [key: string]: any;
    refName: string;
    defaultBranch: string;
    tags: string[];
    branches: string[];
    username: string;
  }

  interface IMode {
    addFile: boolean;
    editFile: boolean;
    fileBlame: boolean;
  }

  interface IComment {
    id: number;
    type: 'diff_note' | 'diff_note_reply' | 'normal';
    discussionId: string;
    oldCommitId: string;
    newCommitId: string;
    note: string;
    data: {
      diffLines: any[];
      oldPath: string;
      newPath: string;
      oldLine: number;
      newLine: number;
      oldCommitId: string;
      newCommitId: string;
    };
    authorId: string;
    author: {
      [prop: string]: any;
      email: string;
      id: number;
      nickName: string;
      username: string;
    };
    createdAt: string;
    updatedAt: string;
  }

  interface IBlob {
    binary: boolean;
    content: string;
    path: string;
    refName: string;
  }

  interface IBlame {
    commit: ICommit;
    startLineNo: number;
    endLineNo: number;
  }

  interface ISection {
    lines: Array<{
      content: string;
      oldLineNo: number;
      newLineNo: number;
      type: string;
    }>;
  }

  interface IFile {
    name: string;
    oldName: string;
    addition?: boolean;
    deletion?: boolean;
    type: 'add' | 'delete' | 'rename';
    sections: ISection[];
    issues: Array<{
      line: string;
    }>;
    isBin?: boolean;
    index: string;
  }

  interface ICommit {
    id: string;
    author: ICommitter;
    commitMessage: string;
    parents: string[];
    committer: ICommitter;
  }

  interface ICommitter {
    email: string;
    name: string;
    when: string;
  }

  interface IMrDetail {
    [prop: string]: any;
  }

  interface MRItem {
    id: number;
    mergeId: number;
    appId: number;
    repoId: number;
    title: string;
    authorId: string;
    description: string;
    assigneeId: string;
    mergeUserId: string;
    closeUserId: string;
    sourceBranch: string;
    targetBranch: string;
    sourceSha: string;
    targetSha: string;
    removeSourceBranch: true;
    state: string;
    defaultCommitMessage: string;
    createdAt: string;
    updatedAt: string;
    closeAt: string;
    mergeAt: string;
  }

  interface IBranch {
    id: string;
    name: string;
    commit: ICommit;
    isDefault: boolean;
    isProtect: boolean;
    isMerged: boolean;
  }

  interface CreateBranch {
    repoPrefix: string;
    branch: string;
    refValue: string;
  }

  interface CreateTag {
    repoPrefix: string;
    tag: string;
    ref?: string;
    message?: string;
  }

  interface IMode {
    addFile: boolean;
    editFile: boolean;
    fileBlame: boolean;
  }

  interface ITemplateConfig {
    names: any[];
    branch: string;
    path: string;
  }

  interface ICommitPaging {
    pageNo: number;
    pageSize: number;
    hasMore?: boolean;
  }

  interface IState {
    info: IInfo;
    tree: ITree;
    blob: IBlob;
    blame: IBlame[];
    branch: any[];
    tag: any[];
    commit: any[];
    mr: Obj;
    mrList: any[];
    mrStats: Obj;
    mrDetail: Obj;
    comments: any[];
    commitDetail: Obj;
    compareDetail: Obj;
    sonarMessage: Obj;
    commitPaging: REPOSITORY.ICommitPaging;
    mode: REPOSITORY.IMode;
    buildId: string;
    groupedAddonList: any[];
    templateConfig: REPOSITORY.ITemplateConfig;
    pipelineYmlStructure: IPipelineYmlStructure;
    mrPaging: any;
  }

  interface QueryCompareDetail {
    compareA: string;
    compareB: string;
  }

  interface Mr {
    [key: string]: any;
    action: string;
    sourceBranch: string;
    targetBranch: string;
    removeSourceBranch: boolean;
  }

  interface CommitAction {
    action: 'add' | 'update' | 'delete';
    content?: string;
    path: string;
    pathType: string;
  }

  interface Commit {
    message: string;
    branch: string;
    actions: CommitAction[];
  }

  type MrStats = Pick<Mr, 'sourceBranch' | 'targetBranch'>;

  type MrType = 'all' | 'open' | 'merged' | 'closed';

  interface QueryRepoTree {
    force: boolean;
  }

  interface QueryMrs {
    state: REPOSITORY.MrType;
    assigneeId?: number;
    authorId?: number;
    pageNo?: number;
  }

  interface QueryBuildId {
    commitId: string;
    branch: string;
    appId: string;
  }

  interface QueryBlobRange {
    repoPrefix: string;
    commitId: string;
    path: string;
    since: number;
    to: number;
    bottom: string;
    offset: number;
    unfold: boolean;
    type: string;
    sectionIndex: number;
  }

  interface IBlobRange {
    binary: boolean;
    refName: string;
    path: string;
    lines: Array<{
      oldLineNo: number;
      newLineNo: number;
      type: string;
      content: string;
    }>;
  }

  interface QueryCommit {
    search?: string;
    repoPrefix?: string;
    pageNo?: number;
    pageSize?: number;
    branch?: string;
  }

  interface ITag {
    name: string;
    id: string;
    object: string;
    tagger: {
      email: string;
      name: string;
      when: string;
    };
    message: string;
  }

  interface IMrState {
    hasConflict: boolean;
    isMerged: boolean;
    hasError: boolean;
    errorMsg: string;
  }

  type MROperation = 'edit' | 'merge' | 'close' | 'revert';

  interface OperateMR {
    commitMessage?: string;
    removeSourceBranch?: string;
    action: MROperation;
    mergeId: string;
  }

  interface CommitDetail {
    commit: ICommit;
    diff: {
      filesChanged: number;
      totalAddition: number;
      totalDeletion: number;
      files: IFile[];
      isFinish: string;
    };
  }

  interface QueryCompare {
    repoPrefix: string;
    compareA: string;
    compareB: string;
  }

  interface CompareDetail {
    commits: ICommit[];
    commitsCount: number;
    diff: {
      filesChanged: number;
      totalAddition: number;
      totalDeletion: number;
      files: IFile[];
      isFinish: string;
    };
    from: string;
    to: string;
  }

  interface MrNote {
    id: number;
    repoId: number;
    type: string;
    discussionId: string;
    oldCommitId: string;
    newCommitId: string;
    note: string;
    data: {
      diffLines: null;
      oldPath: string;
      newPath: string;
      oldLine: number;
      newLine: number;
      oldCommitId: string;
      newCommitId: string;
    };
    authorId: string;
    author: {
      email: string;
      id: number;
      nickName: string;
      username: string;
    };
    createdAt: string;
    updatedAt: string;
  }

  interface MrTemplate {
    branch: string;
    path: string;
    names: string[];
  }

  interface IPipelineTemplate {
    id: number;
    name: string;
    desc: string;
    logoUrl: string;
    scopeType: string;
    scopeID: string;
    createdAt: string;
    updatedAt: string;
    version: string;
    spec?: string;
  }

  interface IPipelineTemplateYml {
    pipelineYaml: string;
    pipelineTemplateVersion: string;
  }

  interface IPipelineTemplateQuery {
    scopeType: 'dice';
    scopeID: string;
  }

  interface IPipelineTemplateContentQuery extends IPipelineTemplateQuery {
    name: string;
    version: string;
  }

  interface BackupQuery {
    id: string;
    remark: string;
    uuid: string;
    name: string;
    size: string;
    url: string;
    type: string;
    from: string;
    commitId: string;
    creator: string;
    createdAt: string;
    updatedAt: string;
  }
  interface IBackupAppendBody {
    commitId: string;
    remark: string;
    branchRef: string;
  }

  interface IBackupUuid {
    uuid: string;
  }

  interface IBackupBranch {
    branchRef: string;
  }
}
