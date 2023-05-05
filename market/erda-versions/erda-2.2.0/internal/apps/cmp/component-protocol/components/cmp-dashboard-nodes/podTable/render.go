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

package podTable

import (
	"strings"

	"github.com/rancher/wrangler/pkg/data"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
)

var steveServer cmp.SteveServer

func (pt *PodInfoTable) Init(sdk *cptype.SDK) {
	pt.SDK = sdk
}

func (pt *PodInfoTable) GetProps() map[string]interface{} {
	return map[string]interface{}{
		"isLoadMore":     true,
		"rowKey":         "id",
		"sortDirections": []string{"descend", "ascend"},
		"columns": []table.Columns{
			{DataIndex: "Node", Title: pt.SDK.I18n("node"), Sortable: true},
			{DataIndex: "Status", Title: pt.SDK.I18n("status"), Sortable: true},
			{DataIndex: "Usage", Title: pt.SDK.I18n("usedRate"), Sortable: true, Align: "left"},
			{DataIndex: "IP", Title: pt.SDK.I18n("ip"), Sortable: true},
			{DataIndex: "Role", Title: pt.SDK.I18n("Role"), Sortable: true},
			{DataIndex: "Version", Title: pt.SDK.I18n("version"), Sortable: true, Hidden: true},
			{DataIndex: "Operate", Title: pt.SDK.I18n("operate"), Fixed: "right"},
		},
		"selectable":      true,
		"pageSizeOptions": []string{"10", "20", "50", "100"},
		"batchOperations": []string{"cordon", "uncordon", "drain"},
	}
}

func (pt *PodInfoTable) GetRowItems(nodes []data.Object, requests map[string]cmp.AllocatedRes) ([]table.RowItem, error) {
	var (
		status *table.SteveStatus
		items  []table.RowItem
		err    error
	)
	clusterName := ""
	if pt.SDK.InParams["clusterName"] != nil {
		clusterName = pt.SDK.InParams["clusterName"].(string)
	} else {
		return nil, common.ClusterNotFoundErr
	}
	nodesAllocatedRes, err := cmp.GetNodesAllocatedRes(pt.Ctx, steveServer, false, clusterName, pt.SDK.Identity.UserID, pt.SDK.Identity.OrgID, nodes)
	if err != nil {
		return nil, err
	}
	for _, c := range nodes {
		status, err = pt.GetItemStatus(c)
		if err != nil {
			return nil, err
		}
		if status, err = pt.GetItemStatus(c); err != nil {
			return nil, err
		}
		nodeName := c.StringSlice("metadata", "fields")[0]
		pod := nodesAllocatedRes[nodeName].PodNum
		capacityPodsQty, _ := resource.ParseQuantity(c.String("status", "allocatable", "pods"))
		ur := table.DistributionValue{Percent: common.GetPercent(float64(pod), float64(capacityPodsQty.Value()))}
		roleStr := c.StringSlice("metadata", "fields")[2]
		ip := c.StringSlice("metadata", "fields")[5]
		if roleStr == "<none>" {
			roleStr = "worker"
		}
		batchOperations := make([]string, 0)
		if !strings.Contains(roleStr, "master") {
			if c.String("spec", "unschedulable") == "true" {
				if !table.IsNodeOffline(c) {
					batchOperations = append(batchOperations, "uncordon")
				}
			} else {
				batchOperations = append(batchOperations, "cordon")
			}
		}
		if roleStr == "worker" && !table.IsNodeLabelInBlacklist(c) {
			//if !table.IsNodeOffline(c) {
			batchOperations = append(batchOperations, "drain")
			//	if c.String("spec", "unschedulable") == "true" && !table.IsNodeOffline(c) {
			//		batchOperations = append(batchOperations, "offline")
			//	}
			//} else {
			//	batchOperations = append(batchOperations, "online")
			//}
		}

		role := table.Role{
			RenderType: "tagsRow",
			Value:      table.RoleValue{Label: roleStr},
			Size:       "normal",
		}

		items = append(items, table.RowItem{
			ID:      c.String("metadata", "name"),
			IP:      ip,
			NodeID:  c.String("metadata", "name"),
			Version: c.String("status", "nodeInfo", "kubeletVersion"),
			Role:    role,
			Node: table.Node{
				RenderType: "multiple",
				Direction:  "row",
				Renders:    pt.GetRenders(c.String("metadata", "name"), c.Map("metadata", "labels")),
			},
			Status: *status,
			Usage: table.Distribution{
				RenderType: "progress",
				Value:      ur.Percent,
				Status:     table.GetDistributionStatus(ur.Percent),
				Tip:        pt.GetScaleValue(float64(pod), float64(capacityPodsQty.Value()), table.Pod),
			},
			Operate:         pt.GetOperate(c.String("metadata", "name")),
			BatchOperations: batchOperations,
		})
	}

	return items, nil
}
