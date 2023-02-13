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

package model

import (
	"fmt"
	"strings"
	"time"
	"unicode/utf8"
)

//the complete template，ID just like addon_elasticsearch_cpu
type Model struct {
	ID        string      `json:"id" yaml:"id"` //id is not the database generated auto id
	Metadata  Metadata    `json:"metadata" yaml:"metadata"`
	Behavior  Behavior    `json:"behavior" yaml:"behavior"`
	Templates []Templates `json:"templates" yaml:"templates"`
}
type Metadata struct {
	Name   string   `json:"name" yaml:"name"`
	Type   string   `json:"type" yaml:"type"`
	Module string   `json:"module" yaml:"module"`
	Scope  []string `json:"scope" yaml:"scope"`
}
type Behavior struct {
	Group string `json:"group" yaml:"group"`
}
type Render struct {
	Formats  map[string]string `json:"formats" yaml:"formats"`
	Title    string            `json:"title" yaml:"title"`
	Template string            `json:"template" yaml:"template"`
}

type Templates struct {
	Trigger []string `json:"trigger" yaml:"trigger"`
	Targets []string `json:"targets" yaml:"targets"`
	I18n    []string `json:"i18n" yaml:"i18n"`
	Render  `json:"render" yaml:"render"`
}

type Target struct {
	GroupID     int64    `json:"group_id"`
	Channels    []string `json:"channels"`
	DingDingUrl string   `json:"dingdingUrl"`
}

type GetNotifyRes struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type CreateNotifyReq struct {
	ScopeID       string            `query:scopeId validate:"required"`
	Scope         string            `query:"scope" validate:"required"`
	TemplateID    []string          `query:"templateId" validate:"required"`
	NotifyName    string            `query:"notifyName" validate:"required"`
	NotifyGroupID int64             `query:"notifyGroupId" validate:"required"`
	Channels      []string          `query:"channels" validate:"required"`
	Attribute     map[string]string `query:"attribute"`
}

type GroupInfo struct {
	Id         int64  `json:"id"`
	TargetData string `json:"targetData"`
}

type NotifyTarget struct {
	Type   string        `json:"type"`
	Values []TargetValue `json:"values"`
}

type TargetValue struct {
	Receiver string `json:"receiver"`
	// only dingding used
	Secret string `json:"secret"`
}

type UpdateNotifyReq struct {
	ID            int64             `param:"id" validate:"required"`
	Scope         string            `query:"scope" validate:"required"`
	ScopeID       string            `query:"scopeId" validate:"required"`
	Channels      []string          `query:"channels" validate:"required"`
	NotifyGroupID int64             `query:"notifyGroupId" validate:"required"`
	TemplateId    []string          `query:"templateId" validate:"required"`
	Attribute     map[string]string `query:"attribute"`
}

type CreateUserDefineNotifyTemplate struct {
	Name     string              `query:"name" validate:"required"`
	Group    string              `query:"group" validate:"required"`
	Trigger  []string            `query:"trigger" validate:"required"`
	Formats  []map[string]string `query:"formats" validate:"required"`
	Title    []string            `query:"title" validate:"required"`
	Template []string            `query:"template" validate:"required"`
	Scope    string              `query:"scope" validate:"required"`
	ScopeID  string              `query:"scopeId" validate:"required"`
	Targets  []string            `query:"targets" validate:"required"`
}

type QueryNotifyListReq struct {
	Scope   string
	ScopeID string
}

type QueryNotifyListRes struct {
	List []NotifyRes
}
type NotifyRes struct {
	CreatedAt    time.Time      `json:"createdAt"`
	Id           int64          `json:"id"`
	NotifyID     string         `json:"notifyId"`
	NotifyName   string         `json:"notifyName"`
	Target       string         `json:"target"`
	NotifyTarget []NotifyTarget `json:"groupInfo"`
	Enable       bool           `json:"enable"`
	Items        []string       `json:"items"`
}

type NotifyDetailResponse struct {
	Id         int64  `json:"id"`
	NotifyID   string `json:"notifyId"`
	NotifyName string `json:"notifyName"`
	Target     string `json:"target"`
	GroupType  string `json:"groupType"`
}

type GetAllGroupData struct {
	Name  string `json:"name"`
	Value int64  `json:"value"`
	Type  string `json:"type"`
}

type NotifyRecord struct {
	NotifyId    string `json:"notify_id" gorm:"column:notify_id"`
	NotifyName  string `json:"notify_name" gorm:"column:notify_name"`
	ScopeType   string `json:"scope_type" gorm:"column:scope_type"`
	ScopeId     int64  `json:"scope_id" gorm:"column:scope_id"`
	GroupId     string `json:"group_id" gorm:"column:group_id"`
	NotifyGroup string `json:"notify_group" gorm:"column:notify_group"`
	Title       string `json:"title" gorm:"column:title"`
	NotifyTime  int64  `json:"notify_time" gorm:"column:notify_time"`
	CreateTime  int64  `json:"create_time" gorm:"column:create_time"`
	UpdateTime  int64  `json:"update_time" gorm:"column:update_time"`
}

func ValidateString(field string) string {
	if len(field) == 0 {
		return FieldEmpty
	}
	if len(strings.TrimSpace(field)) <= 0 {
		return SpaceField
	}
	if utf8.RuneCountInString(field) > RuneCount {
		return FieldTooLong
	}
	return ""
}
func ValidateInt(field int64) string {
	if field == 0 {
		return FiledZero
	}
	return ""
}

func (c CreateNotifyReq) CheckNotify() error {
	str := ValidateInt(c.NotifyGroupID)
	if str != "" {
		return fmt.Errorf("create notify notifyGroupID " + str)
	}
	str = ValidateString(c.NotifyName)
	if str != "" {
		return fmt.Errorf("create notify notifyName " + str)
	}
	return nil
}

func (c CreateUserDefineNotifyTemplate) CheckCustomizeNotify() error {
	str := ValidateString(c.Name)
	if str != "" {
		return fmt.Errorf("create customize notify template name " + str)
	}
	str = ValidateString(c.Group)
	if str != "" {
		return fmt.Errorf("create customize notify template group " + str)
	}
	for i := range c.Trigger {
		str = ValidateString(c.Trigger[i])
		if str != "" {
			return fmt.Errorf("create customize notify template trigger" + str)
		}
		str = ValidateString(c.Targets[i])
		if str != "" {
			return fmt.Errorf("create customize notify template trigger" + str)
		}
	}
	err := CheckElements(c.Formats, c.Title, c.Template)
	if err != nil {
		return err
	}
	return nil
}

func CheckElements(formats []map[string]string, title, template []string) error {
	lFormats, lTitle, lTemplate := len(formats), len(title), len(template)
	if lFormats == 0 || lTitle == 0 || lTemplate == 0 {
		return fmt.Errorf("the len of Formats is zero or the len " +
			"of Title is zero or the len of Templates is zero")
	}
	if lFormats != lTitle || lFormats != lTemplate || lTitle != lTemplate {
		return fmt.Errorf("the Formats and Title and Template num is not equal")
	}
	for i := range formats {
		str := ValidateString(title[i])
		if str != "" {
			return fmt.Errorf("create customize notify template Title " + str)
		}
		str = ValidateString(template[i])
		if str != "" {
			return fmt.Errorf("create customize notify template Template " + str)
		}
	}
	return nil
}

func (c UpdateNotifyReq) CheckNotify() error {
	str := ValidateInt(c.ID)
	if str != "" {
		return fmt.Errorf("update notify id " + str)
	}
	str = ValidateInt(c.NotifyGroupID)
	if str != "" {
		return fmt.Errorf("update notify notifyGroupId " + str)
	}
	return nil
}
