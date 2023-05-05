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

package org

import (
	"context"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

func (o *Org) FetchOrgClusterResource(ctx context.Context, orgID uint64) (*apistructs.OrgClustersResourcesInfo, error) {
	clusters, err := o.db.GetOrgClusterRelationsByOrg(orgID)
	if err != nil {
		return nil, err
	}

	var getClustersResourcesRequest dashboardPb.GetClustersResourcesRequest
	for _, cluster := range clusters {
		getClustersResourcesRequest.ClusterNames = append(getClustersResourcesRequest.ClusterNames, cluster.ClusterName)
	}
	// cmp gRPC 接口查询给定集群所有集群的资源和标签情况
	resources, err := o.cmp.GetClustersResources(ctx, &getClustersResourcesRequest)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to GetClusterResources, clusters: %v", getClustersResourcesRequest.GetClusterNames())
	}

	// 初始化所有集群的总资源 【】
	var (
		// 集群 nodes 数量
		clusterNodes = make(map[string]int)
		// 集群里资源计算器
		calculators     = make(map[string]*calcu.Calculator)
		requestResource = make(map[string]*calcu.Calculator)
	)
	countClustersNodes(clusterNodes, resources.List)
	initClusterAllocatable(calculators, resources.List)
	calculateRequest(requestResource, resources.List)

	// 查出所有项目的 quota 记录
	projectsQuota, err := o.bdl.ListQuotaRecords()
	if err != nil {
		return nil, errors.Wrap(err, "failed to Find all project quota")
	}
	// 遍历所有项目和环境, 扣减对应集群的资源, 以计算出集群剩余资源
	deductionQuota(calculators, projectsQuota)

	var resourceInfo apistructs.OrgClustersResourcesInfo
	for _, workspace := range calcu.Workspaces {
		workspaceStr := calcu.WorkspaceString(workspace)

		for _, clusterName := range getClustersResourcesRequest.ClusterNames {
			calculator, ok := calculators[clusterName]
			if !ok {
				continue
			}
			resource := &apistructs.ClusterResources{
				ClusterName:    clusterName,
				Workspace:      workspaceStr,
				CPUAllocatable: calcu.MillcoreToCore(calculator.AllocatableCPU(workspace), 3),
				CPUAvailable:   calcu.MillcoreToCore(calculator.QuotableCPUForWorkspace(workspace), 3),
				CPUQuotaRate:   0,
				CPURequest:     0,
				MemAllocatable: calcu.ByteToGibibyte(calculator.AllocatableMem(workspace), 3),
				MemAvailable:   calcu.ByteToGibibyte(calculator.QuotableMemForWorkspace(workspace), 3),
				MemQuotaRate:   0,
				MemRequest:     0,
				Nodes:          clusterNodes[clusterName],
				Tips:           "",
				CPUTookUp:      calcu.MillcoreToCore(calculator.AlreadyTookUpCPU(workspace), 3),
				MemTookUp:      calcu.ByteToGibibyte(calculator.AlreadyTookUpMem(workspace), 3),
			}
			if c, ok := requestResource[clusterName]; ok {
				resource.CPURequest = calcu.MillcoreToCore(c.AllocatableCPU(workspace), 3)
				resource.MemRequest = calcu.ByteToGibibyte(c.AllocatableMem(workspace), 3)
			}
			if resource.CPUAllocatable > 0 {
				resource.CPUQuotaRate = 1 - resource.CPUAvailable/resource.CPUAllocatable
			}
			if resource.MemAllocatable > 0 {
				resource.MemQuotaRate = 1 - resource.MemAvailable/resource.MemAllocatable
			}
			o.makeTips(ctx, resource, calculator, workspace)

			resourceInfo.ClusterList = append(resourceInfo.ClusterList, resource)
			resourceInfo.TotalCPU += resource.CPUAllocatable
			resourceInfo.TotalMem += resource.MemAllocatable
			resourceInfo.AvailableCPU += resource.CPUAvailable
			resourceInfo.AvailableMem += resource.MemAvailable
		}
	}

	return &resourceInfo, nil
}

func countClustersNodes(result map[string]int, list []*dashboardPb.ClusterResourceDetail) {
	if result == nil {
		return
	}
	for _, cluster := range list {
		if !cluster.GetSuccess() {
			logrus.WithField("cluster_name", cluster.GetClusterName()).WithField("err", cluster.GetErr()).
				Warnln("the cluster is not valid now")
			continue
		}
		result[cluster.GetClusterName()] = len(cluster.GetHosts())
	}
}

func initClusterAllocatable(result map[string]*calcu.Calculator, list []*dashboardPb.ClusterResourceDetail) {
	if result == nil {
		return
	}
	for _, cluster := range list {
		if !cluster.GetSuccess() {
			logrus.WithField("cluster_name", cluster.GetClusterName()).WithField("err", cluster.GetErr()).
				Warnln("the cluster is not valid now")
			continue
		}

		// 累计此 host 上的 allocatable 资源
		calculator, ok := result[cluster.GetClusterName()]
		if !ok {
			calculator = calcu.New(cluster.GetClusterName())
		}
		for _, host := range cluster.Hosts {
			workspaces := extractWorkspacesFromLabels(host.GetLabels())
			calculator.AddValue(host.GetCpuAllocatable(), host.GetMemAllocatable(), workspaces...)
		}

		result[cluster.GetClusterName()] = calculator
	}
}

// 遍历所有项目和环境, 扣减对应集群的资源, 以计算出集群剩余资源
func deductionQuota(clusters map[string]*calcu.Calculator, quotaRecords []*apistructs.ProjectQuota) {
	for _, workspace := range calcu.Workspaces {
		workspaceStr := calcu.WorkspaceString(workspace)
		for _, project := range quotaRecords {
			if available, ok := clusters[project.GetClusterName(workspaceStr)]; ok {
				available.DeductionQuota(workspace, project.GetCPUQuota(workspaceStr), project.GetMemQuota(workspaceStr))
			}
		}
	}
}

func calculateRequest(result map[string]*calcu.Calculator, list []*dashboardPb.ClusterResourceDetail) {
	if result == nil {
		return
	}
	for _, cluster := range list {
		if !cluster.GetSuccess() {
			logrus.WithField("cluster_name", cluster.GetClusterName()).WithField("err", cluster.GetErr()).
				Warnln("the cluster is not valid now")
			continue
		}

		calculator, ok := result[cluster.GetClusterName()]
		if !ok {
			calculator = calcu.New(cluster.GetClusterName())
		}
		for _, host := range cluster.Hosts {
			workspaces := extractWorkspacesFromLabels(host.GetLabels())
			calculator.AddValue(host.GetCpuRequest(), host.GetMemRequest(), workspaces...)
		}

		result[cluster.GetClusterName()] = calculator
	}
}

func (o *Org) makeTips(ctx context.Context, resource *apistructs.ClusterResources, calculator *calcu.Calculator,
	workspace calcu.Workspace) {
	langCodes, _ := ctx.Value("lang_codes").(i18n.LanguageCodes)
	if resource.CPUAllocatable == 0 && resource.MemAllocatable == 0 {
		resource.Tips = o.trans.Text(langCodes, "NoResourceForTheWorkspace")
		if resource.Nodes == 0 {
			resource.Tips = o.trans.Text(langCodes, "NoNodesInTheCluster")
		}
		return
	}

	workspaceText := o.trans.Text(langCodes, strings.ToUpper(calcu.WorkspaceString(workspace)))
	switch quotableCPU, quotableMem := calculator.QuotableCPUForWorkspace(workspace), calculator.QuotableMemForWorkspace(workspace); {
	case quotableCPU == 0 && quotableMem == 0:
		resource.Tips = fmt.Sprintf(o.trans.Text(langCodes, "ResourceSqueeze"), workspaceText, workspaceText)
	case quotableCPU == 0:
		resource.Tips = fmt.Sprintf(o.trans.Text(langCodes, "CPUResourceSqueeze"), workspaceText, workspaceText)
	case quotableMem == 0:
		resource.Tips = fmt.Sprintf(o.trans.Text(langCodes, "MemResourceSqueeze"), workspaceText, workspaceText)
	}
}

func extractWorkspacesFromLabels(labels []string) []calcu.Workspace {
	var (
		m = make(map[calcu.Workspace]bool)
		w []calcu.Workspace
	)
	for _, label := range labels {
		switch strings.ToLower(label) {
		case "dice/workspace-prod=true":
			m[calcu.Prod] = true
		case "dice/workspace-staging=true":
			m[calcu.Staging] = true
		case "dice/workspace-test=true":
			m[calcu.Test] = true
		case "dice/workspace-dev=true":
			m[calcu.Dev] = true
		}
	}
	for k := range m {
		w = append(w, k)
	}
	return w
}
