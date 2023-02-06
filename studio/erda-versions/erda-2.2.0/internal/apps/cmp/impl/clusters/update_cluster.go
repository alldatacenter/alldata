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

package clusters

import (
	"context"
	"net/http"
	"strings"

	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (c *Clusters) UpdateCluster(ctx context.Context, req apistructs.CMPClusterUpdateRequest, header http.Header) error {
	var (
		mc  *clusterpb.ManageConfig
		err error
	)

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := c.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: req.ClusterUpdateRequest.Name})
	if err != nil {
		return err
	}

	clusterInfo := resp.Data
	mc = clusterInfo.ManageConfig

	// if credential content is empty, use the latest credential data.
	// if credential change to agent from other type, clear credential info
	if req.Credential.Content != "" || (mc.Type != apistructs.ManageProxy && strings.ToUpper(req.CredentialType) == ProxyType) {
		// parse manage config from credential info
		mc, err = ParseManageConfigFromCredential(req.CredentialType, req.Credential)
		if err != nil {
			return err
		}
	}

	var newSchedulerConfig *clusterpb.ClusterSchedConfig

	if req.Type != apistructs.EDAS {
		newSchedulerConfig = clusterInfo.SchedConfig
		newSchedulerConfig.CpuSubscribeRatio = req.SchedulerConfig.CPUSubscribeRatio
	} else {
		newSchedulerConfig = convertSchedConfigToPbSchedConfig(req.SchedulerConfig)
	}

	// TODO: support tag switch, current force true
	// e.g. modules/scheduler/impl/cluster/hook.go line:136
	newSchedulerConfig.EnableTag = true

	if _, err = c.clusterSvc.UpdateCluster(ctx, &clusterpb.UpdateClusterRequest{
		Name:            clusterInfo.Name,
		DisplayName:     req.DisplayName,
		Description:     req.Description,
		Type:            clusterInfo.Type,
		WildcardDomain:  req.WildcardDomain,
		SchedulerConfig: newSchedulerConfig,
		ManageConfig:    mc,
	}); err != nil {
		return err
	}
	return nil
}
