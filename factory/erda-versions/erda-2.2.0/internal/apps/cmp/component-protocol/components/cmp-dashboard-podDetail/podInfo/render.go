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

package PodInfo

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

var steveServer cmp.SteveServer

func (podInfo *PodInfo) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (podInfo *PodInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := podInfo.GenComponentState(c); err != nil {
		return err
	}
	podInfo.server = steveServer
	podInfo.ctx = ctx
	podInfo.SDK = cputil.SDK(ctx)

	splits := strings.Split(podInfo.State.PodID, "_")
	if len(splits) != 2 {
		return fmt.Errorf("invaild pod id: %s", podInfo.State.PodID)
	}
	namespace, name := splits[0], splits[1]

	userID := podInfo.SDK.Identity.UserID
	orgID := podInfo.SDK.Identity.OrgID
	req := &apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        apistructs.K8SPod,
		ClusterName: podInfo.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}
	resp, err := podInfo.server.GetSteveResource(ctx, req)
	if err != nil {
		return err
	}
	obj := resp.Data()
	fields := obj.StringSlice("metadata", "fields")
	if len(fields) != 9 {
		return fmt.Errorf("pod %s/%s has invalid length of fields", namespace, name)
	}
	workloadId, err := podInfo.getWorkloadID(obj)
	if err != nil {
		return err
	}
	podInfo.Props = podInfo.getProps(obj, workloadId)

	data := Data{
		Namespace:   namespace,
		Age:         fields[4],
		Ip:          fields[5],
		Workload:    workloadId,
		Node:        fields[6],
		Labels:      podInfo.getTags(obj, "labels"),
		Annotations: podInfo.getTags(obj, "annotations"),
	}
	podInfo.Data = map[string]Data{
		"data": data,
	}
	podInfo.Transfer(c)
	return nil
}

func (podInfo *PodInfo) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}

	data, err := json.Marshal(component.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(data, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	podInfo.State = state
	return nil
}

func (podInfo *PodInfo) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(podInfo.Props)
	c.Data = map[string]interface{}{}
	for k, v := range podInfo.Data {
		c.Data[k] = v
	}
	c.State = map[string]interface{}{
		"clusterName": podInfo.State.ClusterName,
		"podId":       podInfo.State.PodID,
	}
}

func (podInfo *PodInfo) getProps(pod data.Object, workloadId string) Props {
	var workloadField Field
	if workloadId != "" {
		workloadField = Field{
			Label:      podInfo.SDK.I18n("workload"),
			ValueKey:   "workload",
			RenderType: "linkText",
			Operations: map[string]Operation{
				"click": {
					Key:    "gotoWorkloadDetail",
					Reload: false,
					Command: Command{
						Key:    "goto",
						Target: "cmpClustersWorkloadDetail",
						State: CommandState{Params: map[string]string{
							"workloadId": workloadId,
						}},
						JumpOut: true,
					},
				},
			},
		}
	} else {
		workloadField = Field{
			Label:    podInfo.SDK.I18n("workload"),
			ValueKey: "workload",
		}
	}
	return Props{
		RequestIgnore: []string{"data"},
		ColumnNum:     4,
		Fields: []Field{
			{Label: podInfo.SDK.I18n("namespace"), ValueKey: "namespace"},
			{Label: podInfo.SDK.I18n("age"), ValueKey: "age"},
			{Label: podInfo.SDK.I18n("podIP"), ValueKey: "ip"},
			workloadField,
			{Label: podInfo.SDK.I18n("node"), ValueKey: "node", RenderType: "linkText",
				Operations: map[string]Operation{
					"click": {
						Key:    "gotoNodeDetail",
						Reload: false,
						Command: Command{
							Key:    "goto",
							Target: "cmpClustersNodeDetail",
							State: CommandState{
								Params: map[string]string{
									"nodeId": pod.String("spec", "nodeName"),
								},
								Query: map[string]string{
									"nodeIP": pod.String("status", "hostIP"),
								},
							},
							JumpOut: true,
						},
					},
				}},
			{
				Label:      podInfo.SDK.I18n("labels"),
				ValueKey:   "labels",
				RenderType: "tagsRow",
				SpaceNum:   2,
			},
			{Label: podInfo.SDK.I18n("annotations"), ValueKey: "annotations", SpaceNum: 2, RenderType: "tagsRow"},
		},
	}
}

func (podInfo *PodInfo) getTags(pod data.Object, kind string) []Tag {
	var tags []Tag
	for k, v := range pod.Map("metadata", kind) {
		tags = append(tags, Tag{
			Label: fmt.Sprintf("%s=%s", k, v),
		})
	}
	return tags
}

func (podInfo *PodInfo) getWorkloadID(pod data.Object) (string, error) {
	ownerReferences := pod.Slice("metadata", "ownerReferences")
	if len(ownerReferences) == 0 {
		return "", nil
	}
	ownerReference := ownerReferences[0]
	kind := ownerReference.String("kind")
	if kind != "Deployment" && kind != "StatefulSet" && kind != "ReplicaSet" && kind != "DaemonSet" &&
		kind != "Job" && kind != "CronJob" {
		return "", nil
	}
	name := ownerReference.String("name")
	namespace := pod.String("metadata", "namespace")

	if kind == "ReplicaSet" {
		req := &apistructs.SteveRequest{
			UserID:      podInfo.SDK.Identity.UserID,
			OrgID:       podInfo.SDK.Identity.OrgID,
			Type:        apistructs.K8SReplicaSet,
			ClusterName: podInfo.State.ClusterName,
			Name:        name,
			Namespace:   namespace,
		}

		resp, err := podInfo.server.GetSteveResource(podInfo.ctx, req)
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

func init() {
	base.InitProviderWithCreator("cmp-dashboard-podDetail", "podInfo", func() servicehub.Provider {
		return &PodInfo{}
	})
}
