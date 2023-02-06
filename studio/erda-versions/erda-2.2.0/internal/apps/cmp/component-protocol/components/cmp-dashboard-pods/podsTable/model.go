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

package podsTable

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp"
)

type ComponentPodsTable struct {
	sdk    *cptype.SDK
	bdl    *bundle.Bundle
	ctx    context.Context
	server cmp.SteveServer

	Type       string                 `json:"type,omitempty"`
	Props      Props                  `json:"props,omitempty"`
	Data       Data                   `json:"data,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type State struct {
	ActiveKey         string         `json:"activeKey,omitempty"`
	ClusterName       string         `json:"clusterName,omitempty"`
	CountValues       map[string]int `json:"countValues"`
	PageNo            int            `json:"pageNo"`
	PageSize          int            `json:"pageSize"`
	PodsTableURLQuery string         `json:"podsTable__urlQuery,omitempty"`
	Sorter            Sorter         `json:"sorterData,omitempty"`
	Total             int            `json:"total"`
	Values            Values         `json:"values,omitempty"`
}

type Values struct {
	Kind      []string `json:"kind,omitempty"`
	Namespace string   `json:"namespace,omitempty"`
	Status    []string `json:"status,omitempty"`
	Node      []string `json:"node,omitempty"`
	Search    string   `json:"search,omitempty"`
}

type Sorter struct {
	Field string `json:"field,omitempty"`
	Order string `json:"order,omitempty"`
}

type Data struct {
	List []Item `json:"list,omitempty"`
}

type Item struct {
	ID                string   `json:"id,omitempty"`
	Status            Status   `json:"status"`
	Name              Multiple `json:"name"`
	Namespace         string   `json:"namespace,omitempty"`
	PodName           string   `json:"podName,omitempty"`
	IP                string   `json:"ip,omitempty"`
	Age               string   `json:"age,omitempty"`
	CPURequests       Multiple `json:"cpuRequests,omitempty"`
	CPURequestsNum    int64    `json:"CPURequestsNum,omitempty"`
	CPUPercent        Percent  `json:"cpuPercent,omitempty"`
	CPULimits         Multiple `json:"cpuLimits"`
	CPULimitsNum      int64    `json:"CPULimitsNum,omitempty"`
	MemoryRequests    Multiple `json:"memoryRequests"`
	MemoryRequestsNum int64    `json:"MemoryRequestsNum,omitempty"`
	MemoryPercent     Percent  `json:"memoryPercent"`
	MemoryLimits      Multiple `json:"memoryLimits"`
	MemoryLimitsNum   int64    `json:"MemoryLimitsNum,omitempty"`
	Ready             string   `json:"ready,omitempty"`
	Node              Operate  `json:"node,omitempty"`
	Operations        Operate  `json:"operations"`
}

type Status struct {
	RenderType string `json:"renderType,omitempty"`
	Value      string `json:"value,omitempty"`
	Status     string `json:"status,omitempty"`
	Breathing  bool   `json:"breathing"`
}

type Multiple struct {
	RenderType string        `json:"renderType,omitempty"`
	Direction  string        `json:"direction,omitempty"`
	Renders    []interface{} `json:"renders,omitempty"`
}

type TextWithIcon struct {
	RenderType string `json:"renderType,omitempty"`
	Icon       string `json:"icon,omitempty"`
	Value      string `json:"value,omitempty"`
	Size       string `json:"size,omitempty"`
}

type Operate struct {
	RenderType string                 `json:"renderType,omitempty"`
	Value      string                 `json:"value,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type LinkOperation struct {
	Command    *Command               `json:"command,omitempty"`
	Reload     bool                   `json:"reload"`
	Key        string                 `json:"key,omitempty"`
	Text       string                 `json:"text,omitempty"`
	Meta       map[string]interface{} `json:"meta,omitempty"`
	Confirm    string                 `json:"confirm,omitempty"`
	SuccessMsg string                 `json:"successMsg,omitempty"`
}

type Command struct {
	Key     string       `json:"key,omitempty"`
	Target  string       `json:"target,omitempty"`
	State   CommandState `json:"state,omitempty"`
	JumpOut bool         `json:"jumpOut"`
}

type CommandState struct {
	Params map[string]string `json:"params,omitempty"`
	Query  map[string]string `json:"query,omitempty"`
}

type Percent struct {
	RenderType string `json:"renderType,omitempty"`
	Value      string `json:"value,omitempty"`
	Tip        string `json:"tip,omitempty"`
	Status     string `json:"status,omitempty"`
}

type Props struct {
	RequestIgnore   []string               `json:"requestIgnore,omitempty"`
	RowKey          string                 `json:"rowKey,omitempty"`
	PageSizeOptions []string               `json:"pageSizeOptions,omitempty"`
	Columns         []Column               `json:"columns,omitempty"`
	Operations      map[string]interface{} `json:"operations,omitempty"`
	SortDirections  []string               `json:"sortDirections,omitempty"`
}

type Column struct {
	DataIndex string `json:"dataIndex,omitempty"`
	Title     string `json:"title,omitempty"`
	Sorter    bool   `json:"sorter"`
	Fixed     string `json:"fixed,omitempty"`
	Align     string `json:"align,omitempty"`
	Hidden    bool   `json:"hidden"`
}

type Operation struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload"`
}
