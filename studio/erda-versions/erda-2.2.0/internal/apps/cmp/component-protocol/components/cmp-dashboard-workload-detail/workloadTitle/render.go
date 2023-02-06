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

package workloadTitle

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

var steveServer cmp.SteveServer

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workload-detail", "workloadTitle", func() servicehub.Provider {
		return &ComponentWorkloadTitle{}
	})
}

func (t *ComponentWorkloadTitle) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (t *ComponentWorkloadTitle) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	t.InitComponent(ctx)
	if err := t.GenComponentState(component); err != nil {
		return errors.Errorf("failed to gen workloadTitle state, %v", err)
	}
	if t.State.WorkloadID == "-" {
		var err error
		t.State.WorkloadID, err = t.getWorkloadID(t.State.PodID)
		if err != nil {
			return err
		}
		if t.State.WorkloadID == "" {
			return errors.Errorf("The pod is not created by a workload")
		}
	}
	splits := strings.Split(t.State.WorkloadID, "_")
	if len(splits) != 3 {
		return fmt.Errorf("invalid workload id: %s", t.State.WorkloadID)
	}
	kind, name := splits[0], splits[2]
	typ := ""
	switch kind {
	case string(apistructs.K8SDeployment):
		typ = "Deployment"
	case string(apistructs.K8SReplicaSet):
		typ = "ReplicaSet"
	case string(apistructs.K8SDaemonSet):
		typ = "DaemonSet"
	case string(apistructs.K8SStatefulSet):
		typ = "StatefulSet"
	case string(apistructs.K8SJob):
		typ = "Job"
	case string(apistructs.K8SCronJob):
		typ = "CronJob"
	}

	t.Props.Title = fmt.Sprintf("%s: %s", typ, name)
	t.Transfer(component)
	return nil
}

func (t *ComponentWorkloadTitle) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	t.sdk = sdk
	t.ctx = ctx
	t.server = steveServer
}

func (t *ComponentWorkloadTitle) GenComponentState(component *cptype.Component) error {
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
	t.State = state
	return nil
}

func (t *ComponentWorkloadTitle) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(t.Props)
	component.State = map[string]interface{}{
		"workloadId":  t.State.WorkloadID,
		"podId":       t.State.PodID,
		"clusterName": t.State.ClusterName,
	}
}

func (t *ComponentWorkloadTitle) getWorkloadID(podID string) (string, error) {
	splits := strings.Split(podID, "_")
	if len(splits) != 2 {
		return "", errors.Errorf("invalid pod id: %s", podID)
	}
	podNamespace, podName := splits[0], splits[1]
	req := &apistructs.SteveRequest{
		UserID:      t.sdk.Identity.UserID,
		OrgID:       t.sdk.Identity.OrgID,
		Type:        apistructs.K8SPod,
		ClusterName: t.State.ClusterName,
		Name:        podName,
		Namespace:   podNamespace,
	}
	obj, err := t.server.GetSteveResource(t.ctx, req)
	if err != nil {
		return "", err
	}
	pod := obj.Data()

	ownerReferences := pod.Slice("metadata", "ownerReferences")
	if len(ownerReferences) == 0 {
		return "", nil
	}
	ownerReference := ownerReferences[0]
	name := ownerReference.String("name")
	kind := ownerReference.String("kind")
	namespace := pod.String("metadata", "namespace")

	if kind == "ReplicaSet" {
		req := &apistructs.SteveRequest{
			UserID:      t.sdk.Identity.UserID,
			OrgID:       t.sdk.Identity.OrgID,
			Type:        apistructs.K8SReplicaSet,
			ClusterName: t.State.ClusterName,
			Name:        name,
			Namespace:   namespace,
		}

		resp, err := t.server.GetSteveResource(t.ctx, req)
		if err != nil {
			return "", err
		}
		obj := resp.Data()

		ownerReferences := obj.Slice("metadata", "ownerReferences")
		if len(ownerReferences) == 0 {
			return fmt.Sprintf("%s_%s_%s", apistructs.K8SReplicaSet, namespace, name), nil
		}
		ownerReference = ownerReferences[0]
		kind = ownerReference.String("kind")
		name = ownerReference.String("name")
	}

	ownerKind := map[string]apistructs.K8SResType{
		"Deployment":  apistructs.K8SDeployment,
		"ReplicaSet":  apistructs.K8SReplicaSet,
		"DaemonSet":   apistructs.K8SDaemonSet,
		"StatefulSet": apistructs.K8SStatefulSet,
		"Job":         apistructs.K8SJob,
		"CronJob":     apistructs.K8SCronJob,
	}

	return fmt.Sprintf("%s_%s_%s", ownerKind[kind], namespace, name), nil
}
