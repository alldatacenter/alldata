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

package resource

import (
	"context"
	"strconv"

	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

type ReportTable struct {
	bdl *bundle.Bundle
	cmp interface {
		ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error)
		GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error)
	}
	trans i18n.Translator
}

func NewReportTable(opts ...ReportTableOption) *ReportTable {
	var table ReportTable
	for _, opt := range opts {
		opt(&table)
	}
	return &table
}

func (rt *ReportTable) GetResourceOverviewReport(ctx context.Context, orgID int64, clusterNames []string,
	cpuPerNode, memPerNode uint64, groupBy string) (*apistructs.ResourceOverviewReportData, error) {

	// 1) 查找所有 namespaces
	var (
		namespacesM = make(map[string][]string)
		orgIDStr    = strconv.FormatInt(orgID, 10)
	)
	rt.fetchAllNamespaces(ctx, namespacesM, orgIDStr, clusterNames)

	// 2) 调用 core-services bundle，根据 namespaces 查找各 namespaces 的归属
	projectsNamespaces, err := rt.bdl.FetchNamespacesBelongsTo(ctx, uint64(orgID), clusterNames)
	if err != nil {
		err = errors.Wrap(err, "failed to FetchNamespacesBelongsTo")
		logrus.WithError(err).Errorln()
		return nil, err
	}

	// 3) 查找所有 namespace 下的 request 情况
	resources, err := rt.fetchRequestOnNamespaces(ctx, namespacesM)
	if err != nil {
		err = errors.Wrap(err, "failed to GetNamespacesResources")
		logrus.WithError(err).WithField("namespaces", namespacesM).Errorln()
		return nil, err
	}

	// 4) request 归属到项目，归属不到项目的，算作共享资源
	return rt.groupResponse(ctx, resources, projectsNamespaces, cpuPerNode, memPerNode, groupBy), nil
}

func (rt *ReportTable) fetchAllNamespaces(ctx context.Context, namespaces map[string][]string, orgID string, clusters []string) {
	for _, clusterName := range clusters {
		resources, err := rt.cmp.ListSteveResource(ctx, &apistructs.SteveRequest{
			NoAuthentication: true,
			OrgID:            orgID,
			Type:             apistructs.K8SNamespace,
			ClusterName:      clusterName,
		})
		if err != nil {
			err = errors.Wrap(err, "failed to ListSteveResource")
			logrus.WithError(err).Warnln()
		}
		namespaces[clusterName] = nil
		for _, resource := range resources {
			namespace := resource.Data().String("metadata", "name")
			namespaces[clusterName] = append(namespaces[clusterName], namespace)
		}
	}
}

func (rt *ReportTable) fetchRequestOnNamespaces(ctx context.Context, namespaces map[string][]string) (*pb.GetNamespacesResourcesResponse, error) {
	var getNamespacesResourcesReq pb.GetNamespacesResourcesRequest
	for clusterName, namespaceList := range namespaces {
		for _, namespace := range namespaceList {
			getNamespacesResourcesReq.Namespaces = append(getNamespacesResourcesReq.Namespaces, &pb.ClusterNamespacePair{
				ClusterName: clusterName,
				Namespace:   namespace,
			})
		}
	}
	return rt.cmp.GetNamespacesResources(ctx, &getNamespacesResourcesReq)
}

func (rt *ReportTable) groupResponse(ctx context.Context, resources *pb.GetNamespacesResourcesResponse, namespaces *apistructs.GetProjectsNamesapcesResponseData,
	cpuPerNode, memPerNode uint64, groupBy string) *apistructs.ResourceOverviewReportData {
	l := logrus.WithField("func", "*ReportTable.groupResponse")
	var (
		langCodes, _   = ctx.Value("lang_codes").(i18n.LanguageCodes)
		sharedResource [2]uint64
	)
	for _, clusterItem := range resources.List {
		for _, namespaceItem := range clusterItem.List {
			if namespaceItem.GetNamespace() == "default" {
				sharedResource[0] += namespaceItem.GetCpuRequest()
				sharedResource[1] += namespaceItem.GetMemRequest()
				continue
			}
			var belongsToProject = false
			for _, projectItem := range namespaces.List {
				if projectItem.Has(clusterItem.GetClusterName(), namespaceItem.GetNamespace()) {
					l.Debugf("projectID: %v, project namespaces: %+v, goal cluster: %s, goal namespace: %s, cpuRequest: %v, memRequest: %v",
						projectItem.ProjectID, projectItem.Clusters, clusterItem.GetClusterName(), namespaceItem.GetNamespace(),
						namespaceItem.GetCpuRequest(), namespaceItem.GetMemRequest())
					belongsToProject = true
					projectItem.AddResource(namespaceItem.GetCpuRequest(), namespaceItem.GetMemRequest())
					break
				}
			}
			if !belongsToProject {
				sharedResource[0] += namespaceItem.GetCpuRequest()
				sharedResource[1] += namespaceItem.GetMemRequest()
			}
		}
	}

	var data apistructs.ResourceOverviewReportData
	for _, projectItem := range namespaces.List {
		if projectItem.CPUQuota == 0 && projectItem.MemQuota == 0 {
			continue
		}
		item := apistructs.ResourceOverviewReportDataItem{
			ProjectID:          int64(projectItem.ProjectID),
			ProjectName:        projectItem.ProjectName,
			ProjectDisplayName: projectItem.ProjectDisplayName,
			ProjectDesc:        projectItem.ProjectDesc,
			OwnerUserID:        int64(projectItem.OwnerUserID),
			OwnerUserName:      projectItem.OwnerUserName,
			OwnerUserNickName:  projectItem.OwnerUserNickname,
			CPUQuota:           calcu.MillcoreToCore(projectItem.CPUQuota, 3),
			CPURequest:         calcu.MillcoreToCore(projectItem.GetCPUReqeust(), 3),
			CPUWaterLevel:      0,
			MemQuota:           calcu.ByteToGibibyte(projectItem.MemQuota, 3),
			MemRequest:         calcu.ByteToGibibyte(projectItem.GetMemRequest(), 3),
			MemWaterLevel:      0,
			Nodes:              0,
		}
		data.List = append(data.List, &item)
	}
	sharedText := rt.trans.Text(langCodes, "SharedResources")
	ownerUnknown := rt.trans.Text(langCodes, "OwnerUnknown")
	sharedItem := newSharedItem(sharedResource, sharedText, ownerUnknown)
	_ = sharedItem

	if groupBy == "owner" {
		data.GroupByOwner()
	}
	data.Calculates(cpuPerNode, memPerNode)

	return &data
}

type ReportTableOption func(table *ReportTable)

func ReportTableWithBundle(bdl *bundle.Bundle) ReportTableOption {
	return func(table *ReportTable) {
		table.bdl = bdl
	}
}

func ReportTableWithCMP(cmp interface {
	ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error)
	GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error)
}) ReportTableOption {
	return func(table *ReportTable) {
		table.cmp = cmp
	}
}

func ReportTableWithTrans(trans i18n.Translator) ReportTableOption {
	return func(table *ReportTable) {
		table.trans = trans
	}
}

func newSharedItem(shared [2]uint64, sharedText, ownerUnknown string) *apistructs.ResourceOverviewReportDataItem {
	cpu := calcu.MillcoreToCore(shared[0], 3)
	mem := calcu.ByteToGibibyte(shared[1], 3)
	return &apistructs.ResourceOverviewReportDataItem{
		ProjectID:          0,
		ProjectName:        "-",
		ProjectDisplayName: sharedText,
		ProjectDesc:        "",
		OwnerUserID:        0,
		OwnerUserName:      ownerUnknown,
		OwnerUserNickName:  ownerUnknown,
		CPUQuota:           cpu,
		CPURequest:         cpu,
		CPUWaterLevel:      100,
		MemQuota:           mem,
		MemRequest:         mem,
		MemWaterLevel:      100,
		Nodes:              0,
	}
}
