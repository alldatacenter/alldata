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

package moreButton

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/component_spec/button"
)

type MoreButton struct {
	sdk *cptype.SDK

	Props button.Props `json:"props"`
	State State        `json:"state"`
}

type State struct {
	Visible bool `json:"visible"`
}

func (i *MoreButton) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	i.sdk = cputil.SDK(ctx)
	if event.Operation == "autoRefresh" || event.Operation == "openRecord" {
		i.State.Visible = true
	}
	i.Props = button.Props{
		Text:  i.sdk.I18n("moreOperations"),
		Type:  "primary",
		Ghost: true,
		Menu: []button.MenuItem{
			{
				Key:  "import",
				Text: i.sdk.I18n(i18n.I18nKeyImport),
				Operations: map[string]interface{}{
					"click": map[string]interface{}{
						"key":    "import",
						"reload": false,
					},
				},
			},
			{
				Key:  "record",
				Text: i.sdk.I18n(i18n.I18nKeyImportExportRecord),
				Operations: map[string]interface{}{
					"click": map[string]interface{}{
						"key":    "openRecord",
						"reload": true,
					},
				},
			},
		},
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "moreButton",
		func() servicehub.Provider { return &MoreButton{} })
}
