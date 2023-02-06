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

package ContainerTable

type ContainerTable struct {
	Type  string            `json:"type,omitempty"`
	Props Props             `json:"props"`
	Data  map[string][]Data `json:"data"`
	State State             `json:"state,omitempty"`
}

type State struct {
	ClusterName string `json:"clusterName,omitempty"`
	PodID       string `json:"podId,omitempty"`
}

type Data struct {
	Status       Status      `json:"status"`
	Ready        string      `json:"ready"`
	Name         string      `json:"name"`
	Images       Images      `json:"images"`
	RestartCount interface{} `json:"restartCount"`
	Operate      Operate     `json:"operate"`
}

type Scroll struct {
	X int `json:"x"`
}

type Props struct {
	RequestIgnore  []string `json:"requestIgnore,omitempty"`
	RowKey         string   `json:"rowKey"`
	Pagination     bool     `json:"pagination"`
	Scroll         Scroll   `json:"scroll"`
	Columns        []Column `json:"columns"`
	SortDirections []string `json:"sortDirections,omitempty"`
}

type Column struct {
	DataIndex string `json:"dataIndex"`
	Title     string `json:"title"`
	Fixed     string `json:"fixed,omitempty"`
	Align     string `json:"align,omitempty"`
}

type Operate struct {
	Operations map[string]Operation `json:"operations"`
	RenderType string               `json:"renderType"`
	Value      string               `json:"value,omitempty"`
}

type Status struct {
	RenderType string `json:"renderType"`
	Value      string `json:"value,omitempty"`
	Status     string `json:"status,omitempty"`
	Breathing  bool   `json:"breathing,omitempty"`
}

type Images struct {
	RenderType string `json:"renderType"`
	Value      Value  `json:"value"`
}

type Operation struct {
	Key    string                 `json:"key"`
	Text   string                 `json:"text"`
	Reload bool                   `json:"reload"`
	Meta   map[string]interface{} `json:"meta,omitempty"`
}

type StyleConfig struct {
	Color string `json:"color"`
}

type Value struct {
	Text string `json:"text"`
}
