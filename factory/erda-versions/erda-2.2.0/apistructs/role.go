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

// /api/roles/<role>
// method: put
// 更改用户角色
type RoleChangeRequest struct {
	// 用户角色
	Role string         `json:"-" path:"role"`
	Body RoleChangeBody `json:"body"`
}

type RoleChangeBody struct {
	// 目标id, 对应的applicationId, projectId, orgId
	TargetId string `json:"targetId"`

	// 目标类型 APPLICATION,PROJECT,ORG
	TargetType string `json:"targetType"`
	UserId     string `json:"userId"`
}

// /api/roles/<role>
// method: put
type RoleChangeResponse struct {
	Header
	Data bool `json:"data"`
}
