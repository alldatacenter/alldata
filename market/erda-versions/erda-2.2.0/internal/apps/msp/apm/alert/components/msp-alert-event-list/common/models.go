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
	Name               string    `json:"name,omitempty"`
	AlertRuleIds       []uint64  `json:"alertRuleIds,omitempty"`
	AlertIds           []uint64  `json:"alertIds,omitempty"`
	AlertLevels        []string  `json:"alertLevels,omitempty"`
	AlertStates        []string  `json:"alertStates,omitempty"`
	AlertSources       []string  `json:"alertSources,omitempty"`
	LastTriggerTime    []*uint64 `json:"lastTriggerTime,omitempty"`
	LastTriggerTimeMin uint64    `json:"lastTriggerTimeMin,omitempty"`
	LastTriggerTimeMax uint64    `json:"lastTriggerTimeMax,omitempty"`
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
	f.fillLastTriggerTime()
	return f
}

func (f *ConfigurableFilterOptions) DecodeFromClientData(data filter.OpFilterClientData) *ConfigurableFilterOptions {
	_ = mapstructure.Decode(data.Values, f)
	f.fillLastTriggerTime()
	return f
}

func (f *ConfigurableFilterOptions) UpdateName(name string) *ConfigurableFilterOptions {
	f.Name = name
	return f
}

func (f *ConfigurableFilterOptions) SetToGlobalState(gs cptype.GlobalStateData) {
	gs[GlobalStateKeyConfigurableFilterOptionsKey] = f
}

func (f *ConfigurableFilterOptions) fillLastTriggerTime() {
	if len(f.LastTriggerTime) != 2 {
		return
	}
	if f.LastTriggerTime[0] != nil {
		f.LastTriggerTimeMin = *f.LastTriggerTime[0]
	}
	if f.LastTriggerTime[1] != nil {
		f.LastTriggerTimeMax = *f.LastTriggerTime[1]
	}
}
