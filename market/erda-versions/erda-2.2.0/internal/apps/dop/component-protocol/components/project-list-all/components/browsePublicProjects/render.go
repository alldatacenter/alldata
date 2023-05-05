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

package browsePublicProjects

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/i18n"
)

func (i *ComponentBrowsePublic) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)
	visible := gh.GetIsEmpty()
	selectedOption := gh.GetOption()
	if !visible || selectedOption != "my" {
		c.Props = map[string]interface{}{"visible": false}
		return nil
	}

	if event.Operation.String() == "toPublicProject" {
		gh.SetOption("public")
		gh.SetIsEmpty(false)
		return nil
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
				map[string]interface{}{
					"text":         cputil.I18n(ctx, i18n.I18nProjectPublicBrowse),
					"operationKey": "toPublicProject",
					"styleConfig":  map[string]interface{}{"bold": true},
				},
			},
		},
	}

	i.Operations = map[string]interface{}{
		"toPublicProject": Operation{
			Key:    "toPublicProject",
			Reload: true,
		},
	}

	c.Operations = i.Operations
	c.Props = cputil.MustConvertProps(i.Props)
	return nil
}

func init() {
	base.InitProviderWithCreator("project-list-all", "browsePublicProjects",
		func() servicehub.Provider { return &ComponentBrowsePublic{} },
	)
}
