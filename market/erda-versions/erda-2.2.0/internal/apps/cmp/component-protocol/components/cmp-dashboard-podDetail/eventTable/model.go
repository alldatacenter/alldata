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

package eventTable

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp"
)

type ComponentEventTable struct {
	SDK    *cptype.SDK `json:"-"`
	ctx    context.Context
	server cmp.SteveServer

	Type       string                 `json:"type,omitempty"`
	Props      Props                  `json:"props,omitempty"`
	Data       Data                   `json:"data,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type State struct {
	ClusterName string `json:"clusterName,omitempty"`
	PodID       string `json:"podId,omitempty"`
}

type Data struct {
	List []Item `json:"list"`
}

type Item struct {
	ID                string `json:"id,omitempty"`
	LastSeen          string `json:"lastSeen"`
	LastSeenTimestamp int64  `json:"lastSeenTimestamp"`
	Type              string `json:"type"`
	Reason            string `json:"reason"`
	Message           string `json:"message"`
}

type Props struct {
	RequestIgnore  []string `json:"requestIgnore,omitempty"`
	RowKey         string   `json:"rowKey,omitempty''"`
	Pagination     bool     `json:"pagination"`
	Columns        []Column `json:"columns"`
	SortDirections []string `json:"sortDirections,omitempty"`
}

type Column struct {
	DataIndex string `json:"dataIndex"`
	Title     string `json:"title"`
	Width     int    `json:"width"`
}

type Operation struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload"`
}

type Sorter struct {
	Field string `json:"field,omitempty"`
	Order string `json:"order,omitempty"`
}
