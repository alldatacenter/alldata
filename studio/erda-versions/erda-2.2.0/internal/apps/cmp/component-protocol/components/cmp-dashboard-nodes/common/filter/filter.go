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

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
)

type Filter struct {
	CtxBdl     *bundle.Bundle
	SDK        *cptype.SDK
	Type       string                 `json:"type"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Props      Props                  `json:"props"`
}

var DefaultLabels = []string{"dice/workspace-dev=true", "dice/workspace-test=true", "dice/workspace-staging=true",
	"dice/workspace-prod=true", "dice/stateful-service=true", "dice/stateless-service=true",
	"dice/location-cluster-service=true", "dice/job=true", "dice/bigdata-job=true", "dice/lb=true", "dice/platform=true"}

func (f *Filter) GetFilterProps(labels map[string]struct{}) Props {
	p := Props{
		Delay: 1000,
	}
	return p
}

type Values map[string]interface{}

type State struct {
	Conditions     []Condition `json:"conditions"`
	Values         Values      `json:"values"`
	ClusterName    string      `json:"clusterName"`
	FilterUrlQuery string      `json:"filter__urlQuery,omitempty"`
}
type Condition struct {
	EmptyText   string   `json:"emptyText"`
	Fixed       bool     `json:"fixed"`
	Key         string   `json:"key"`
	Label       string   `json:"label"`
	HaveFilter  bool     `json:"haveFilter"`
	Options     []Option `json:"options"`
	Type        string   `json:"type"`
	Placeholder string   `json:"placeholder"`
}

type Props struct {
	Delay int `json:"delay"`
}

type Operation struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload,omitempty"`
}

//type Field struct {
//	Multiple    bool     `json:"multiple"`
//	Label       string   `json:"label"`
//	Type        string   `json:"type"`
//	Options     []Option `json:"options"`
//	Key         string   `json:"key"`
//	Placeholder string   `json:"placeholder"`
//}

type Option struct {
	Label    string   `json:"label"`
	Value    string   `json:"value"`
	Children []Option `json:"children"`
}
