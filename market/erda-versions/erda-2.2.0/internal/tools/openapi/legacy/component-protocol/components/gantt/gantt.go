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

package gantt

import (
	"github.com/erda-project/erda/apistructs"
)

type CommonGantt struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      Props                                            `json:"props,omitempty"`
	State      State                                            `json:"state,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       Data                                             `json:"data,omitempty"`
}

type State struct {
	// set after render
	Total               uint64 `json:"total,omitempty"`
	PageNo              uint64 `json:"pageNo,omitempty"`
	PageSize            uint64 `json:"pageSize,omitempty"`
	IssueViewGroupValue string `json:"issueViewGroupValue,omitempty"`
	IssueType           string
}

type Props struct {
	Visible   bool         `json:"visible"`
	RowKey    string       `json:"rowKey,omitempty"`
	ClassName string       `json:"className,omitempty"`
	Columns   []PropColumn `json:"columns,omitempty"`
}

type PropColumn struct {
	Title           string           `json:"title,omitempty"`
	TitleRenderType string           `json:"titleRenderType,omitempty"`
	DataIndex       string           `json:"dataIndex,omitempty"`
	Width           uint64           `json:"width,omitempty"`
	Data            []PropColumnData `json:"data,omitempty"`
	TitleTip        []string         `json:"titleTip,omitempty"`
}

type PropColumnData struct {
	Month uint64   `json:"month,omitempty"`
	Date  []uint64 `json:"date,omitempty"`
}

var (
	OpChangePageNo apistructs.OperationKey = "changePageNo"
)

var Operations = map[apistructs.OperationKey]apistructs.Operation{
	OpChangePageNo: {Reload: true},
}

type Data struct {
	List []DataItem `json:"list,omitempty"`
}

type DataItem struct {
	// 此ID全局唯一: autoID + issueID
	ID        uint64    `json:"id,omitempty"`
	DateRange DateRange `json:"dateRange,omitempty"`
	Tasks     DataTask  `json:"issues,omitempty"`
	User      User      `json:"user,omitempty"`
}

type DateRange struct {
	RenderType RenderType       `json:"renderType,omitempty"`
	Value      []DateRangeValue `json:"value,omitempty"`
}

type DateRangeValue struct {
	Tooltip string `json:"tooltip"`
	// 单位天
	RestTime   float64 `json:"restTime"`
	Offset     float64 `json:"offset"`
	Delay      float64 `json:"delay"`
	ActualTime float64 `json:"actualTime"`
}

type DataTask struct {
	RenderType RenderType      `json:"renderType,omitempty"`
	Value      []DataTaskValue `json:"value,omitempty"`
}

type DataTaskValue struct {
	Text        string               `json:"text,omitempty"`
	ID          int64                `json:"id,omitempty"`
	Type        apistructs.IssueType `json:"type,omitempty"`
	IterationID int64                `json:"iterationID,omitempty"` // TODO not in common
	LinkStyle   bool                 `json:"linkStyle,omitempty"`
}

type User struct {
	Avatar     string     `json:"avatar,omitempty"`
	Value      uint64     `json:"value,omitempty"`
	Name       string     `json:"name,omitempty"`
	Nick       string     `json:"nick,omitempty"`
	RenderType RenderType `json:"renderType,omitempty"`
}

type RenderType string

var (
	RenderTypeGantt        RenderType = "gantt"
	RenderTypeStringList   RenderType = "string-list"
	RenderTypeMemberAvatar RenderType = "userAvatar"

	DefaultPageNo   = uint64(1)
	DefaultPageSize = uint64(200)
)
