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

// ErrorResponse 统一的 response 的 err 部分
type ErrorResponse struct {
	Code string      `json:"code"`
	Msg  string      `json:"msg"`
	Ctx  interface{} `json:"ctx"`
}

// Header 统一的 response 的除了接口数据的 header 部分
type Header struct {
	Success bool          `json:"success" `
	Error   ErrorResponse `json:"err"`
}

// RequestHeader 统一的 request
type RequestHeader struct {
	Locale string
}

// UserInfoHeader 由 openAPI 统一注入在 response 中
type UserInfoHeader struct {
	UserIDs  []string            `json:"userIDs"`
	UserInfo map[string]UserInfo `json:"userInfo"`
}

// EventHeader event 公共 header
type EventHeader struct {
	Event         string `json:"event"`
	Action        string `json:"action"`
	OrgID         string `json:"orgID"`
	ProjectID     string `json:"projectID"`
	ApplicationID string `json:"applicationID"`
	UserID        string `json:"userID"`
	Env           string `json:"env"`
	// Content   PlaceHolder `json:"content"`
	TimeStamp string `json:"timestamp"`
}
