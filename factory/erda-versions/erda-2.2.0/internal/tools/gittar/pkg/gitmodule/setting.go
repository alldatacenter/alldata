// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package gitmodule

var Setting = struct {
	MaxGitDiffLines          int
	MaxGitDiffLineCharacters int
	MaxGitDiffFiles          int
	MaxGitDiffSize           int
	ContextLineCount         int
	RepoStatsCache           Cache
	RepoSubmoduleCache       Cache
	PathCommitCache          Cache
	CommitCache              Cache
	ObjectSizeCache          Cache
}{
	MaxGitDiffLines:          200,
	MaxGitDiffLineCharacters: 1500, //diff line最大长度
	MaxGitDiffFiles:          100,
	MaxGitDiffSize:           5000, //大于指定大小就当二进制文件不做diff处理,单位Byte,单文件diff不做限制
	ContextLineCount:         3,
	RepoStatsCache:           NewMemCache(1000, "Repo-"),
	RepoSubmoduleCache:       NewMemCache(1000, "Repo-Submodule-"),
	PathCommitCache:          NewMemCache(5000, "path_commit_"),
	CommitCache:              NewMemCache(5000, "commit_"),
	ObjectSizeCache:          NewMemCache(10000, "object_size_"),
}
