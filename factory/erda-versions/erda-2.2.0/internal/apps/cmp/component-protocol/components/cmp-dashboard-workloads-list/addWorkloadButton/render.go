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

package addWorkloadButton

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workloads-list", "addWorkloadButton", func() servicehub.Provider {
		return &ComponentAddWorkloadButton{}
	})
}

func (b *ComponentAddWorkloadButton) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b.InitComponent(ctx)
	b.Props.Text = b.sdk.I18n("createWorkload")
	b.Props.Type = "primary"
	b.Props.Menu = []Menu{
		{
			Key: string(apistructs.K8SDeployment),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    string(apistructs.K8SDeployment),
					Reload: true,
				},
			},
			Text: "Deployment",
		},
		{
			Key: string(apistructs.K8SStatefulSet),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    string(apistructs.K8SStatefulSet),
					Reload: true,
				},
			},
			Text: "StatefulSet",
		},
		{
			Key: string(apistructs.K8SDaemonSet),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    string(apistructs.K8SDaemonSet),
					Reload: true,
				},
			},
			Text: "DaemonSet",
		},
		{
			Key: string(apistructs.K8SJob),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    string(apistructs.K8SJob),
					Reload: true,
				},
			},
			Text: "Job",
		},
		{
			Key: string(apistructs.K8SCronJob),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    string(apistructs.K8SCronJob),
					Reload: true,
				},
			},
			Text: "CronJob",
		},
	}

	(*gs)["drawerOpen"] = true
	if event.Operation != cptype.InitializeOperation && event.Operation != cptype.RenderingOperation {
		b.State.WorkloadKind = event.Operation.String()
	}
	b.Transfer(component)
	return nil
}

func (b *ComponentAddWorkloadButton) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	b.sdk = sdk
}

func (b *ComponentAddWorkloadButton) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(b.Props)
	c.State = map[string]interface{}{
		"workloadKind": b.State.WorkloadKind,
	}
}
