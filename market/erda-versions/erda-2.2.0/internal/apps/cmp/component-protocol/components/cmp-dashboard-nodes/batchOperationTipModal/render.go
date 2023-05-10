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

package batchOperationTipModal

import (
	"context"
	"fmt"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

var steveServer cmp.SteveServer

func (bot *BatchOperationTipModal) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (bot *BatchOperationTipModal) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	bot.CtxBdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	bot.SDK = cputil.SDK(ctx)
	bot.ctx = ctx
	err := common.Transfer(c.State, &bot.State)
	if err != nil {
		return err
	}
	err = common.Transfer(c.Operations, &bot.Operations)
	if err != nil {
		return err
	}
	switch event.Operation {
	case cptype.InitializeOperation:
		bot.getOperations()
		err := common.Transfer(bot.Operations, &c.Operations)
		if err != nil {
			return err
		}
		bot.getProps()
		err = common.Transfer(bot.Props, &c.Props)
		if err != nil {
			return err
		}
		return nil
	case cptype.RenderingOperation:
		bot.State.Visible = false
		operationKey := (*gs)["OperationKey"]
		if operationKey == nil {
			break
		}
		switch operationKey {
		case common.CMPDashboardCordonNode:
			bot.State.Visible = true
			selectRowKeys := (*gs)["SelectedRowKeys"].([]string)
			bot.State.SelectedRowKeys = selectRowKeys
			ops := Operation{}
			err := common.Transfer(bot.Operations["onOk"], &ops)
			if err != nil {
				return err
			}
			ops.Meta.Type = common.CMPDashboardCordonNode
			bot.Operations["onOk"] = ops
			bot.Props.Content = bot.getContent(bot.SDK.I18n("Cordon following nodes"), selectRowKeys)
		case common.CMPDashboardUncordonNode:
			bot.State.Visible = true
			selectRowKeys := (*gs)["SelectedRowKeys"].([]string)
			bot.State.SelectedRowKeys = selectRowKeys
			ops := Operation{}
			err := common.Transfer(bot.Operations["onOk"], &ops)
			if err != nil {
				return err
			}
			ops.Meta.Type = common.CMPDashboardUncordonNode
			bot.Operations["onOk"] = ops
			bot.Props.Content = bot.getContent(bot.SDK.I18n("Uncordon following nodes"), selectRowKeys)
		case common.CMPDashboardDrainNode:
			bot.State.Visible = true
			selectRowKeys := (*gs)["SelectedRowKeys"].([]string)
			bot.State.SelectedRowKeys = selectRowKeys
			ops := Operation{}
			err := common.Transfer(bot.Operations["onOk"], &ops)
			if err != nil {
				return err
			}
			ops.Meta.Type = common.CMPDashboardDrainNode
			bot.Operations["onOk"] = ops
			bot.Props.Content = bot.getContent(bot.SDK.I18n("Drain following nodes"), selectRowKeys)
		case common.CMPDashboardOfflineNode:
			bot.State.Visible = true
			selectRowKeys := (*gs)["SelectedRowKeys"].([]string)
			bot.State.SelectedRowKeys = selectRowKeys
			ops := Operation{}
			err := common.Transfer(bot.Operations["onOk"], &ops)
			if err != nil {
				return err
			}
			ops.Meta.Type = common.CMPDashboardOfflineNode
			bot.Operations["onOk"] = ops
			content, err := bot.getOfflineContent(selectRowKeys)
			if err != nil {
				return err
			}
			bot.Props.Content = content
		case common.CMPDashboardOnlineNode:
			bot.State.Visible = true
			selectRowKeys := (*gs)["SelectedRowKeys"].([]string)
			bot.State.SelectedRowKeys = selectRowKeys
			ops := Operation{}
			err := common.Transfer(bot.Operations["onOk"], &ops)
			if err != nil {
				return err
			}
			ops.Meta.Type = common.CMPDashboardOnlineNode
			bot.Operations["onOk"] = ops
			bot.Props.Content = bot.getContent(bot.SDK.I18n("Online following nodes"), selectRowKeys)
		}
	case common.CMPDashboardBatchSubmit:
		bot.State.Visible = false
		selectRowKeys := bot.State.SelectedRowKeys
		ops := Operation{}
		err := common.Transfer(bot.Operations["onOk"], &ops)
		if err != nil {
			return err
		}
		switch ops.Meta.Type {
		case common.CMPDashboardCordonNode:
			err := bot.CordonNode(selectRowKeys)
			if err != nil {
				return err
			}
		case common.CMPDashboardUncordonNode:
			err := bot.UncordonNode(selectRowKeys)
			if err != nil {
				return err
			}
		case common.CMPDashboardDrainNode:
			err := bot.DrainNode(selectRowKeys)
			if err != nil {
				return err
			}
		case common.CMPDashboardOfflineNode:
			err := bot.OfflineNode(selectRowKeys)
			if err != nil {
				return err
			}
		case common.CMPDashboardOnlineNode:
			err := bot.OnlineNode(selectRowKeys)
			if err != nil {
				return err
			}
		}
	}
	return bot.SetComponent(c)
}

func (bot *BatchOperationTipModal) SetComponent(c *cptype.Component) error {
	err := common.Transfer(bot.Props, &c.Props)
	if err != nil {
		return err
	}
	err = common.Transfer(bot.State, &c.State)
	if err != nil {
		return err
	}
	err = common.Transfer(bot.Operations, &c.Operations)
	if err != nil {
		return err
	}
	return nil
}

func (bot *BatchOperationTipModal) CordonNode(nodeIDs []string) error {
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		name := splits[0]
		req := &apistructs.SteveRequest{
			UserID:      bot.SDK.Identity.UserID,
			OrgID:       bot.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: bot.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		err := steveServer.CordonNode(bot.ctx, req)
		if err != nil {
			return err
		}
	}
	return nil
}

func (bot *BatchOperationTipModal) UncordonNode(nodeIDs []string) error {
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		name := splits[0]
		req := &apistructs.SteveRequest{
			UserID:      bot.SDK.Identity.UserID,
			OrgID:       bot.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: bot.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		err := steveServer.UnCordonNode(bot.ctx, req)
		if err != nil {
			return err
		}
	}
	return nil
}

func (bot *BatchOperationTipModal) DrainNode(nodeIDs []string) error {
	if len(nodeIDs) == 0 {
		return nil
	}
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		name := splits[0]
		req := &apistructs.SteveRequest{
			UserID:      bot.SDK.Identity.UserID,
			OrgID:       bot.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: bot.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		if err := steveServer.DrainNode(bot.ctx, req); err != nil {
			return err
		}
	}
	return nil
}

func (bot *BatchOperationTipModal) OfflineNode(nodeIDs []string) error {
	return steveServer.OfflineNode(bot.ctx, bot.SDK.Identity.UserID, bot.SDK.Identity.OrgID,
		bot.SDK.InParams["clusterName"].(string), nodeIDs)
}

func (bot *BatchOperationTipModal) OnlineNode(nodeIDs []string) error {
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		name := splits[0]
		req := &apistructs.SteveRequest{
			UserID:      bot.SDK.Identity.UserID,
			OrgID:       bot.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: bot.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		err := steveServer.OnlineNode(bot.ctx, req)
		if err != nil {
			return err
		}
	}
	return nil
}

func (bot *BatchOperationTipModal) getProps() {
	bot.Props = Props{
		Status:  "warning",
		Content: "",
		Title:   bot.SDK.I18n("warning"),
	}
}

func (bot *BatchOperationTipModal) getOperations() {
	bot.Operations = map[string]interface{}{
		"onOk": Operation{
			Key:        "batchSubmit",
			Reload:     true,
			SuccessMsg: bot.SDK.I18n("status update success"),
			Meta:       Meta{Type: ""},
		},
	}
}

func (bot *BatchOperationTipModal) getContent(tip string, nodeIDs []string) string {
	content := fmt.Sprintf("%s\n", tip)
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		content += fmt.Sprintf("%s\n", splits[0])
	}
	return content
}

func (bot *BatchOperationTipModal) getOfflineContent(nodeIDs []string) (string, error) {
	req := &apistructs.SteveRequest{
		UserID:      bot.SDK.Identity.UserID,
		OrgID:       bot.SDK.Identity.OrgID,
		Type:        apistructs.K8SPod,
		ClusterName: bot.SDK.InParams["clusterName"].(string),
	}
	list, err := steveServer.ListSteveResource(bot.ctx, req)
	if err != nil {
		return "", err
	}

	nodeDrained := map[string]bool{}
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		nodeDrained[splits[0]] = true
	}

	for _, obj := range list {
		pod := obj.Data()
		fields := pod.StringSlice("metadata", "fields")
		status := fields[2]
		nodeName := fields[6]
		if _, ok := nodeDrained[nodeName]; ok && status != "Evicted" && status != "Succeed" && status != "Failed" {
			nodeDrained[nodeName] = false
		}
	}

	content := bot.SDK.I18n("Offline following nodes") + "\n"
	for node, isDrained := range nodeDrained {
		if isDrained {
			content += fmt.Sprintf("%s\n", node)
		} else {
			content += fmt.Sprintf("%s (%s)\n", node, bot.SDK.I18n("undrained"))
		}
	}
	return content, nil
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodes", "batchOperationTipModal", func() servicehub.Provider {
		return &BatchOperationTipModal{Type: "Modal"}
	})
}
