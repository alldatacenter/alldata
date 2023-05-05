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

package table

import (
	"context"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/cpuTable"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/memTable"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/podTable"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
)

var (
	steveServer cmp.SteveServer
	mServer     metrics.Interface
)

func (t *Table) initTables() {
	mt := &memTable.MemInfoTable{}
	mt.Init(t.SDK)
	t.MemTable = mt
	ct := &cpuTable.CpuInfoTable{}
	ct.Init(t.SDK)
	t.CpuTable = ct
	pt := &podTable.PodInfoTable{}
	pt.Init(t.SDK)
	t.PodTable = pt
}
func (t *Table) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	mserver, ok := ctx.Service("cmp").(metrics.Interface)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a metrics server")
	}
	steveServer = server
	mServer = mserver
	return nil
}

func (t *Table) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	t.SDK = cputil.SDK(ctx)
	t.initTables()
	err := common.Transfer(c.State, &t.State)
	if err != nil {
		return err
	}
	t.Operations = t.GetTableOperation()
	t.Ctx = ctx
	t.Table.Server = steveServer
	t.Table.Metrics = mServer
	activeKey := (*gs)["activeKey"].(string)
	// Tab name not equal this component name
	if event.Operation != cptype.InitializeOperation {
		switch event.Operation {
		//case common.CMPDashboardChangePageSizeOperationKey, common.CMPDashboardChangePageNoOperationKey:
		case common.CMPDashboardSortByColumnOperationKey:
		case common.CMPDashboardRemoveLabel:
			metaName := event.OperationData["fillMeta"].(string)
			label := event.OperationData["meta"].(map[string]interface{})[metaName].(map[string]interface{})["label"].(string)
			labelKey := strings.Split(label, "=")[0]
			nodeId := event.OperationData["meta"].(map[string]interface{})["recordId"].(string)
			req := apistructs.SteveRequest{}
			req.ClusterName = t.SDK.InParams["clusterName"].(string)
			req.OrgID = t.SDK.Identity.OrgID
			req.UserID = t.SDK.Identity.UserID
			req.Type = apistructs.K8SNode
			req.Name = nodeId
			err = steveServer.UnlabelNode(ctx, &req, []string{labelKey})
		case common.CMPDashboardUncordonNode:
			(*gs)["SelectedRowKeys"] = t.State.SelectedRowKeys
			(*gs)["OperationKey"] = common.CMPDashboardUncordonNode
		case common.CMPDashboardCordonNode:
			(*gs)["SelectedRowKeys"] = t.State.SelectedRowKeys
			(*gs)["OperationKey"] = common.CMPDashboardCordonNode
		case common.CMPDashboardDrainNode:
			(*gs)["SelectedRowKeys"] = t.State.SelectedRowKeys
			(*gs)["OperationKey"] = common.CMPDashboardDrainNode
		case common.CMPDashboardOfflineNode:
			(*gs)["SelectedRowKeys"] = t.State.SelectedRowKeys
			(*gs)["OperationKey"] = common.CMPDashboardOfflineNode
		case common.CMPDashboardOnlineNode:
			(*gs)["SelectedRowKeys"] = t.State.SelectedRowKeys
			(*gs)["OperationKey"] = common.CMPDashboardOnlineNode
		default:
			logrus.Warnf("operation [%s] not support, scenario:%v, event:%v", event.Operation, s, event)
		}
	}

	if err = t.RenderList(c, table.TableType(activeKey), gs); err != nil {
		return err
	}
	if err = t.SetComponentValue(c); err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodes", "table", func() servicehub.Provider {
		cc := &Table{}
		return cc
	})
}
