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

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"net/url"
	"time"
)

type NotifyTargetType string
type NotifyLevel string

const (
	RoleNotifyTarget               NotifyTargetType = "role"
	UserNotifyTarget               NotifyTargetType = "user"
	ExternalUserNotifyTarget       NotifyTargetType = "external_user"
	DingdingNotifyTarget           NotifyTargetType = "dingding"
	DingdingWorkNoticeNotifyTarget NotifyTargetType = "dingding_worknotice"
	WebhookNotifyTarget            NotifyTargetType = "webhook"
)

var ValidateNotifyChannel = map[string]bool{
	"dingding": true,
	"sms":      true,
	"email":    true,
	"mbox":     true,
	"webhook":  true,
	"vms":      true,
}

// NotifyTarget 通知目标
type NotifyTarget struct {
	Type   NotifyTargetType `json:"type"`
	Values []Target         `json:"values"`
}

// OldNotifyTarget 兼容老版本通知组
type OldNotifyTarget struct {
	Type   NotifyTargetType `json:"type"`
	Values []string         `json:"values"`
}

// Target 目标详情
type Target struct {
	Receiver string `json:"receiver"`
	// 目前只有钉钉用
	Secret string `json:"secret"`
}

func (n *OldNotifyTarget) CovertToNewNotifyTarget() NotifyTarget {
	var values []Target
	for _, v := range n.Values {
		values = append(values, Target{
			Receiver: v,
			Secret:   "",
		})
	}
	return NotifyTarget{
		Type:   n.Type,
		Values: values,
	}
}

func (t *Target) GetSignURL() (string, error) {
	if t.Secret == "" {
		return t.Receiver, nil
	}

	tm := time.Now().UnixNano() / 1e6
	strToHash := fmt.Sprintf("%d\n%s", tm, t.Secret)
	hmac256 := hmac.New(sha256.New, []byte(t.Secret))
	hmac256.Write([]byte(strToHash))
	data := hmac256.Sum(nil)
	dataStr := base64.StdEncoding.EncodeToString(data)

	u, err := url.Parse(t.Receiver)
	if err != nil {
		return "", err
	}
	values := u.Query()
	values.Set("timestamp", fmt.Sprintf("%d", tm))
	values.Set("sign", dataStr)
	u.RawQuery = values.Encode()

	return u.String(), nil
}

type NotifyUser struct {
	ID       string `json:"id"`
	Username string `json:"username"`
	Email    string `json:"email"`
	Mobile   string `json:"mobile"`
	Type     string `json:"type"` // inner external
}

// NotifyGroup 通知组信息
type NotifyGroup struct {
	ID        int64          `json:"id"`
	Name      string         `json:"name"`
	ScopeType string         `json:"scopeType,omitempty"`
	ScopeID   string         `json:"scopeId,omitempty"`
	Targets   []NotifyTarget `json:"targets"`
	CreatedAt time.Time      `json:"createdAt"`
	Creator   string         `json:"creator"`
	Label     string         `json:"label"`
}

// NotifyGroupDetail 通知组详情信息
type NotifyGroupDetail struct {
	ID                     int64          `json:"id"`
	Name                   string         `json:"name"`
	ScopeType              string         `json:"scopeType"`
	ScopeID                string         `json:"scopeId"`
	Users                  []NotifyUser   `json:"users"`
	Targets                []NotifyTarget `json:"targets"`
	DingdingList           []Target       `json:"dingdingList"`
	DingdingWorkNoticeList []Target       `json:"dingdingWorknoticeList"`
	WebHookList            []string       `json:"webhookList"`
}

// CreateNotifyGroupRequest 创建通知组请求
type CreateNotifyGroupRequest struct {
	Name        string         `json:"name"`
	ScopeType   string         `json:"scopeType"`
	ScopeID     string         `json:"scopeId"`
	Targets     []NotifyTarget `json:"targets"`
	Creator     string         `json:"creator"`
	Label       string         `json:"label"`
	ClusterName string         `json:"clusterName"`
	AutoCreate  bool           `json:"-"`
	OrgID       int64          `json:"-"`
}

// CreateNotifyGroupResponse 创建通知组响应
type CreateNotifyGroupResponse struct {
	Header
	Data NotifyGroup `json:"data"`
}

// DeleteNotifyGroupResponse 删除通知组响应
type DeleteNotifyGroupResponse struct {
	Header
	Data NotifyGroup `json:"data"`
}

// UpdateNotifyGroupRequest 更新通知组请求
type UpdateNotifyGroupRequest struct {
	ID      int64          `json:"-"`
	Name    string         `json:"name"`
	Targets []NotifyTarget `json:"targets"`
	OrgID   int64          `json:"-"`
}

// UpdateNotifyGroupResponse 更新通知组响应
type UpdateNotifyGroupResponse struct {
	Header
	Data NotifyGroup `json:"data"`
}

// QueryNotifyGroupRequest 查询通知组列表请求
type QueryNotifyGroupRequest struct {
	PageNo      int64  `query:"pageNo"`
	PageSize    int64  `query:"pageSize"`
	ScopeType   string `query:"scopeType"`
	ScopeID     string `query:"scopeID"`
	Label       string `query:"label"`
	ClusterName string `query:"clusterName"`
	// 通知组名字
	Name string `query:"name"`
}

// QueryNotifyGroupResponse 查询通知组列表响应
type QueryNotifyGroupResponse struct {
	Header
	UserInfoHeader
	Data QueryNotifyGroupData `json:"data"`
}

// QueryNotifyGroupData 通知组列表数据结构
type QueryNotifyGroupData struct {
	List  []*NotifyGroup `json:"list"`
	Total int            `json:"total"`
}

// GetNotifyGroupResponse 查询通知组响应
type GetNotifyGroupResponse struct {
	Header
	UserInfoHeader
	Data NotifyGroup `json:"data"`
}

// GetNotifyGroupResponse 查询通知组详情响应
type GetNotifyGroupDetailResponse struct {
	Header
	Data NotifyGroupDetail `json:"data"`
}
