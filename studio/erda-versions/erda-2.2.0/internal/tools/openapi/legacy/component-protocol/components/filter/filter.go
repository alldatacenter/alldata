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

package filter

type CommonFilter struct {
	Version    string                     `json:"version,omitempty"`
	Name       string                     `json:"name,omitempty"`
	Type       string                     `json:"type,omitempty"`
	Props      Props                      `json:"props,omitempty"`
	Operations map[OperationKey]Operation `json:"operations,omitempty"`
}

type Props struct {
	Delay uint64 `json:"delay,omitempty"`
}

type PropConditionKey string

func (k PropConditionKey) String() string { return string(k) }

type PropCondition struct {
	Key         PropConditionKey       `json:"key,omitempty"`
	Label       string                 `json:"label,omitempty"`
	EmptyText   string                 `json:"emptyText,omitempty"`
	Fixed       bool                   `json:"fixed,omitempty"`
	ShowIndex   int                    `json:"showIndex,omitempty"`
	HaveFilter  bool                   `json:"haveFilter,omitempty"`
	Split       bool                   `json:"split,omitempty"`
	Type        PropConditionType      `json:"type,omitempty"`
	QuickSelect QuickSelect            `json:"quickSelect,omitempty"`
	QuickAdd    QuickAdd               `json:"quickAdd,omitempty"`
	QuickDelete QuickDelete            `json:"quickDelete,omitempty"`
	Placeholder string                 `json:"placeholder,omitempty"`
	Options     []PropConditionOption  `json:"options,omitempty"`
	Required    bool                   `json:"required,omitempty"`
	CustomProps map[string]interface{} `json:"customProps,omitempty"`
	Disabled    bool                   `json:"disabled,omitempty"`
}

type QuickSelect struct {
	Label        string       `json:"label,omitempty"`
	OperationKey OperationKey `json:"operationKey,omitempty"`
}

type QuickAdd struct {
	OperationKey OperationKey `json:"operationKey,omitempty"`
	Placeholder  string       `json:"placeholder,omitempty"`
	Show         bool         `json:"show"`
}

type QuickDelete struct {
	OperationKey OperationKey `json:"operationKey,omitempty"`
}

type PropConditionOption struct {
	Label    string                `json:"label,omitempty"`
	Value    interface{}           `json:"value,omitempty"`
	Icon     string                `json:"icon,omitempty"`
	Children []PropConditionOption `json:"children,omitempty"`
}

type PropConditionType string

var (
	PropConditionTypeSelect        PropConditionType = "select"
	PropConditionTypeInput         PropConditionType = "input"
	PropConditionTypeDateRange     PropConditionType = "dateRange"
	PropConditionTypeRangePicker   PropConditionType = "rangePicker"
	PropConditionTypeTimespanRange PropConditionType = "timespanRange"
)

type StateKey string

type OperationKey string
type Operation struct {
	Key      OperationKey           `json:"key,omitempty"`
	Reload   bool                   `json:"reload,omitempty"`
	Meta     map[string]interface{} `json:"meta,omitempty"`
	FillMeta string                 `json:"fillMeta,omitempty"`
}

func (k OperationKey) String() string {
	return string(k)
}
