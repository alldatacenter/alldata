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

package recordDrawer

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
)

type Props struct {
	Title string `json:"title"`
	Size  string `json:"size"`
}

type State struct {
	Visible bool `json:"visible"`
}

type RecordDrawer struct {
	sdk *cptype.SDK

	Type  string `json:"type"`
	Props Props  `json:"props"`
	State State  `json:"state"`
}

func (r *RecordDrawer) GenComponentState(c *apistructs.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	r.State = state
	return nil
}

func (r *RecordDrawer) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	r.sdk = cputil.SDK(ctx)
	r.Type = "Drawer"
	r.Props.Title = r.sdk.I18n(i18n.I18nKeyImportExportTable)
	r.Props.Size = "xl"
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "recordDrawer",
		func() servicehub.Provider { return &RecordDrawer{} })
}
