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

package envFilter

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
)

type ComponentAction struct {
}

const urlQuery = "envFilter__urlQuery"

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	sdk := cputil.SDK(ctx)

	if c.State == nil {
		c.State = map[string]interface{}{}
	}

	var defaultEnv string
	if c.State["value"] == nil {
		if sdk.InParams != nil && sdk.InParams[urlQuery] != nil {
			defaultEnv = sdk.InParams[urlQuery].(string)
		} else {
			defaultEnv = apistructs.DevEnv
			c.State[urlQuery] = defaultEnv
		}
	} else {
		defaultEnv = c.State["value"].(string)
		c.State[urlQuery] = defaultEnv
	}

	c.Type = "Radio"
	c.Operations = map[string]interface{}{
		"onChange": map[string]interface{}{
			"key":    "changeEnv",
			"reload": true,
		},
	}
	c.State["value"] = defaultEnv
	c.Props = map[string]interface{}{
		"buttonStyle": "solid",
		"radioType":   "button",
		"options": []map[string]interface{}{
			{
				"key":  apistructs.DevEnv,
				"text": cputil.I18n(ctx, "dev-env"),
			},
			{
				"key":  apistructs.TestEnv,
				"text": cputil.I18n(ctx, "test-env"),
			},
			{
				"key":  apistructs.StagingEnv,
				"text": cputil.I18n(ctx, "feature-env"),
			},
			{
				"key":  apistructs.ProdEnv,
				"text": cputil.I18n(ctx, "prod-env"),
			},
		},
	}

	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "envFilter", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
