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

package emptyText

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/i18n"
)

func (i *ComponentText) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)
	visible := gh.GetIsEmpty()
	if !visible {
		c.Props = map[string]interface{}{"visible": false}
		return nil
	}

	text := cputil.I18n(ctx, i18n.I18nProjectNotJoined)
	selectedOption := gh.GetOption()
	if selectedOption == "public" {
		text = cputil.I18n(ctx, "publicProjectEmpty")
	}

	i.Props = Props{
		Visible:    true,
		RenderType: "linkText",
		StyleConfig: StyleConfig{
			FontSize:   16,
			LineHeight: 24,
		},
		Value: map[string]interface{}{
			"text": []interface{}{
				text,
			},
		},
	}

	c.Props = cputil.MustConvertProps(i.Props)
	return nil
}

func init() {
	base.InitProviderWithCreator("project-list-all", "emptyText",
		func() servicehub.Provider { return &ComponentText{} },
	)
}
