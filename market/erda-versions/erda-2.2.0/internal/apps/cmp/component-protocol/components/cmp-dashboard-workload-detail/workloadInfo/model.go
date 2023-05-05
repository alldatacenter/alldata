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

package workloadInfo

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp"
)

type ComponentWorkloadInfo struct {
	sdk    *cptype.SDK
	ctx    context.Context
	server cmp.SteveServer

	Type  string `json:"type,omitempty"`
	Props Props  `json:"props,omitempty"`
	Data  Data   `json:"data,omitempty"`
	State State  `json:"state,omitempty"`
}

type Data struct {
	Data DataInData `json:"data,omitempty"`
}

type DataInData struct {
	Namespace   string `json:"namespace,omitempty"`
	Age         string `json:"age,omitempty"`
	Images      string `json:"images,omitempty"`
	Labels      []Tag  `json:"labels"`
	Annotations []Tag  `json:"annotations"`
}

type Tag struct {
	Label string `json:"label,omitempty"`
	Group string `json:"group,omitempty"`
}

type Props struct {
	RequestIgnore []string `json:"requestIgnore,omitempty"`
	ColumnNum     int      `json:"columnNum"`
	Fields        []Field  `json:"fields,omitempty"`
}

type Field struct {
	Label      string `json:"label,omitempty"`
	ValueKey   string `json:"valueKey,omitempty"`
	RenderType string `json:"renderType,omitempty"`
	SpaceNum   int    `json:"spaceNum,omitempty"`
}

type State struct {
	ClusterName string `json:"clusterName,omitempty"`
	WorkloadID  string `json:"workloadId,omitempty"`
}
