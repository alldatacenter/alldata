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

package operationButton

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-podDetail", "operationButton", func() servicehub.Provider {
		return &ComponentOperationButton{}
	})
}

var steveServer cmp.SteveServer

func (b *ComponentOperationButton) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (b *ComponentOperationButton) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b.InitComponent(ctx)
	if err := b.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen operationButton component state, %v", err)
	}
	b.SetComponentValue()
	switch event.Operation {
	case "checkYaml":
		(*gs)["drawerOpen"] = true
	case "delete":
		if err := b.DeletePod(); err != nil {
			return errors.Errorf("failed to delete pod, %v", err)
		}
		delete(*gs, "drawerOpen")
		(*gs)["deleted"] = true
	}
	b.Transfer(component)
	return nil
}

func (b *ComponentOperationButton) InitComponent(ctx context.Context) {
	b.ctx = ctx
	sdk := cputil.SDK(ctx)
	b.sdk = sdk
	b.server = steveServer
}

func (b *ComponentOperationButton) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var state State
	data, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(data, &state); err != nil {
		return err
	}
	b.State = state
	return nil
}

func (b *ComponentOperationButton) SetComponentValue() {
	b.Props.Text = b.sdk.I18n("moreOperations")
	b.Props.Type = "primary"
	b.Props.Menu = []Menu{
		{
			Key:  "checkYaml",
			Text: b.sdk.I18n("viewOrEditYaml"),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    "checkYaml",
					Reload: true,
				},
			},
		},
		{
			Key:  "delete",
			Text: b.sdk.I18n("delete"),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:        "delete",
					Reload:     true,
					SuccessMsg: b.sdk.I18n("deletedPodSuccessfully"),
					Confirm:    b.sdk.I18n("confirmDelete"),
					Command: Command{
						Key:    "goto",
						Target: "cmpClustersPods",
						State: CommandState{
							Params: map[string]string{
								"clusterName": b.State.ClusterName,
							},
						},
					},
				},
			},
		},
	}
}

func (b *ComponentOperationButton) DeletePod() error {
	splits := strings.Split(b.State.PodID, "_")
	if len(splits) != 2 {
		return errors.Errorf("invalid pod id, %s", b.State.PodID)
	}
	namespace, name := splits[0], splits[1]

	request := &apistructs.SteveRequest{
		UserID:      b.sdk.Identity.UserID,
		OrgID:       b.sdk.Identity.OrgID,
		Type:        apistructs.K8SPod,
		ClusterName: b.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}

	return b.server.DeleteSteveResource(b.ctx, request)
}

func (b *ComponentOperationButton) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(b.Props)
	c.State = map[string]interface{}{
		"clusterName": b.State.ClusterName,
		"podId":       b.State.PodID,
	}
}
