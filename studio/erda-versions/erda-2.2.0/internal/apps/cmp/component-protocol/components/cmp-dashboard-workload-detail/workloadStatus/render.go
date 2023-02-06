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

package workloadStatus

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	cputil2 "github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workload-detail", "workloadStatus", func() servicehub.Provider {
		return &ComponentWorkloadStatus{}
	})
}

var steveServer cmp.SteveServer

func (s *ComponentWorkloadStatus) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (s *ComponentWorkloadStatus) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	s.InitComponent(ctx)
	if err := s.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen workloadStatue component state, %v", err)
	}
	if err := s.SetComponentValue(); err != nil {
		return fmt.Errorf("failed to set workoadStatus component value, %v", err)
	}
	s.Transfer(component)
	return nil
}

func (s *ComponentWorkloadStatus) InitComponent(ctx context.Context) {
	sdk := cputil2.SDK(ctx)
	s.sdk = sdk
	s.ctx = ctx
	s.server = steveServer
}

func (s *ComponentWorkloadStatus) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var state State
	jsonData, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(jsonData, &state); err != nil {
		return err
	}
	s.State = state
	return nil
}

func (s *ComponentWorkloadStatus) SetComponentValue() error {
	kind, namespace, name, err := cputil.ParseWorkloadID(s.State.WorkloadID)
	if err != nil {
		return err
	}

	userID := s.sdk.Identity.UserID
	orgID := s.sdk.Identity.OrgID
	req := apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        kind,
		ClusterName: s.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}

	resp, err := s.server.GetSteveResource(s.ctx, &req)
	if err != nil {
		return err
	}
	obj := resp.Data()

	status, color, breathing, err := cputil.ParseWorkloadStatus(obj)
	if err != nil {
		return err
	}
	s.Props.Text = s.sdk.I18n(status)
	s.Props.Status = color
	s.Props.Breathing = breathing
	return nil
}

func (s *ComponentWorkloadStatus) Transfer(component *cptype.Component) {
	component.Props = cputil2.MustConvertProps(s.Props)
	component.State = map[string]interface{}{
		"clusterName": s.State.ClusterName,
		"workloadId":  s.State.WorkloadID,
	}
}
