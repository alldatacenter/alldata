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

package api

type CreateTagRequest struct {
	Name    string `json:"name"`
	Message string `json:"message"`
	Ref     string `json:"ref"`
}

type CreateBranchRequest struct {
	Name string `json:"name"`
	Ref  string `json:"ref"`
}

type CreateRepoRequest struct {
	OrgID       int64             `json:"org_id"`
	ProjectID   int64             `json:"project_id"`
	AppID       int64             `json:"app_id"`
	OrgName     string            `json:"org_name"`
	ProjectName string            `json:"project_name"`
	AppName     string            `json:"app_name"`
	HostMode    string            `json:"host_mode"` //selfhost|external
	Config      map[string]string `json:"config"`
}

type CreateRepoResponseData struct {
	ID int64 `json:"id"`

	// 仓库相对路径
	RepoPath string `json:"repo_path"`
}

type MergeTemplatesResponseData struct {
	Branch string   `json:"branch"`
	Path   string   `json:"path"`
	Names  []string `json:"names"`
}

// 分页查询
type PagingRequest struct {
	// +optional default 1
	PageNo int `json:"PageNo"`
	// +optional default 10
	PageSize int `json:"PageSize"`
}
