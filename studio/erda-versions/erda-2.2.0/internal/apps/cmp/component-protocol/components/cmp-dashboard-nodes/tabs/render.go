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
	"context"
	"encoding/base64"
	"fmt"

	"github.com/sirupsen/logrus"
	"gopkg.in/square/go-jose.v2/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
)

func (t *Tabs) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	// import components data
	t.SDK = cputil.SDK(ctx)
	t.Props = Props{"small",
		"button",
		[]MenuPair{
			{
				Key:  table.CPU_TAB,
				Text: t.SDK.I18n(string(table.CPU_TAB)),
			},
			{
				Key:  table.MEM_TAB,
				Text: t.SDK.I18n(string(table.MEM_TAB)),
			},
			{
				Key:  table.POD_TAB,
				Text: t.SDK.I18n(string(table.POD_TAB)),
			},
		},
		"solid",
	}
	// Default is cpu, overwritten if event is changeTab.
	t.State.Value = table.CPU_TAB
	switch event.Operation {
	case cptype.InitializeOperation:
		if _, ok := t.SDK.InParams["tableTabs__urlQuery"]; ok {
			if err := t.DecodeURLQuery(); err != nil {
				return fmt.Errorf("failed to decode url query for filter component, %v", err)
			}
		}
	case common.CMPDashboardTableTabs:
		t.State.Value = table.TableType(c.State["value"].(string))
	default:
		logrus.Warnf("operation [%s] not support, scenario:%v, event:%v", event.Operation, s, event)
	}
	(*gs)["activeKey"] = string(t.State.Value)
	t.getOperations()
	err := t.EncodeURLQuery()
	if err != nil {
		return err
	}
	return t.RenderProtocol(c)
}

func (t *Tabs) DecodeURLQuery() error {
	query, ok := t.SDK.InParams["tableTabs__urlQuery"].(string)
	if !ok {
		return nil
	}
	decoded, err := base64.StdEncoding.DecodeString(query)
	if err != nil {
		return err
	}

	var values string
	if err := json.Unmarshal(decoded, &values); err != nil {
		return err
	}
	t.State.Value = table.TableType(values)
	return nil
}

func (t *Tabs) EncodeURLQuery() error {
	jsonData, err := json.Marshal(t.State.Value)
	if err != nil {
		return err
	}

	encoded := base64.StdEncoding.EncodeToString(jsonData)
	t.State.TableTabsURLQuery = encoded
	return nil
}

func (t *Tabs) getOperations() {
	t.Operations = map[string]interface{}{"onChange": Operation{
		Key:    string(common.CMPDashboardTableTabs),
		Reload: true,
	},
	}
}

func (t *Tabs) RenderProtocol(c *cptype.Component) error {
	return common.Transfer(*t, c)
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodes", "tabs", func() servicehub.Provider {
		return &Tabs{Type: "Radio"}
	})
}
