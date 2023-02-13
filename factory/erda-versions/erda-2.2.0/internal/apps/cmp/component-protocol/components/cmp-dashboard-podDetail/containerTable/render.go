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

package ContainerTable

import (
	"context"
	"encoding/json"
	"fmt"
	"sort"
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

func (containerTable *ContainerTable) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (containerTable *ContainerTable) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := containerTable.GenComponentState(c); err != nil {
		return err
	}
	sdk := cputil.SDK(ctx)

	userID := sdk.Identity.UserID
	orgID := sdk.Identity.OrgID

	splits := strings.Split(containerTable.State.PodID, "_")
	if len(splits) != 2 {
		return fmt.Errorf("invalid pod id: %s", containerTable.State.PodID)
	}

	namespace, name := splits[0], splits[1]
	req := &apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        apistructs.K8SPod,
		ClusterName: containerTable.State.ClusterName,
		Name:        name,
		Namespace:   namespace,
	}

	resp, err := steveServer.GetSteveResource(ctx, req)
	if err != nil {
		return err
	}
	obj := resp.Data()

	var datas []Data
	containerStatuses := obj.Slice("status", "containerStatuses")
	for _, containerStatus := range containerStatuses {
		states := containerStatus.Map("state")
		status := Status{}
		for k := range states {
			status = parseContainerStatus(ctx, k)
		}

		containerId := getContainerID(containerStatus.String("containerID"))
		restartCountStr := containerStatus.String("restartCount") + " " + cputil.I18n(ctx, "times")
		var restartCount interface{}
		lastContainerState := containerStatus.Map("lastState")
		for _, v := range lastContainerState {
			lastState, err := data.Convert(v)
			if err != nil {
				continue
			}
			lastContainerID := getContainerID(lastState.String("containerID"))
			if lastContainerID != "" {
				restartCount = Operate{
					Operations: map[string]Operation{
						"click": {
							Key:    "checkPrevLog",
							Reload: false,
							Meta: map[string]interface{}{
								"hasRestarted":  true,
								"containerName": containerStatus.String("name"),
								"podName":       name,
								"namespace":     namespace,
								"containerId":   lastContainerID,
							},
						},
					},
					RenderType: "linkText",
					Value:      restartCountStr,
				}
				break
			}
		}
		if restartCount == nil {
			restartCount = restartCountStr
		}

		datas = append(datas, Data{
			Status: status,
			Ready:  containerStatus.String("ready"),
			Name:   containerStatus.String("name"),
			Images: Images{
				RenderType: "copyText",
				Value: Value{
					Text: containerStatus.String("image"),
				},
			},
			RestartCount: restartCount,
			Operate: Operate{
				Operations: map[string]Operation{
					"log": {
						Key:    "checkLog",
						Text:   cputil.I18n(ctx, "log"),
						Reload: false,
						Meta: map[string]interface{}{
							"containerName": containerStatus.String("name"),
							"podName":       name,
							"namespace":     namespace,
							"containerId":   containerId,
						},
					},
					"console": {
						Key:    "checkConsole",
						Text:   cputil.I18n(ctx, "console"),
						Reload: false,
						Meta: map[string]interface{}{
							"containerName": containerStatus.String("name"),
							"podName":       name,
							"namespace":     namespace,
						},
					},
				},
				RenderType: "tableOperation",
			},
		})
	}
	sort.Slice(datas, func(i, j int) bool {
		return datas[i].Name < datas[j].Name
	})
	containerTable.Data = map[string][]Data{
		"list": datas,
	}

	containerTable.Props.SortDirections = []string{"descend", "ascend"}
	containerTable.Props.RequestIgnore = []string{"data"}
	containerTable.Props.RowKey = "name"
	containerTable.Props.Pagination = false
	containerTable.Props.Scroll.X = 1000
	containerTable.Props.Columns = []Column{
		{
			DataIndex: "status",
			Title:     cputil.I18n(ctx, "status"),
		},
		{
			DataIndex: "ready",
			Title:     cputil.I18n(ctx, "ready"),
		},
		{
			DataIndex: "name",
			Title:     cputil.I18n(ctx, "name"),
		},
		{
			DataIndex: "images",
			Title:     cputil.I18n(ctx, "images"),
		},
		{
			DataIndex: "restartCount",
			Title:     cputil.I18n(ctx, "restartCount"),
		},
		{
			DataIndex: "operate",
			Title:     cputil.I18n(ctx, "operate"),
		},
	}
	containerTable.Transfer(c)
	return nil
}

func (containerTable *ContainerTable) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}

	jsonData, err := json.Marshal(component.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(jsonData, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	containerTable.State = state
	return nil
}

func (containerTable *ContainerTable) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(containerTable.Props)
	component.Data = map[string]interface{}{}
	for k, v := range containerTable.Data {
		component.Data[k] = v
	}
	component.State = map[string]interface{}{
		"clusterName": containerTable.State.ClusterName,
		"podId":       containerTable.State.PodID,
	}
}

func parseContainerStatus(ctx context.Context, s string) Status {
	color := ""
	breathing := false
	switch s {
	case "running":
		color = "success"
		breathing = true
	case "waiting":
		color = "processing"
	case "terminated":
		color = "error"
	}
	return Status{
		RenderType: "textWithBadge",
		Value:      cputil.I18n(ctx, s),
		Status:     color,
		Breathing:  breathing,
	}
}

func getContainerID(id string) string {
	splits := strings.Split(id, "://")
	if len(splits) != 2 {
		return id
	}
	return splits[1]
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-podDetail", "containerTable", func() servicehub.Provider {
		return &ContainerTable{}
	})
}
