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

package cancelExecuteButton

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	visible    bool
	pipelineId uint64
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "cancelExecuteButton",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)

	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.visible = gh.GetExecuteTaskBreadcrumbVisible()
	ca.pipelineId = gh.GetExecuteHistoryTablePipelineID()
	ca.sdk = cputil.SDK(ctx)

	switch event.Operation {
	case "cancelExecute":

		var req apistructs.PipelineCancelRequest
		req.PipelineID = gh.GetExecuteHistoryTablePipelineID()
		req.UserID = ca.sdk.Identity.UserID
		err := ca.bdl.CancelPipeline(req)
		if err != nil {
			return err
		}

		c.State["reloadScenesInfo"] = true
		c.Props = map[string]interface{}{
			"text":    ca.sdk.I18n("cancelRun"),
			"visible": false,
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		c.Type = "Button"
		visible := gh.GetExecuteTaskBreadcrumbVisible()
		if ca.pipelineId > 0 && visible {
			if ca.pipelineId > 0 {
				rsp := gh.GetPipelineInfoWithPipelineID(ca.pipelineId, ca.bdl)
				if rsp == nil {
					return fmt.Errorf("not find pipelineID %v info", ca.pipelineId)
				}
				if !rsp.Status.IsReconcilerRunningStatus() {
					visible = false
				}
			} else {
				visible = false
			}
		}
		if ca.pipelineId == 0 {
			visible = false
		}
		c.Props = map[string]interface{}{
			"text":    ca.sdk.I18n("cancelRun"),
			"visible": visible,
		}
		c.Operations = map[string]interface{}{
			"click": map[string]interface{}{
				"key":    "cancelExecute",
				"reload": true,
			},
		}
	}
	return nil
}
