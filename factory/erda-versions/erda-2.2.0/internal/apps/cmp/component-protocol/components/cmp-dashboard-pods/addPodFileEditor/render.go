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

package addPodFileEditor

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/ghodss/yaml"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "addPodFileEditor", func() servicehub.Provider {
		return &ComponentAddPodFileEditor{}
	})
}

var steveServer cmp.SteveServer

func (e *ComponentAddPodFileEditor) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (e *ComponentAddPodFileEditor) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if _, ok := (*gs)["renderByFilter"]; ok {
		delete(*gs, "renderByFilter")
		return nil
	}

	e.InitComponent(ctx)
	if err := e.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen addPodFileEditor component state, %v", err)
	}

	switch event.Operation {
	case cptype.RenderingOperation:
		if err := e.RenderFile(); err != nil {
			return err
		}
	case "submit":
		if err := e.CreatePod(); err != nil {
			return errors.Errorf("failed to create pod, %v", err)
		}
		(*gs)["drawerOpen"] = false
	}
	e.SetComponentValue()
	e.Transfer(component)
	return nil
}

func (e *ComponentAddPodFileEditor) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	e.sdk = sdk
	e.ctx = ctx
	e.server = steveServer
}

func (e *ComponentAddPodFileEditor) GenComponentState(component *cptype.Component) error {
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
	e.State = state
	return nil
}

func (e *ComponentAddPodFileEditor) SetComponentValue() {
	e.Props.Bordered = true
	e.Props.FileValidate = []string{"not-empty", "yaml"}
	e.Props.MinLines = 22
	e.Operations = map[string]interface{}{
		"submit": Operation{
			Key:        "submit",
			Reload:     true,
			SuccessMsg: e.sdk.I18n("createdPodSuccessfully"),
		},
	}
}

func (e *ComponentAddPodFileEditor) RenderFile() error {
	e.State.Value = podTemplate
	return nil
}

func (e *ComponentAddPodFileEditor) CreatePod() error {
	var pod corev1.Pod
	jsonData, err := yaml.YAMLToJSON([]byte(e.State.Value))
	if err != nil {
		return err
	}
	if err := json.Unmarshal(jsonData, &pod); err != nil {
		return err
	}

	// use selected namespace
	// ignore namespace in yaml
	pod.Namespace = e.State.Values.Namespace

	req := &apistructs.SteveRequest{
		NoAuthentication: false,
		UserID:           e.sdk.Identity.UserID,
		OrgID:            e.sdk.Identity.OrgID,
		Type:             apistructs.K8SPod,
		ClusterName:      e.State.ClusterName,
		Obj:              pod,
	}
	if _, err := e.server.CreateSteveResource(e.ctx, req); err != nil {
		return err
	}
	return nil
}

func (e *ComponentAddPodFileEditor) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(e.Props)
	component.State = map[string]interface{}{
		"clusterName": e.State.ClusterName,
		"values":      e.State.Values,
		"value":       e.State.Value,
	}
	component.Operations = e.Operations
}
