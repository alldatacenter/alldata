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

package cpuTable

import (
	"strings"

	"github.com/rancher/wrangler/pkg/data"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/cache"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
)

func (ct *CpuInfoTable) Init(sdk *cptype.SDK) {
	ct.SDK = sdk
}

func (ct *CpuInfoTable) GetProps() map[string]interface{} {
	return map[string]interface{}{
		"isLoadMore":     true,
		"rowKey":         "id",
		"sortDirections": []string{"descend", "ascend"},
		"columns": []table.Columns{
			{DataIndex: "Node", Title: ct.SDK.I18n("node"), Sortable: true},
			{DataIndex: "Status", Title: ct.SDK.I18n("status"), Sortable: true},
			{DataIndex: "Distribution", Title: ct.SDK.I18n("distribution"), Sortable: true, Align: "left"},
			{DataIndex: "Usage", Title: ct.SDK.I18n("usedRate"), Sortable: true, Align: "left"},
			{DataIndex: "DistributionRate", Title: ct.SDK.I18n("distributionRate"), Sortable: true, TitleTip: ct.SDK.I18n("The proportion of allocated resources that are used"), Hidden: true},
			{DataIndex: "IP", Title: ct.SDK.I18n("ip"), Sortable: true},
			{DataIndex: "Role", Title: ct.SDK.I18n("Role"), Sortable: true},
			{DataIndex: "Version", Title: ct.SDK.I18n("version"), Sortable: true, Hidden: true},
			{DataIndex: "Operate", Title: ct.SDK.I18n("operate"), Fixed: "right"},
		},
		"selectable":      true,
		"pageSizeOptions": []string{"10", "20", "50", "100"},
		"batchOperations": []string{"cordon", "uncordon", "drain"},
	}
}

func (ct *CpuInfoTable) GetRowItems(nodes []data.Object, requests map[string]cmp.AllocatedRes) ([]table.RowItem, error) {
	var (
		err                 error
		status              *table.SteveStatus
		distribution, usage table.DistributionValue
		clusterName         string
		nodeLabels          []string
		items               []table.RowItem
	)
	if ct.SDK.InParams["clusterName"] != nil {
		clusterName = ct.SDK.InParams["clusterName"].(string)
	} else {
		return nil, common.ClusterNotFoundErr
	}
	for _, c := range nodes {
		nodeLabelsData := c.Map("metadata", "labels")

		for k := range nodeLabelsData {
			nodeLabels = append(nodeLabels, k)
		}
		if status, err = ct.GetItemStatus(c); err != nil {
			return nil, err
		}
		Ip := c.StringSlice("metadata", "fields")[5]
		//request := c.Map("status", "allocatable").String("cpu")
		nodeName := c.StringSlice("metadata", "fields")[0]
		requestQty, _ := resource.ParseQuantity(c.String("status", "allocatable", "cpu"))
		cpuRequest := requests[nodeName].CPU

		distribution = ct.GetDistributionValue(float64(cpuRequest), float64(requestQty.ScaledValue(resource.Milli)), table.Cpu)
		key := cache.GenerateKey(metrics.Cpu, metrics.Node, clusterName, Ip)
		metricsData := metrics.GetCache(key)
		used := 0.0
		if metricsData != nil {
			used = metricsData.Used
		}
		usage = ct.GetUsageValue(used*1000, float64(requestQty.Value())*1000, table.Cpu)
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
			ID:      c.String("metadata", "name") + "/" + ip,
			IP:      ip,
			NodeID:  c.String("metadata", "name"),
			Version: c.String("status", "nodeInfo", "kubeletVersion"),
			Role:    role,
			Node: table.Node{
				RenderType: "multiple",
				Direction:  "row",
				Renders:    ct.GetRenders(c.String("metadata", "name"), c.Map("metadata", "labels")),
			},
			Status: *status,
			Distribution: table.Distribution{
				RenderType: "progress",
				Value:      distribution.Percent,
				Status:     table.GetDistributionStatus(distribution.Percent),
				Tip:        distribution.Text,
			},
			Usage: table.Distribution{
				RenderType: "progress",
				Value:      usage.Percent,
				Status:     table.GetDistributionStatus(usage.Percent),
				Tip:        usage.Text,
			},
			DistributionRate: ct.GetDistributionRate(used*1000, float64(cpuRequest), table.Cpu),
			Operate:          ct.GetOperate(c.String("metadata", "name")),
			BatchOperations:  batchOperations,
		},
		)
	}
	return items, nil
}
