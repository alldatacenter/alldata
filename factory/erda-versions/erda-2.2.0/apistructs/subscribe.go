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
	"fmt"
	"time"
)

type Subscribe struct {
	ID        string     `json:"id"`
	Type      string     `json:"type"`
	TypeID    uint64     `json:"typeID"`
	Name      string     `json:"name"`
	UserID    string     `json:"userID"`
	OrgID     uint64     `json:"orgID"`
	CreatedAt *time.Time `json:"createdAt"`
	UpdateAt  *time.Time `json:"updatedAt"`
}

type SubscribeType string

const (
	// ProjectSubscribe project subscribe type
	ProjectSubscribe SubscribeType = "project"

	// AppSubscribe app subscribe type
	AppSubscribe SubscribeType = "app"
)

func (s SubscribeType) String() string {
	return string(s)
}

func (s SubscribeType) IsEmpty() bool {
	return string(s) == ""
}

type CreateSubscribeReq struct {
	Type   SubscribeType `json:"type"`
	TypeID uint64        `json:"typeID"`
	Name   string        `json:"name"`
	UserID string        `json:"userID"`
	OrgID  uint64        `json:"orgID"`
}

type CreateSubscribeRsp struct {
	Header
	Data string `json:"data"`
}

func (c CreateSubscribeReq) Validate() error {
	if c.Type.IsEmpty() {
		return fmt.Errorf("empty type")
	}
	if c.TypeID == 0 {
		return fmt.Errorf("invalid typeID: %v", c.TypeID)
	}
	if c.Name == "" {
		return fmt.Errorf("empty name")
	}
	return nil
}

type UnSubscribeReq struct {
	ID     string        `json:"id"`
	Type   SubscribeType `json:"type"`
	TypeID uint64        `json:"typeID"`
	UserID string        `json:"userID"`
	OrgID  uint64        `json:"orgID"`
}

type GetSubscribeReq struct {
	Type SubscribeType `json:"type"`
	// optional
	TypeID uint64 `json:"typeID"`
	UserID string `json:"userID"`
	OrgID  uint64 `json:"orgID"`
}

func (c GetSubscribeReq) Validate() error {
	if c.Type.IsEmpty() {
		return fmt.Errorf("empty type")
	}
	return nil
}

type SubscribeDTO struct {
	Total int         `json:"total"`
	List  []Subscribe `json:"list"`
}

type GetSubscribesResponse struct {
	Header
	Data SubscribeDTO `json:"data"`
}
