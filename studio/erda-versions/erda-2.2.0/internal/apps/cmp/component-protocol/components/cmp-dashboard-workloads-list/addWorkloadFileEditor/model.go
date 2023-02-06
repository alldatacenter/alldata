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

package addWorkloadFileEditor

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp"
)

type ComponentAddWorkloadFileEditor struct {
	sdk        *cptype.SDK
	ctx        context.Context
	server     cmp.SteveServer
	Type       string                 `json:"type,omitempty"`
	State      State                  `json:"state"`
	Props      Props                  `json:"props"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type Props struct {
	Bordered     bool     `json:"bordered"`
	FileValidate []string `json:"fileValidate,omitempty"`
	MinLines     int      `json:"minLines"`
}

type State struct {
	ClusterName  string `json:"clusterName,omitempty"`
	Value        string `json:"value,omitempty"`
	Values       Values `json:"values,omitempty"`
	WorkloadKind string `json:"workloadKind,omitempty"`
}

type Operation struct {
	Key        string `json:"key,omitempty"`
	Reload     bool   `json:"reload"`
	SuccessMsg string `json:"successMsg,omitempty"`
}

type Values struct {
	WorkloadKind string `json:"workloadKind,omitempty"`
	Namespace    string `json:"namespace,omitempty"`
}
