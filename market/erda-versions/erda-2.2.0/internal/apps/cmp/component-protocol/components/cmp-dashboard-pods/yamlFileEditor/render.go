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

package yamlFileEditor

import (
	"context"
	"encoding/json"
	"strings"

	"github.com/ghodss/yaml"
	"github.com/pkg/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	cputil2 "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "yamlFileEditor", func() servicehub.Provider {
		return &ComponentYamlFileEditor{}
	})
}

var steveServer cmp.SteveServer

func (f *ComponentYamlFileEditor) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (f *ComponentYamlFileEditor) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	f.InitComponent(ctx)
	if err := f.GenComponentState(component); err != nil {
		return errors.Errorf("failed to gen yamlFileEditor component state, %v", err)
	}

	switch event.Operation {
	case cptype.RenderingOperation:
		podID, _ := (*gs)["podID"].(string)
		if podID == "" {
			return nil
		}
		f.State.PodID = podID
		if err := f.RenderFile(); err != nil {
			return errors.Errorf("failed to render yaml file, %v", err)
		}
	case "submit":
		if err := f.UpdatePod(); err != nil {
			return errors.Errorf("failed to update pod, %v", err)
		}
		f.State.PodID = ""
	}
	delete(*gs, "podID")
	f.SetComponentValue()
	f.Transfer(component)
	return nil
}

func (f *ComponentYamlFileEditor) InitComponent(ctx context.Context) {
	f.ctx = ctx
	sdk := cputil.SDK(ctx)
	f.sdk = sdk
	f.server = steveServer
}

func (f *ComponentYamlFileEditor) GenComponentState(component *cptype.Component) error {
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
	f.State = state
	f.Transfer(component)
	return nil
}

func (f *ComponentYamlFileEditor) RenderFile() error {
	splits := strings.Split(f.State.PodID, "_")
	if len(splits) != 2 {
		return errors.Errorf("invalid pod id: %s", f.State.PodID)
	}

	namespace, name := splits[0], splits[1]
	cli, err := cputil2.GetImpersonateClient(f.server, f.sdk.Identity.UserID, f.sdk.Identity.OrgID, f.State.ClusterName)
	if err != nil {
		return err
	}

	pod, err := cli.ClientSet.CoreV1().Pods(namespace).Get(f.ctx, name, metav1.GetOptions{})
	if err != nil {
		return errors.Errorf("failed to get pod %s:%s, %v", namespace, name, err)
	}

	gvk, unversioned, err := cli.CRClient.Scheme().ObjectKinds(pod)
	if err != nil {
		return errors.Errorf("failed to get object kind, %v", err)
	}
	if !unversioned && len(gvk) == 1 {
		pod.SetGroupVersionKind(gvk[0])
	}

	data, err := json.Marshal(pod)
	if err != nil {
		return err
	}

	yamlData, err := yaml.JSONToYAML(data)
	if err != nil {
		return err
	}

	f.State.Value = string(yamlData)
	return nil
}

func (f *ComponentYamlFileEditor) UpdatePod() error {
	splits := strings.Split(f.State.PodID, "_")
	if len(splits) != 2 {
		return errors.Errorf("invalid pod id: %s", f.State.PodID)
	}
	namespace, name := splits[0], splits[1]

	jsonData, err := yaml.YAMLToJSON([]byte(f.State.Value))
	if err != nil {
		return errors.Errorf("failed to convert yaml to json, %v", err)
	}
	var pod map[string]interface{}
	if err = json.Unmarshal(jsonData, &pod); err != nil {
		return errors.Errorf("failed to unmarshal pod, %v", err)
	}

	req := &apistructs.SteveRequest{
		UserID:      f.sdk.Identity.UserID,
		OrgID:       f.sdk.Identity.OrgID,
		Type:        apistructs.K8SPod,
		ClusterName: f.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
		Obj:         pod,
	}

	_, err = f.server.UpdateSteveResource(f.ctx, req)
	return err
}

func (f *ComponentYamlFileEditor) SetComponentValue() {
	f.Props.Bordered = true
	f.Props.FileValidate = []string{"not-empty", "yaml"}
	f.Props.MinLines = 22
	f.Operations = map[string]interface{}{
		"submit": Operation{
			Key:        "submit",
			Reload:     true,
			SuccessMsg: f.sdk.I18n("updatePodSuccessfully"),
		},
	}
}

func (f *ComponentYamlFileEditor) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(f.Props)
	component.State = map[string]interface{}{
		"clusterName": f.State.ClusterName,
		"podId":       f.State.PodID,
		"value":       f.State.Value,
	}
	component.Operations = f.Operations
}
