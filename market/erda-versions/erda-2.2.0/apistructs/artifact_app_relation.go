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

type AppPublishItemRelation struct {
	Env             string   `json:"env"`
	OrgID           int64    `json:"-"`
	AppID           int64    `json:"appId"`
	PublishItemID   int64    `json:"publishItemId"`
	PublishItemName string   `json:"publishItemName"`
	PublisherID     int64    `json:"publisherId"`
	PublisherName   string   `json:"publisherName"`
	PublishItemNs   []string `json:"publishItemNs"` // 同步 nexus 配置至 pipeline cm
	AK              string   `json:"ak"`
	AI              string   `json:"ai"`
}

type QueryAppPublishItemRelationGroupByENVResponse struct {
	Header
	Data map[string]AppPublishItemRelation `json:"data"`
}

type QueryAppPublishItemRelationRequest struct {
	AppID         int64  `query:"appID"`
	PublishItemID int64  `query:"publishItemID"`
	AK            string `query:"ak"`
	AI            string `query:"ai"`
}

type QueryAppPublishItemRelationResponse struct {
	Header
	Data []AppPublishItemRelation
}

type UpdateAppPublishItemRelationRequest struct {
	AppID         int64                         `json:"-"`
	UserID        string                        `json:"-"`
	ProdItemID    int64                         `json:"PROD"`
	STAGINGItemID int64                         `json:"STAGING"`
	TESTItemID    int64                         `json:"TEST"`
	DEVItemID     int64                         `json:"DEV"`
	AKAIMap       map[DiceWorkspace]MonitorKeys `json:"-"` // 0是AK，1是AI
}

type MonitorKeys struct {
	AK    string `json:"ak"`
	AI    string `json:"ai"`
	Env   string `json:"env"`
	AppID int64  `json:"appId"`
}

// GetPublishItemIDByWorkspace 根据环境获取对应的 publishItem ID
func (u *UpdateAppPublishItemRelationRequest) GetPublishItemIDByWorkspace(workspace DiceWorkspace) int64 {
	switch workspace {
	case ProdWorkspace:
		return u.ProdItemID
	case StagingWorkspace:
		return u.STAGINGItemID
	case TestWorkspace:
		return u.TESTItemID
	case DevWorkspace:
		return u.DEVItemID
	default:
		return 0
	}
}

// SetPublishItemIDTo0ByWorkspace 根据环境获取对应的 设置publishItem ID 为 0
func (u *UpdateAppPublishItemRelationRequest) SetPublishItemIDTo0ByWorkspace(workspace DiceWorkspace) {
	switch workspace {
	case ProdWorkspace:
		u.ProdItemID = 0
	case StagingWorkspace:
		u.STAGINGItemID = 0
	case TestWorkspace:
		u.TESTItemID = 0
	case DevWorkspace:
		u.DEVItemID = 0
	}
}

type UpdateAppPublishItemRelationResponse struct {
	Header
}

type RemoveAppPublishItemRelationsRequest struct {
	PublishItemId int64 `json:"publishItemId"`
}

type RemoveAppPublishItemRelationsResponse struct {
	Header
}
