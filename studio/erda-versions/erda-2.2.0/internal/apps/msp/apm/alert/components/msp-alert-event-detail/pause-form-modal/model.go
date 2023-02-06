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

package pause_form_modal

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

type ComponentPauseModalFormInfo struct {
	sdk      *cptype.SDK `json:"-"`
	ctx      context.Context
	inParams *common.InParams

	Type       string               `json:"type,omitempty"`
	Props      Props                `json:"props"`
	Data       map[string]Data      `json:"data,omitempty"`
	State      State                `json:"state,omitempty"`
	Operations map[string]Operation `json:"operations,omitempty"`

	MonitorAlertService monitorpb.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService"`
}

type Props struct {
	RequestIgnore []string `json:"requestIgnore,omitempty"`
	Name          string   `json:"name"`
	Title         string   `json:"title"`
	Fields        []Field  `json:"fields"`
}

type Data struct {
}

type Field struct {
	Key            string                 `json:"key"`
	Label          string                 `json:"label"`
	Required       bool                   `json:"required"`
	Component      string                 `json:"component"`
	ComponentProps map[string]interface{} `json:"componentProps,omitempty"`
}

type Operation struct {
	Key     string   `json:"key"`
	Reload  bool     `json:"reload"`
	Command *Command `json:"command,omitempty"`
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

type State struct {
	Reload   bool      `json:"reload"`
	Visible  bool      `json:"visible"`
	FormData *FormData `json:"formData"`
	Paused   bool      `json:"paused"`
}

type FormData struct {
	PauseExpireTime uint64 `json:"pauseExpireTime"`
}
