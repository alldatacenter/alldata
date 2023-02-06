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

package tabs

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
)

type Tabs struct {
	SDK        *cptype.SDK
	Type       string                 `json:"type"`
	Props      Props                  `json:"props"`
	State      State                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
}

type Props struct {
	Size        string     `json:"size"`
	RadioType   string     `json:"radioType"`
	Options     []MenuPair `json:"options"`
	ButtonStyle string     `json:"buttonStyle"`
}

type MenuPair struct {
	Key  table.TableType `json:"key"`
	Text string          `json:"text"`
}

type State struct {
	Value             table.TableType `json:"value"`
	TableTabsURLQuery string          `json:"tableTabs__urlQuery,omitempty"`
}

type Meta struct {
	ActiveKey string `json:"activeKey"`
}

type Operation struct {
	Key      string `json:"key"`
	Reload   bool   `json:"reload"`
	FillMeta string `json:"fillMeta"`
	Meta     Meta   `json:"meta"`
}
