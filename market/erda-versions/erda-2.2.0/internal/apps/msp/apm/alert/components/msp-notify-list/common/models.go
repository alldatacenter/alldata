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

package common

import (
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type IdNameValue struct {
	Id   interface{}
	Name string
}

type Sort struct {
	FieldKey  string
	Ascending bool
}

type ConfigurableFilterOptions struct {
	NotifyName string  `json:"notifyName"`
	Status     string  `json:"status"`
	Channel    string  `json:"channel"`
	AlertId    int64   `json:"alertId"`
	SendTime   []int64 `json:"sendTime"`
}

type NotifyAttributes struct {
	AlertId   int64  `json:"alertId"`
	AlertName string `json:"alertName"`
	GroupId   int64  `json:"groupId"`
}

type DataRef struct {
	DataRef map[string]interface{} `json:"dataRef"`
}

func NewConfigurableFilterOptions() *ConfigurableFilterOptions {
	return &ConfigurableFilterOptions{}
}
func (f *ConfigurableFilterOptions) GetFromGlobalState(gs cptype.GlobalStateData) *ConfigurableFilterOptions {
	val := gs[GlobalStateKeyConfigurableFilterOptionsKey]
	if val == nil {
		return f
	}

	typedVal, ok := val.(*ConfigurableFilterOptions)
	if ok {
		return typedVal
	}

	_ = mapstructure.Decode(val, f)
	return f
}

func (f *ConfigurableFilterOptions) DecodeFromClientData(data filter.OpFilterClientData) *ConfigurableFilterOptions {
	_ = mapstructure.Decode(data.Values, f)
	return f
}

func (f *ConfigurableFilterOptions) UpdateName(name string) *ConfigurableFilterOptions {
	f.NotifyName = name
	return f
}

func (f *ConfigurableFilterOptions) SetToGlobalState(gs cptype.GlobalStateData) {
	gs[GlobalStateKeyConfigurableFilterOptionsKey] = f
}
