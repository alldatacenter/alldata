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

package charts

import (
	"context"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/chart"
	cputil2 "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
)

var steveServer cmp.SteveServer
var mServer metrics.Interface

func (cht *Charts) Init(ctx servicehub.Context) error {
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
func (cht Charts) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	cht.Props = Props{
		Gutter: 8,
	}
	c.Props = cputil.MustConvertProps(cht.Props)
	cht.SDK = cputil.SDK(ctx)

	clusterName := ""
	if cht.SDK.InParams["clusterName"] != nil {
		clusterName = cht.SDK.InParams["clusterName"].(string)
	} else {
		return common.ClusterNotFoundErr
	}

	nodes := (*gs)["nodes"].([]data.Object)
	nodesAllocatedRes, err := cmp.GetNodesAllocatedRes(ctx, steveServer, false, clusterName, cht.SDK.Identity.UserID, cht.SDK.Identity.OrgID, nodes)
	if err != nil {
		return err
	}
	resourceNames := []string{chart.CPU, chart.Memory, chart.Pods}
	for _, resourceName := range resourceNames {
		resourceType := resource.DecimalSI
		if resourceName == chart.Memory {
			resourceType = resource.BinarySI
		}
		requestQuantity := resource.NewQuantity(0, resourceType)
		unAllocatableQuantity := resource.NewQuantity(0, resourceType)
		leftQuantity := resource.NewQuantity(0, resourceType)
		if len(nodes) == 0 {
			(*gs)[resourceName+"Chart"] = []chart.DataItem{}
		}
		for _, node := range nodes {
			if cmp.IsVirtualNode(node) {
				continue
			}
			nodeName := node.StringSlice("metadata", "fields")[0]
			nar := nodesAllocatedRes[nodeName]
			cpu, mem, pod := nar.CPU, nar.Mem, nar.PodNum
			switch resourceName {
			case chart.CPU:
				unallocatedCPU, _, leftCPU, _, _ := cputil2.CalculateNodeRes(node, cpu, 0, 0)
				unAllocatableQuantity.Add(*resource.NewMilliQuantity(unallocatedCPU, resource.DecimalSI))
				leftQuantity.Add(*resource.NewMilliQuantity(leftCPU, resource.DecimalSI))
				requestQuantity.Add(*resource.NewMilliQuantity(cpu, resource.DecimalSI))
			case chart.Memory:
				_, unallocatedMem, _, leftMem, _ := cputil2.CalculateNodeRes(node, 0, mem, 0)
				unAllocatableQuantity.Add(*resource.NewQuantity(unallocatedMem, resource.BinarySI))
				leftQuantity.Add(*resource.NewQuantity(leftMem, resource.BinarySI))
				requestQuantity.Add(*resource.NewQuantity(mem, resource.BinarySI))
			case chart.Pods:
				_, _, _, _, leftPods := cputil2.CalculateNodeRes(node, 0, 0, pod)
				leftQuantity.Add(*resource.NewQuantity(leftPods, resource.DecimalSI))
				requestQuantity.Add(*resource.NewQuantity(pod, resource.DecimalSI))
			}
		}
		var requestStr, leftStr, unAllocatableStr string
		var requestValue, leftValue, unAllocatableValue float64
		switch resourceName {
		case chart.CPU:
			requestStr = cputil2.ResourceToString(cht.SDK, float64(requestQuantity.MilliValue()), resource.DecimalSI)
			leftStr = cputil2.ResourceToString(cht.SDK, float64(leftQuantity.MilliValue()), resource.DecimalSI)
			unAllocatableStr = cputil2.ResourceToString(cht.SDK, float64(unAllocatableQuantity.MilliValue()), resource.DecimalSI)
		case chart.Memory:
			requestStr = cputil2.ResourceToString(cht.SDK, float64(requestQuantity.Value()), resource.BinarySI)
			leftStr = cputil2.ResourceToString(cht.SDK, float64(leftQuantity.Value()), resource.BinarySI)
			unAllocatableStr = cputil2.ResourceToString(cht.SDK, float64(unAllocatableQuantity.Value()), resource.BinarySI)
		case chart.Pods:
			requestStr = cputil2.ResourceToString(cht.SDK, float64(requestQuantity.Value()), "")
			leftStr = cputil2.ResourceToString(cht.SDK, float64(leftQuantity.Value()), "")
			unAllocatableStr = cputil2.ResourceToString(cht.SDK, float64(unAllocatableQuantity.Value()), "")
		}
		requestValue = float64(requestQuantity.MilliValue()) / 1000
		leftValue = float64(leftQuantity.MilliValue()) / 1000
		unAllocatableValue = float64(unAllocatableQuantity.MilliValue()) / 1000
		var di []chart.DataItem
		if requestValue != 0 {
			di = append(di, chart.DataItem{
				Value:     requestValue,
				Name:      cht.SDK.I18n(chart.Allocated),
				Formatter: requestStr,
				Color:     "primary8",
			})
		}
		if leftValue != 0 {
			di = append(di, chart.DataItem{
				Value:     leftValue,
				Name:      cht.SDK.I18n(chart.Free_Allocate),
				Formatter: leftStr,
				Color:     "primary5",
			})
		}
		if unAllocatableValue != 0 {
			di = append(di, chart.DataItem{
				Value:     unAllocatableValue,
				Name:      cht.SDK.I18n(chart.Cannot_Allocate),
				Formatter: unAllocatableStr,
				Color:     "primary2",
			})
		}
		(*gs)[resourceName+"Chart"] = di
	}
	return nil
}
func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodes", "charts", func() servicehub.Provider {
		return &Charts{
			Type: "Grid",
		}
	})
}
