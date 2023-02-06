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

//go:generate go run ../pkg/structparser/comment/comment.go -pkg-name apistructs

// WebhookListResponse webhook 列表
// Path:         "/api/webhooks",
// BackendPath:  "/api/dice/eventbox/webhooks",
type WebhookListResponse struct {
	Header
	Data WebhookListResponseData `json:"data"`
}

// WebhookInspectResponse 获取 webhook 详情
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookInspectResponse struct {
	Header
	Data WebhookInspectResponseData `json:"data"`
}

// WebhookCreateResponse 创建 webhook
// Path:         "/api/webhooks",
// BackendPath:  "/api/dice/eventbox/webhooks",
type WebhookCreateResponse struct {
	Header
	Data WebhookCreateResponseData `json:"data"`
}

// WebhookUpdateResponse 更新 webhook
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookUpdateResponse struct {
	Header
	Data WebhookUpdateResponseData `json:"data"`
}

// WebhookPingResponse ping webhook, 发送 ping 事件
// Path:         "/api/webhooks/<id>/actions/ping",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>/actions/ping",
type WebhookPingResponse struct {
	Header
	Data WebhookPingResponseData `json:"data"`
}

// WebhookDeleteResponse 删除 webhook
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookDeleteResponse struct {
	Header
	Data WebhookDeleteResponseData `json:"data"`
}

// WebhookListEventsResponse webhook 事件列表
// Path:         "/api/webhook-events",
// BackendPath:  "/api/dice/eventbox/webhook_events",
type WebhookListEventsResponse struct {
	Header
	Data WebhookListEventsResponseData `json:"data"`
}

// WebhookListRequest webhook 列表
// Path:         "/api/webhooks",
// BackendPath:  "/api/dice/eventbox/webhooks",
type WebhookListRequest struct {
	// 列出 orgid & projectID & applicationID & env 下的 webhook
	OrgID string `query:"orgID"`

	// 列出 orgid & projectID & applicationID & env 下的 webhook
	ProjectID string `query:"projectID"`

	// 列出 orgid & projectID & applicationID & env 下的 webhook
	ApplicationID string `query:"applicationID"`

	// 列出 orgid & projectID & applicationID & env 下的 webhook, env格式：test,prod,dev
	Env string `query:"env"`
}

// WebhookListResponseData WebhookListResponse 的 Data
type WebhookListResponseData []Hook

// WebhookInspectRequest 获取 webhook 详情
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookInspectRequest struct {
	// 所查询的 webhook ID
	ID string `path:"id"`
}

// WebhookInspectResponseData WebhookInspectResponse 的 Data
type WebhookInspectResponseData Hook

// WebhookCreateRequest 创建 webhook
// Path:         "/api/webhooks",
// BackendPath:  "/api/dice/eventbox/webhooks",
type WebhookCreateRequest CreateHookRequest

// WebhookCreateResponseData WebhookCreateResponse 的 Data
type WebhookCreateResponseData string

// WebhookUpdateRequest 更新 webhook
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookUpdateRequest struct {
	// webhook ID
	ID   string                   `json:"-" path:"id"`
	Body WebhookUpdateRequestBody `json:"body"`
}

// WebhookUpdateRequestBody WebhookUpdateRequest
type WebhookUpdateRequestBody struct {
	// 全量更新这个 webhook 关心的 event 列表
	Events []string `json:"events"`

	// 从 webhook event 列表中删除
	RemoveEvents []string `json:"removeEvents"`

	// 从 webhook event 列表中增加
	AddEvents []string `json:"addEvents"`

	// 该 webhook 对应的 URL， 所关心事件触发后会POST到该URL
	URL string `json:"url"`

	// 是否激活，如果没有该参数，默认为false
	Active bool `json:"active"`
}

type WebhookDeleteRequestBody struct {
	OrgId         string `json:"orgId"`
	ProjectId     string `json:"projectId"`
	ApplicationId string `json:"applicationId"`
	Id            string `json:"id"`
}

// WebhookUpdateResponseData WebhookUpdateResponse 的 Data
type WebhookUpdateResponseData string

// WebhookPingRequest ping webhook, 发送 ping 事件
// Path:         "/api/webhooks/<id>/actions/ping",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>/actions/ping",
type WebhookPingRequest struct {
	// webhook ID
	ID string `path:"id"`
}

// WebhookPingResponseData WebhookPingResponse 的 Data
type WebhookPingResponseData string

// WebhookDeleteRequest 删除 webhook
// Path:         "/api/webhooks/<id>",
// BackendPath:  "/api/dice/eventbox/webhooks/<id>",
type WebhookDeleteRequest struct {
	// webhook ID
	ID string `path:"id"`
}

// WebhookDeleteResponseData WebhookDeleteResponse 的 Data
type WebhookDeleteResponseData string

// WebhookListEventsRequest webhook 事件列表
// Path:         "/api/webhook-events",
// BackendPath:  "/api/dice/eventbox/webhook_events",
type WebhookListEventsRequest struct{}

// WebhookListEventsResponseData WebhookListEventsResponse 的 Data
type WebhookListEventsResponseData []struct {
	// webhook key 名字，是实际起作用的名字
	Key string `json:"key"`

	// webhook 描述性 title
	Title string `json:"title"`

	// webhook 描述文本
	Desc string `json:"desc"`
}

// Hook 代表 webhook 的结构
type Hook struct {
	// webhook ID
	ID        string `json:"id"`
	UpdatedAt string `json:"updatedAt"`
	CreatedAt string `json:"createdAt"`

	// 用于计算后续发送的事件内容的sha值，目前没有用
	Secret string `json:"secret"`

	CreateHookRequest
}

// CreateHookRequest 内部使用的创建 webhook 的请求结构体
type CreateHookRequest struct {
	// webhook 名字
	Name string `json:"name"`
	// webhook 所关心事件的列表
	Events []string `json:"events"`
	// webhook URL, 后续的事件触发时，会POST到该URL
	URL string `json:"url"`
	// 是否激活
	Active bool `json:"active"`
	HookLocation
}

// HookLocation 代表 webhook 归属
type HookLocation struct {
	// webhook 所属 orgID
	Org string `json:"orgID"`

	// webhook 所属 projectID
	Project string `json:"projectID"`

	// webhook 所属 applicationID
	Application string `json:"applicationID"`

	// webhook 所关心环境, nil 代表所有
	Env []string `json:"env"`
}

type DeleteHookRequest struct {
	Name string `json:"name"`
	HookLocation
}
