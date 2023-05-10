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

package apistructs

import "github.com/erda-project/erda-proto-go/core/dicehub/release/pb"

// AppWorkspaceReleasesGetRequest 查询应用某个环境所有可部署的 release 请求
type AppWorkspaceReleasesGetRequest struct {
	AppID     uint64        `schema:"appID,required"`
	Workspace DiceWorkspace `schema:"workspace,required"`
}

// AppWorkspaceReleasesGetResponse 查询应用某个环境所有可部署的 release 响应
type AppWorkspaceReleasesGetResponse struct {
	Header
	Data AppWorkspaceReleasesGetResponseData `json:"data,omitempty"`
}

// AppWorkspaceReleasesGetResponseData map key: branch, map value: paging releases
type AppWorkspaceReleasesGetResponseData map[string]*pb.ReleaseListResponseData
