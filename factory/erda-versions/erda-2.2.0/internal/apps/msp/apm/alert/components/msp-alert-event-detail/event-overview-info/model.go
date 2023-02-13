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

package event_overview_info

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

type ComponentEventOverviewInfo struct {
	sdk *cptype.SDK `json:"-"`
	ctx context.Context

	Type   string                       `json:"type,omitempty"`
	Props  Props                        `json:"props"`
	Data   map[string]Data              `json:"data,omitempty"`
	State  State                        `json:"state,omitempty"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

type Props struct {
	RequestIgnore []string `json:"requestIgnore,omitempty"`
	ColumnNum     int      `json:"columnNum"`
	Fields        []Field  `json:"fields"`
}

type Data struct {
	RelatedRuleName  string `json:"relatedRuleName"`
	RelatedAlertName string `json:"relatedAlertName"`
	AlertLevel       string `json:"alertLevel"`
	AlertSource      string `json:"alertSource"`
	AlertSubject     []Tag  `json:"alertSubject"`
	AlertCount       string `json:"alertCount"`
	FirstTriggerTime string `json:"firstTriggerTime"`
	LastTriggerTime  string `json:"lastTriggerTime"`
}

type Field struct {
	Label      string               `json:"label"`
	ValueKey   string               `json:"valueKey"`
	RenderType string               `json:"renderType,omitempty"`
	Operations map[string]Operation `json:"operations,omitempty"`
	SpaceNum   int                  `json:"spaceNum,omitempty"`
	Color      string               `json:"color,omitempty"`
}

type Operation struct {
	Key     string  `json:"key"`
	Reload  bool    `json:"reload"`
	Command Command `json:"command,omitempty"`
}

type Command struct {
	Key     string       `json:"key"`
	Target  string       `json:"target"`
	State   CommandState `json:"state"`
	JumpOut bool         `json:"jumpOut"`
}

type CommandState struct {
	Params map[string]string `json:"params"`
	Query  map[string]string `json:"query,omitempty"`
}

type Tag struct {
	Label string `json:"label,omitempty"`
	Group string `json:"group,omitempty"`
}

type State struct {
}
