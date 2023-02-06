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

package cancelButton

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
)

type ComponentAction struct {
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {

	svc := ctx.Value(types.CodeCoverageService).(*code_coverage.CodeCoverage)
	sdk := cputil.SDK(ctx)
	projectIDStr := sdk.InParams["projectId"].(string)
	projectId, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return err
	}

	workspace, ok := c.State["workspace"].(string)
	if !ok {
		return fmt.Errorf("workspace was empty")
	}

	var disable bool

	switch event.Operation.String() {
	case apistructs.ClickOperation.String():
		err := svc.Cancel(apistructs.CodeCoverageCancelRequest{
			ProjectID: projectId,
			Workspace: workspace,
			IdentityInfo: apistructs.IdentityInfo{
				UserID: sdk.Identity.UserID,
			},
		})
		if err != nil {
			return err
		}
	case apistructs.InitializeOperation.String(), apistructs.RenderingOperation.String():
		if c.State == nil {
			c.State = map[string]interface{}{}
		}

		disableSourcecov := c.State["disableSourcecov"]
		if disableSourcecov != nil {
			disable = disableSourcecov.(bool)
		}
	}

	c.Type = "Button"
	c.Props = map[string]interface{}{
		"text": cputil.I18n(ctx, "forced-cancel"),
		"type": "primary",
	}
	c.Operations = map[string]interface{}{
		"click": map[string]interface{}{
			"key":      "click",
			"reload":   true,
			"confirm":  cputil.I18n(ctx, "cancel-confirm"),
			"disabled": disable,
		},
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "cancelButton", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
