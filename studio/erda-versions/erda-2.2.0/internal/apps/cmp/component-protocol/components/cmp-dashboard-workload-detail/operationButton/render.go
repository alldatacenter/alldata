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
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/apps/cmp"
	cputil2 "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
	cmpTypes "github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/cmp/steve"
	"github.com/erda-project/erda/internal/apps/cmp/steve/middleware"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workload-detail", "operationButton", func() servicehub.Provider {
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
	case "restart":
		if err := b.RestartWorkload(); err != nil {
			return errors.Errorf("failed to restart workload, %v", err)
		}
	case "delete":
		if err := b.DeleteWorkload(); err != nil {
			return errors.Errorf("failed to delete workload, %v", err)
		}
		delete(*gs, "drawerOpen")
		(*gs)["deleted"] = true
	}
	b.Transfer(component)
	return nil
}

func (b *ComponentOperationButton) InitComponent(ctx context.Context) {
	b.ctx = ctx
	bdl := ctx.Value(cmpTypes.GlobalCtxKeyBundle).(*bundle.Bundle)
	b.bdl = bdl
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
	splits := strings.Split(b.State.WorkloadID, "_")
	if len(splits) != 3 {
		logrus.Errorf("invalid workload id, %s", b.State.WorkloadID)
		return
	}
	kind, namespace, name := splits[0], splits[1], splits[2]

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
	}

	restartOperation := Operation{
		Key:        "restart",
		Reload:     true,
		SuccessMsg: b.sdk.I18n("restartWorkloadSuccessfully"),
		Confirm:    b.sdk.I18n("confirmRestart"),
	}
	restartMenu := Menu{
		Key:        "restart",
		Text:       b.sdk.I18n("restart"),
		Operations: map[string]interface{}{},
	}
	if kind == string(apistructs.K8SJob) || kind == string(apistructs.K8SCronJob) {
		restartOperation.Disabled = true
		restartOperation.DisabledTip = b.sdk.I18n("cannotRestart")
	}
	restartMenu.Operations["click"] = restartOperation
	b.Props.Menu = append(b.Props.Menu, restartMenu)

	deleteOperation := Operation{
		Key:        "delete",
		Reload:     true,
		SuccessMsg: b.sdk.I18n("deletedWorkloadSuccessfully"),
		Confirm:    b.sdk.I18n("confirmDelete"),
		Command: Command{
			Key:    "goto",
			Target: "cmpClustersWorkload",
			State: CommandState{
				Params: map[string]string{
					"clusterName": b.State.ClusterName,
				},
			},
		},
	}
	deleteMenu := Menu{
		Key:        "delete",
		Text:       b.sdk.I18n("delete"),
		Operations: map[string]interface{}{},
	}

	if namespace == "kube-system" || namespace == "erda-system" || b.isSystemWorkload(kind, namespace, name) {
		deleteOperation.Disabled = true
		deleteOperation.DisabledTip = b.sdk.I18n("canNotDeleteSystemWorkload")
	}
	deleteMenu.Operations["click"] = deleteOperation
	b.Props.Menu = append(b.Props.Menu, deleteMenu)
}

func (b *ComponentOperationButton) isSystemWorkload(kind, namespace, name string) bool {
	req := &apistructs.SteveRequest{
		UserID:      b.sdk.Identity.UserID,
		OrgID:       b.sdk.Identity.OrgID,
		Type:        apistructs.K8SResType(kind),
		ClusterName: b.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}
	workload, err := b.server.GetSteveResource(b.ctx, req)
	if err != nil {
		logrus.Errorf("failed to get workload %s:%s:%s, %v", kind, namespace, name, err)
		return false
	}

	nodeSelectorTerms := workload.Data().Slice("spec", "template", "spec", "affinity", "nodeAffinity",
		"requiredDuringSchedulingIgnoredDuringExecution", "nodeSelectorTerms")
	for _, obj := range nodeSelectorTerms {
		matchExpressions := obj.Slice("matchExpressions")
		if len(matchExpressions) == 0 {
			continue
		}
		for _, obj := range matchExpressions {
			key := obj.String("key")
			operator := obj.String("operator")
			if key != "dice/platform" {
				continue
			}
			if operator == "Exists" {
				return true
			}
			if operator == "In" {
				values := obj.StringSlice("values")
				for _, value := range values {
					if value == "true" {
						return true
					}
				}
			}
		}
	}
	return false
}

func (b *ComponentOperationButton) RestartWorkload() error {
	splits := strings.Split(b.State.WorkloadID, "_")
	if len(splits) != 3 {
		return errors.Errorf("invalid workload id, %s", b.State.WorkloadID)
	}

	kind, namespace, name := splits[0], splits[1], splits[2]
	if kind != string(apistructs.K8SDeployment) && kind != string(apistructs.K8SStatefulSet) &&
		kind != string(apistructs.K8SDaemonSet) {
		return errors.Errorf("invalid workload kind %s (only deployment, statefulSet and daemonSet can be restarted)", kind)
	}

	userID := b.sdk.Identity.UserID
	orgID := b.sdk.Identity.OrgID
	clusterName := b.State.ClusterName
	scopeID, err := strconv.ParseUint(orgID, 10, 64)
	if err != nil {
		return apierrors.ErrInvoke.InvalidParameter(fmt.Errorf("invalid org id %s, %v", orgID, err))
	}

	if err := b.restartWorkload(userID, orgID, clusterName, kind, namespace, name); err != nil {
		return err
	}

	steve.RemoveCache(clusterName, namespace, string(apistructs.K8SPod))

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  clusterName,
		middleware.AuditResourceName: name,
		middleware.AuditNamespace:    namespace,
		middleware.AuditResourceType: kind,
	}

	now := strconv.FormatInt(time.Now().Unix(), 10)
	if err := b.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{
		Audit: apistructs.Audit{
			UserID:       userID,
			ScopeType:    apistructs.OrgScope,
			ScopeID:      scopeID,
			OrgID:        scopeID,
			Context:      auditCtx,
			TemplateName: middleware.AuditRestartWorkload,
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
		},
	}); err != nil {
		logrus.Errorf("failed to audit for restarting workload, %v", err)
	}
	return nil
}

func (b *ComponentOperationButton) restartWorkload(userID, orgID, clusterName, kind, namespace, name string) error {
	client, err := cputil2.GetImpersonateClient(b.server, userID, orgID, clusterName)
	if err != nil {
		return errors.Errorf("failed to get k8s client, %v", err)
	}

	patchBody := map[string]interface{}{
		"spec": map[string]interface{}{
			"template": map[string]interface{}{
				"metadata": map[string]interface{}{
					"annotations": map[string]interface{}{
						"kubectl.kubernetes.io/restartedAt": time.Now().Format("2006-01-02T15:04:05+07:00"),
					},
				},
			},
		},
	}

	data, err := json.Marshal(patchBody)
	if err != nil {
		return errors.Errorf("failed to marshal body, %v", err)
	}

	switch kind {
	case string(apistructs.K8SDeployment):
		_, err = client.ClientSet.AppsV1().Deployments(namespace).Patch(b.ctx, name, types.StrategicMergePatchType, data, v1.PatchOptions{})
	case string(apistructs.K8SStatefulSet):
		_, err = client.ClientSet.AppsV1().StatefulSets(namespace).Patch(b.ctx, name, types.StrategicMergePatchType, data, v1.PatchOptions{})
	case string(apistructs.K8SDaemonSet):
		_, err = client.ClientSet.AppsV1().DaemonSets(namespace).Patch(b.ctx, name, types.StrategicMergePatchType, data, v1.PatchOptions{})
	default:
		return errors.Errorf("invalid workload kind %s (only deployment, statefulSet and daemonSet can be restarted)", kind)
	}
	if err != nil {
		return err
	}

	steve.RemoveCache(b.State.ClusterName, "", kind)
	steve.RemoveCache(b.State.ClusterName, namespace, kind)
	return nil
}

func (b *ComponentOperationButton) DeleteWorkload() error {
	splits := strings.Split(b.State.WorkloadID, "_")
	if len(splits) != 3 {
		return errors.Errorf("invalid workload id, %s", b.State.WorkloadID)
	}
	kind, namespace, name := splits[0], splits[1], splits[2]

	req := &apistructs.SteveRequest{
		UserID:      b.sdk.Identity.UserID,
		OrgID:       b.sdk.Identity.OrgID,
		Type:        apistructs.K8SResType(kind),
		ClusterName: b.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}

	if err := b.server.DeleteSteveResource(b.ctx, req); err != nil {
		return err
	}
	steve.RemoveCache(b.State.ClusterName, namespace, string(apistructs.K8SPod))
	return nil
}

func (b *ComponentOperationButton) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(b.Props)
	component.State = map[string]interface{}{
		"clusterName": b.State.ClusterName,
		"workloadId":  b.State.WorkloadID,
	}
}
