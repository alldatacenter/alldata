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

package project

import (
	"context"
	"net/url"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
	"github.com/erda-project/erda/pkg/strutil"
)

// Get gets the project info.
// id is the project id.
func (p *Project) Get(ctx context.Context, id uint64) (*apistructs.ProjectDTO, *errorresp.APIError) {
	l := logrus.WithField("func", "*Project.Get")
	dto, err := p.bdl.GetProjectWithSetter(id, httpclient.SetParams(url.Values{"withQuota": {"true"}}))
	if err != nil {
		l.Errorf("failed to GetProject by bdl: %v", err)
		return nil, apierrors.ErrGetProject.InternalError(err)
	}

	if dto.ResourceConfig == nil {
		return dto, nil
	}

	p.fetchProjectAvailableResource(ctx, dto)
	p.makeProjectResourceTips(ctx, dto)
	return dto, nil
}

// 查出各环境的实际可用资源
// 各环境的实际可用资源 = 有该环境标签的所有集群的可用资源之和
// 每台机器的可用资源 = 该机器的 allocatable - 该机器的 request
func (p *Project) fetchProjectAvailableResource(ctx context.Context, dto *apistructs.ProjectDTO) {
	// make the request content for calling gRPC to get available resource info from CMP
	if dto.ResourceConfig == nil {
		return
	}
	var clusterNames []string
	for _, resource := range []*apistructs.ResourceConfigInfo{
		dto.ResourceConfig.PROD,
		dto.ResourceConfig.STAGING,
		dto.ResourceConfig.TEST,
		dto.ResourceConfig.DEV,
	} {
		if resource != nil {
			clusterNames = append(clusterNames, resource.ClusterName)
		}
	}
	clusterNames = strutil.DedupSlice(clusterNames)
	if len(clusterNames) == 0 {
		return
	}
	req := &dashboardPb.GetClustersResourcesRequest{ClusterNames: clusterNames}
	ctx, cancel := context.WithTimeout(ctx, time.Second*15)
	defer cancel()
	clustersResources, err := p.cmp.GetClustersResources(ctx, req)
	if err != nil {
		logrus.WithError(err).WithField("func", "fetchProjectAvailableResource").
			Errorf("failed to GetClustersResources, clusterNames: %v", clusterNames)
		return
	}
	for _, clusterItem := range clustersResources.List {
		if !clusterItem.GetSuccess() {
			logrus.WithField("cluster_name", clusterItem.GetClusterName()).WithField("err", clusterItem.GetErr()).
				Warnln("the cluster is not valid now")
			continue
		}
		for _, host := range clusterItem.Hosts {
			if host == nil {
				continue
			}
			for _, label := range host.Labels {
				var source *apistructs.ResourceConfigInfo
				switch strings.ToLower(label) {
				case "dice/workspace-prod=true":
					source = dto.ResourceConfig.PROD
				case "dice/workspace-staging=true":
					source = dto.ResourceConfig.STAGING
				case "dice/workspace-test=true":
					source = dto.ResourceConfig.TEST
				case "dice/workspace-dev=true":
					source = dto.ResourceConfig.DEV
				}
				if source != nil && source.ClusterName == clusterItem.GetClusterName() {
					source.CPUAvailable += calcu.MillcoreToCore(host.GetCpuAllocatable()-host.GetCpuRequest(), 3)
					source.MemAvailable += calcu.ByteToGibibyte(host.GetMemAllocatable()-host.GetMemRequest(), 3)
				}
			}
		}
	}
}

func (p *Project) makeProjectResourceTips(ctx context.Context, dto *apistructs.ProjectDTO) {
	if dto.ResourceConfig == nil {
		return
	}

	langCodes := httpserver.UnwrapI18nCodes(ctx)

	for _, resource := range []*apistructs.ResourceConfigInfo{
		dto.ResourceConfig.PROD,
		dto.ResourceConfig.STAGING,
		dto.ResourceConfig.TEST,
		dto.ResourceConfig.DEV,
	} {
		if resource == nil {
			continue
		}
		if resource.CPUAvailable < resource.CPUQuota || resource.MemAvailable < resource.MemQuota {
			resource.Tips = p.trans.Text(langCodes, "AvailableIsLessThanQuota")
		}
	}
}
