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

package workloadTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/go-openapi/strfmt"
	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/recallsong/go-utils/container/slice"
	"github.com/sirupsen/logrus"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	types2 "k8s.io/apimachinery/pkg/types"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-workloads-list/filter"
	cmpcputil "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
	cmpTypes "github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/cmp/steve"
	"github.com/erda-project/erda/internal/apps/cmp/steve/middleware"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-workloads-list", "workloadTable", func() servicehub.Provider {
		return &ComponentWorkloadTable{}
	})
}

var steveServer cmp.SteveServer

func (w *ComponentWorkloadTable) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (w *ComponentWorkloadTable) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	w.InitComponent(ctx)
	if err := w.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen workloadTable component state, %v", err)
	}

	switch event.Operation {
	case cptype.InitializeOperation:
		w.State.PageNo = 1
		w.State.PageSize = 20
		if err := w.DecodeURLQuery(); err != nil {
			return fmt.Errorf("failed to get url query for workloadTable component, %v", err)
		}
	case cptype.RenderingOperation, "changePageSize", "changeSort":
		w.State.PageNo = 1
	case "restart":
		workloadID, err := getWorkloadID(event.OperationData)
		if err != nil {
			return err
		}
		if err = w.RestartWorkload(workloadID); err != nil {
			return errors.Errorf("failed to restart workload, %v", err)
		}
	case "delete":
		workloadID, err := getWorkloadID(event.OperationData)
		if err != nil {
			return err
		}
		if err = w.DeleteWorkload(workloadID); err != nil {
			return errors.Errorf("failed to delete workload, %v", err)
		}
	case "checkYaml":
		workloadID, err := getWorkloadID(event.OperationData)
		if err != nil {
			return err
		}
		(*gs)["workloadID"] = workloadID
		return nil
	}

	if err := w.RenderTable(); err != nil {
		return fmt.Errorf("failed to render workloadTable component, %v", err)
	}
	if err := w.EncodeURLQuery(); err != nil {
		return fmt.Errorf("failed to gen url query for workloadTable component, %v", err)
	}
	w.SetComponentValue(ctx)
	w.Transfer(component)
	return nil
}

func (w *ComponentWorkloadTable) DecodeURLQuery() error {
	queryData, ok := w.sdk.InParams["workloadTable__urlQuery"].(string)
	if !ok {
		return nil
	}
	decode, err := base64.StdEncoding.DecodeString(queryData)
	if err != nil {
		return err
	}
	query := make(map[string]interface{})
	if err := json.Unmarshal(decode, &query); err != nil {
		return err
	}
	w.State.PageNo = uint64(query["pageNo"].(float64))
	w.State.PageSize = uint64(query["pageSize"].(float64))
	sorterData := query["sorterData"].(map[string]interface{})
	w.State.Sorter.Field, _ = sorterData["field"].(string)
	w.State.Sorter.Order, _ = sorterData["order"].(string)
	return nil
}

func (w *ComponentWorkloadTable) EncodeURLQuery() error {
	urlQuery := make(map[string]interface{})
	urlQuery["pageNo"] = w.State.PageNo
	urlQuery["pageSize"] = w.State.PageSize
	urlQuery["sorterData"] = w.State.Sorter
	byteData, err := json.Marshal(urlQuery)
	if err != nil {
		return err
	}

	encoded := base64.StdEncoding.EncodeToString(byteData)
	w.State.WorkloadTableURLQuery = encoded
	return nil
}

func (w *ComponentWorkloadTable) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	w.sdk = sdk
	bdl := ctx.Value(cmpTypes.GlobalCtxKeyBundle).(*bundle.Bundle)
	w.bdl = bdl
	w.ctx = ctx
	w.server = steveServer
}

func (w *ComponentWorkloadTable) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	byteData, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(byteData, &state); err != nil {
		return err
	}
	w.State = state
	return nil
}

func (w *ComponentWorkloadTable) RestartWorkload(workloadID string) error {
	splits := strings.Split(workloadID, "_")
	if len(splits) != 3 {
		return errors.Errorf("invalid workload id, %s", workloadID)
	}

	kind, namespace, name := splits[0], splits[1], splits[2]
	if kind != string(apistructs.K8SDeployment) && kind != string(apistructs.K8SStatefulSet) &&
		kind != string(apistructs.K8SDaemonSet) {
		return errors.Errorf("invalid workload kind %s (only deployment, statefulSet and daemonSet can be restarted)", kind)
	}

	userID := w.sdk.Identity.UserID
	orgID := w.sdk.Identity.OrgID
	clusterName := w.State.ClusterName
	scopeID, err := strconv.ParseUint(orgID, 10, 64)
	if err != nil {
		return apierrors.ErrInvoke.InvalidParameter(fmt.Errorf("invalid org id %s, %v", orgID, err))
	}

	if err := w.restartWorkload(userID, orgID, clusterName, kind, namespace, name); err != nil {
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
	if err := w.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{
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

func (w *ComponentWorkloadTable) restartWorkload(userID, orgID, clusterName, kind, namespace, name string) error {
	client, err := cmpcputil.GetImpersonateClient(w.server, userID, orgID, clusterName)
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
		_, err = client.ClientSet.AppsV1().Deployments(namespace).Patch(w.ctx, name, types2.StrategicMergePatchType, data, v1.PatchOptions{})
	case string(apistructs.K8SStatefulSet):
		_, err = client.ClientSet.AppsV1().StatefulSets(namespace).Patch(w.ctx, name, types2.StrategicMergePatchType, data, v1.PatchOptions{})
	case string(apistructs.K8SDaemonSet):
		_, err = client.ClientSet.AppsV1().DaemonSets(namespace).Patch(w.ctx, name, types2.StrategicMergePatchType, data, v1.PatchOptions{})
	default:
		return errors.Errorf("invalid workload kind %s (only deployment, statefulSet and daemonSet can be restarted)", kind)
	}
	if err != nil {
		return err
	}

	steve.RemoveCache(w.State.ClusterName, "", kind)
	steve.RemoveCache(w.State.ClusterName, namespace, kind)
	return nil
}

func (w *ComponentWorkloadTable) DeleteWorkload(workloadID string) error {
	splits := strings.Split(workloadID, "_")
	if len(splits) != 3 {
		return errors.Errorf("invalid workload id, %s", workloadID)
	}
	kind, namespace, name := splits[0], splits[1], splits[2]

	req := &apistructs.SteveRequest{
		UserID:      w.sdk.Identity.UserID,
		OrgID:       w.sdk.Identity.OrgID,
		Type:        apistructs.K8SResType(kind),
		ClusterName: w.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}

	if err := w.server.DeleteSteveResource(w.ctx, req); err != nil {
		return err
	}
	steve.RemoveCache(w.State.ClusterName, namespace, string(apistructs.K8SPod))
	return nil
}

func (w *ComponentWorkloadTable) RenderTable() error {
	userID := w.sdk.Identity.UserID
	orgID := w.sdk.Identity.OrgID

	steveRequest := apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Namespace:   w.State.Values.Namespace,
		ClusterName: w.State.ClusterName,
	}

	var items []Item
	kinds := getWorkloadKindMap(w.State.Values.Kind)

	activeCount := map[apistructs.K8SResType]int{}
	abnormalCount := map[apistructs.K8SResType]int{}
	succeededCount := map[apistructs.K8SResType]int{}
	failedCount := map[apistructs.K8SResType]int{}
	updatingCount := map[apistructs.K8SResType]int{}
	stoppedCount := map[apistructs.K8SResType]int{}

	for _, kind := range []apistructs.K8SResType{apistructs.K8SDeployment, apistructs.K8SStatefulSet,
		apistructs.K8SDaemonSet, apistructs.K8SJob, apistructs.K8SCronJob} {
		if _, ok := kinds[string(kind)]; !ok && len(kinds) != 0 {
			continue
		}
		steveRequest.Type = kind
		var (
			list []types.APIObject
			err  error
		)
		list, err = w.server.ListSteveResource(w.ctx, &steveRequest)
		if err != nil {
			return err
		}

		for _, obj := range list {
			workload := obj.Data()
			if w.State.Values.Search != "" && !strings.Contains(workload.String("metadata", "name"), w.State.Values.Search) {
				continue
			}

			statusValue, statusColor, breathing, err := cmpcputil.ParseWorkloadStatus(workload)
			if err != nil {
				logrus.Error(err)
				continue
			}
			if w.State.Values.Status != nil && !contain(w.State.Values.Status, statusValue) {
				continue
			}
			status := Status{
				RenderType: "textWithBadge",
				Value:      w.sdk.I18n(statusValue),
				Status:     statusColor,
				Breathing:  breathing,
			}

			switch statusValue {
			case "Active":
				activeCount[kind]++
			case "Abnormal":
				abnormalCount[kind]++
			case "Updating":
				updatingCount[kind]++
			case "Stopped":
				stoppedCount[kind]++
			case "Succeeded":
				succeededCount[kind]++
			case "Failed":
				failedCount[kind]++
			}

			name := workload.String("metadata", "name")
			namespace := workload.String("metadata", "namespace")
			id := fmt.Sprintf("%s_%s_%s", kind, namespace, name)

			operations := w.getOperations(workload, string(kind), id)

			fields := workload.StringSlice("metadata", "fields")
			item := Item{
				ID:     id,
				Status: status,
				Name: Multiple{
					RenderType: "multiple",
					Direction:  "row",
					Renders: []interface{}{
						[]interface{}{
							TextWithIcon{
								RenderType: "icon",
								Icon:       "default_k8s_workload",
							},
						},
						[]interface{}{
							Link{
								RenderType: "linkText",
								Value:      name,
								Operations: map[string]interface{}{
									"click": LinkOperation{
										Reload: false,
										Key:    "openWorkloadDetail",
									},
								},
							},
							TextWithIcon{
								RenderType: "subText",
								Value:      fmt.Sprintf("%s: %s", w.sdk.I18n("namespace"), namespace),
							},
						},
					},
				},
				WorkloadName: name,
				Namespace:    namespace,
				Kind: Kind{
					RenderType: "tagsRow",
					Size:       "normal",
					Value: KindValue{
						Label: workload.String("kind"),
					},
				},
				Operations: operations,
			}

			switch kind {
			case apistructs.K8SDeployment:
				if len(fields) != 8 {
					logrus.Errorf("deployment %s:%s has invalid fields length %d", namespace, name, len(fields))
					continue
				}
				item.Age = fields[4]
				item.Ready = fields[1]
				item.UpToDate = fields[2]
				item.Available = fields[3]
			case apistructs.K8SDaemonSet:
				if len(fields) != 11 {
					logrus.Errorf("daemonset %s:%s has invalid fields length %d", namespace, name, len(fields))
					continue
				}
				item.Age = fields[7]
				item.Ready = fields[3]
				item.UpToDate = fields[4]
				item.Available = fields[5]
				item.Desired = fields[1]
				item.Current = fields[2]
			case apistructs.K8SStatefulSet:
				if len(fields) != 5 {
					logrus.Errorf("statefulSet %s:%s has invalid fields length %d", namespace, name, len(fields))
					continue
				}
				item.Age = fields[2]
				item.Ready = fields[1]
			case apistructs.K8SJob:
				if len(fields) != 7 {
					logrus.Errorf("job %s:%s has invalid fields length %d", namespace, name, len(fields))
					continue
				}
				item.Age = fields[3]
				item.Completions = fields[1]
				item.Duration = fields[2]
			case apistructs.K8SCronJob:
				if len(fields) != 9 {
					logrus.Errorf("cronJob %s:%s has invalid fields length %d", namespace, name, len(fields))
					continue
				}
				item.Age = fields[5]
				item.Schedule = fields[1]
				item.LastSchedule = fields[4]
			}
			items = append(items, item)
		}
	}

	w.State.CountValues = CountValues{
		DeploymentsCount: Count{
			Active:   activeCount[apistructs.K8SDeployment],
			Abnormal: abnormalCount[apistructs.K8SDeployment],
			Updating: updatingCount[apistructs.K8SDeployment],
			Stopped:  stoppedCount[apistructs.K8SDeployment],
		},
		DaemonSetCount: Count{
			Active:   activeCount[apistructs.K8SDaemonSet],
			Abnormal: abnormalCount[apistructs.K8SDaemonSet],
			Updating: updatingCount[apistructs.K8SDaemonSet],
			Stopped:  stoppedCount[apistructs.K8SDaemonSet],
		},
		StatefulSetCount: Count{
			Active:   activeCount[apistructs.K8SStatefulSet],
			Abnormal: abnormalCount[apistructs.K8SStatefulSet],
			Updating: updatingCount[apistructs.K8SStatefulSet],
			Stopped:  updatingCount[apistructs.K8SDaemonSet],
		},
		JobCount: Count{
			Active:    activeCount[apistructs.K8SJob],
			Succeeded: succeededCount[apistructs.K8SJob],
			Failed:    failedCount[apistructs.K8SJob],
		},
		CronJobCount: Count{
			Active: activeCount[apistructs.K8SCronJob],
		},
	}

	if w.State.Sorter.Field != "" {
		cmpWrapper := func(field, order string) func(int, int) bool {
			ascend := order == "ascend"
			switch field {
			case "status":
				return func(i int, j int) bool {
					less := items[i].Status.Value < items[j].Status.Value
					if ascend {
						return less
					}
					return !less
				}
			case "name":
				return func(i int, j int) bool {
					less := items[i].WorkloadName < items[j].WorkloadName
					if ascend {
						return less
					}
					return !less
				}
			case "namespace":
				return func(i int, j int) bool {
					less := items[i].Namespace < items[j].Namespace
					if ascend {
						return less
					}
					return !less
				}
			case "kind":
				return func(i int, j int) bool {
					less := items[i].Kind.Value.Label < items[j].Kind.Value.Label
					if ascend {
						return less
					}
					return !less
				}
			case "age":
				return func(i int, j int) bool {
					ageI, _ := strfmt.ParseDuration(items[i].Age)
					ageJ, _ := strfmt.ParseDuration(items[j].Age)
					less := ageI < ageJ
					if ascend {
						return less
					}
					return !less
				}
			case "ready":
				return func(i int, j int) bool {
					less := false
					if strings.Contains(items[i].Ready, "/") {
						readyI := strings.Split(items[i].Ready, "/")[0]
						readyJ := strings.Split(items[j].Ready, "/")[0]
						less = readyI < readyJ
					} else {
						readyI, _ := strconv.ParseInt(items[i].Ready, 10, 64)
						readyJ, _ := strconv.ParseInt(items[j].Ready, 10, 64)
						less = readyI < readyJ
					}
					if ascend {
						return less
					}
					return !less
				}
			case "upToDate":
				return func(i int, j int) bool {
					nI, _ := strconv.ParseInt(items[i].UpToDate, 10, 64)
					nJ, _ := strconv.ParseInt(items[j].UpToDate, 10, 64)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "available":
				return func(i int, j int) bool {
					nI, _ := strconv.ParseInt(items[i].Available, 10, 64)
					nJ, _ := strconv.ParseInt(items[j].Available, 10, 64)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "desired":
				return func(i int, j int) bool {
					nI, _ := strconv.ParseInt(items[i].Desired, 10, 64)
					nJ, _ := strconv.ParseInt(items[j].Desired, 10, 64)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "current":
				return func(i int, j int) bool {
					nI, _ := strconv.ParseInt(items[i].Current, 10, 64)
					nJ, _ := strconv.ParseInt(items[j].Current, 10, 64)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "completions":
				return func(i int, j int) bool {
					nI, _ := strconv.ParseInt(items[i].Completions, 10, 64)
					nJ, _ := strconv.ParseInt(items[j].Completions, 10, 64)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "duration":
				return func(i int, j int) bool {
					nI, _ := strfmt.ParseDuration(items[i].Duration)
					nJ, _ := strfmt.ParseDuration(items[j].Duration)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			case "schedule":
				return func(i int, j int) bool {
					less := items[i].Schedule < items[j].Schedule
					if ascend {
						return less
					}
					return !less
				}
			case "lastSchedule":
				return func(i int, j int) bool {
					nI, _ := strfmt.ParseDuration(items[i].LastSchedule)
					nJ, _ := strfmt.ParseDuration(items[j].LastSchedule)
					less := nI < nJ
					if ascend {
						return less
					}
					return !less
				}
			default:
				return func(i int, j int) bool {
					return false
				}
			}
		}
		slice.Sort(items, cmpWrapper(w.State.Sorter.Field, w.State.Sorter.Order))
	}

	w.Data.List = items
	w.State.Total = uint64(len(items))
	return nil
}

func (w *ComponentWorkloadTable) getOperations(obj data.Object, kind, id string) Operate {
	namespace := obj.String("metadata", "namespace")
	restartOperation := LinkOperation{
		Reload: true,
		Key:    "restart",
		Meta: map[string]interface{}{
			"workloadID": id,
		},
		Text:       w.sdk.I18n("restart"),
		Confirm:    w.sdk.I18n("confirmRestart"),
		SuccessMsg: w.sdk.I18n("restartWorkloadSuccessfully"),
	}
	if kind == string(apistructs.K8SJob) || kind == string(apistructs.K8SCronJob) {
		restartOperation.Disabled = true
		restartOperation.DisabledTip = w.sdk.I18n("cannotRestart")
	}

	deleteOperation := LinkOperation{
		Reload: true,
		Key:    "delete",
		Meta: map[string]interface{}{
			"workloadID": id,
		},
		Text:       w.sdk.I18n("delete"),
		Confirm:    w.sdk.I18n("confirmDelete"),
		SuccessMsg: w.sdk.I18n("deletedWorkloadSuccessfully"),
	}
	if namespace == "kube-system" || namespace == "erda-system" || isSystemWorkload(obj) {
		deleteOperation.Disabled = true
		deleteOperation.DisabledTip = w.sdk.I18n("canNotDeleteSystemWorkload")
	}

	return Operate{
		RenderType: "tableOperation",
		Operations: map[string]interface{}{
			"restart": restartOperation,
			"delete":  deleteOperation,
			"checkYaml": LinkOperation{
				Reload: true,
				Key:    "checkYaml",
				Text:   w.sdk.I18n("viewOrEditYaml"),
				Meta: map[string]interface{}{
					"workloadID": id,
				},
			},
		},
	}
}

func isSystemWorkload(workload data.Object) bool {
	nodeSelectorTerms := workload.Slice("spec", "template", "spec", "affinity", "nodeAffinity",
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

func (w *ComponentWorkloadTable) SetComponentValue(ctx context.Context) {
	w.Props.SortDirections = []string{"descend", "ascend"}
	w.Props.RowKey = "workloadId"
	w.Props.PageSizeOptions = []string{"10", "20", "50", "100"}
	w.Props.RequestIgnore = []string{"data"}

	statusColumn := Column{
		DataIndex: "status",
		Title:     cputil.I18n(ctx, "status"),
		Sorter:    true,
	}
	kindColumn := Column{
		DataIndex: "kind",
		Title:     cputil.I18n(ctx, "workloadKind"),
		Sorter:    true,
	}
	nameColumn := Column{
		DataIndex: "name",
		Title:     cputil.I18n(ctx, "name"),
		Sorter:    true,
	}
	ageColumn := Column{
		DataIndex: "age",
		Title:     cputil.I18n(ctx, "age"),
		Sorter:    true,
	}
	readyColumn := Column{
		DataIndex: "ready",
		Title:     cputil.I18n(ctx, "ready"),
		Sorter:    true,
	}
	upToDateColumn := Column{
		DataIndex: "upToDate",
		Title:     cputil.I18n(ctx, "upToDate"),
		Sorter:    true,
	}
	availableColumn := Column{
		DataIndex: "available",
		Title:     cputil.I18n(ctx, "available"),
		Sorter:    true,
	}
	desiredColumn := Column{
		DataIndex: "desired",
		Title:     cputil.I18n(ctx, "desired"),
		Sorter:    true,
	}
	currentColumn := Column{
		DataIndex: "current",
		Title:     cputil.I18n(ctx, "current"),
		Sorter:    true,
	}
	completionsColumn := Column{
		DataIndex: "completions",
		Title:     cputil.I18n(ctx, "completions"),
		Sorter:    true,
	}
	durationColumn := Column{
		DataIndex: "duration",
		Title:     cputil.I18n(ctx, "jobDuration"),
		Sorter:    true,
	}
	scheduleColumn := Column{
		DataIndex: "schedule",
		Title:     cputil.I18n(ctx, "schedule"),
		Sorter:    true,
	}
	lastScheduleColumn := Column{
		DataIndex: "lastSchedule",
		Title:     cputil.I18n(ctx, "lastSchedule"),
		Sorter:    true,
	}
	operateColumn := Column{
		DataIndex: "operations",
		Title:     cputil.I18n(ctx, "operate"),
	}

	if len(w.State.Values.Kind) != 1 {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, ageColumn, operateColumn,
		}
	} else if w.State.Values.Kind[0] == filter.DeploymentType {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, readyColumn, upToDateColumn, availableColumn, ageColumn, operateColumn,
		}
	} else if w.State.Values.Kind[0] == filter.DaemonSetType {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, desiredColumn, currentColumn, readyColumn, upToDateColumn, availableColumn, ageColumn, operateColumn,
		}
	} else if w.State.Values.Kind[0] == filter.StatefulSetType {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, readyColumn, ageColumn, operateColumn,
		}
	} else if w.State.Values.Kind[0] == filter.JobType {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, completionsColumn, durationColumn, ageColumn, operateColumn,
		}
	} else if w.State.Values.Kind[0] == filter.CronJobType {
		w.Props.Columns = []Column{
			nameColumn, statusColumn, kindColumn, scheduleColumn, lastScheduleColumn, ageColumn, operateColumn,
		}
	}

	w.Operations = make(map[string]interface{})
	w.Operations = map[string]interface{}{
		apistructs.OnChangeSortOperation.String(): Operation{
			Key:    apistructs.OnChangeSortOperation.String(),
			Reload: true,
		},
	}
}

func (w *ComponentWorkloadTable) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(w.Props)
	component.Data = map[string]interface{}{
		"list": w.Data.List,
	}
	component.State = map[string]interface{}{
		"clusterName":             w.State.ClusterName,
		"countValues":             w.State.CountValues,
		"pageNo":                  w.State.PageNo,
		"pageSize":                w.State.PageSize,
		"sorterData":              w.State.Sorter,
		"total":                   w.State.Total,
		"values":                  w.State.Values,
		"workloadTable__urlQuery": w.State.WorkloadTableURLQuery,
	}
	component.Operations = w.Operations
}

func getWorkloadKindMap(kinds []string) map[string]struct{} {
	res := make(map[string]struct{})
	for _, kind := range kinds {
		switch kind {
		case filter.DeploymentType:
			res[string(apistructs.K8SDeployment)] = struct{}{}
		case filter.StatefulSetType:
			res[string(apistructs.K8SStatefulSet)] = struct{}{}
		case filter.DaemonSetType:
			res[string(apistructs.K8SDaemonSet)] = struct{}{}
		case filter.JobType:
			res[string(apistructs.K8SJob)] = struct{}{}
		case filter.CronJobType:
			res[string(apistructs.K8SCronJob)] = struct{}{}
		default:
			res[kind] = struct{}{}
		}
	}
	return res
}

func contain(arr []string, target string) bool {
	for _, str := range arr {
		if target == str {
			return true
		}
	}
	return false
}

func getRange(length, pageNo, pageSize int) (int, int) {
	l := (pageNo - 1) * pageSize
	if l >= length || l < 0 {
		l = 0
	}
	r := l + pageSize
	if r > length || r < 0 {
		r = length
	}
	return l, r
}

func getWorkloadID(operationData map[string]interface{}) (string, error) {
	meta, ok := operationData["meta"].(map[string]interface{})
	if !ok {
		return "", errors.New("invalid meta in event.operationData")
	}
	workloadID, ok := meta["workloadID"].(string)
	if !ok {
		return "", errors.New("invalid workload id in event.operationData")
	}
	return workloadID, nil
}
