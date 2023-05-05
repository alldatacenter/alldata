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

package manager

import (
	"context"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (am *AdminManager) AppendClusterEndpoint() {
	am.endpoints = append(am.endpoints, []httpserver.Endpoint{
		{Path: "/api/clusters", Method: http.MethodGet, Handler: am.ListCluster},
		{Path: "/api/clusters/{clusterName}", Method: http.MethodGet, Handler: am.InspectCluster},
		{Path: "/api/clusters/actions/dereference", Method: http.MethodPut, Handler: am.DereferenceCluster},
	}...)
}

func (am *AdminManager) ListCluster(ctx context.Context, req *http.Request, resources map[string]string) (httpserver.Responser, error) {

	var (
		orgID uint64
		err   error
	)

	// get user info
	userID := req.Header.Get("USER-ID")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListApprove.InvalidParameter(fmt.Errorf("invalid user id")).ToResp(), nil
	}
	orgIDStr, _ := GetOrgIDStr(req)

	// check permission
	err = PermissionCheck(am.bundle, userID, orgIDStr, "", apistructs.GetAction)
	if err != nil {
		logrus.Errorf("list cluster failed, error: %v", err)
		return apierrors.ErrListCluster.InternalError(err).ToResp(), nil
	}

	clusterType := req.URL.Query().Get("clusterType")
	newClusters := []*clusterpb.ClusterInfo{}

	// use sys symbol if use admin user and return all cluster info
	// else return cluster with org relation
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	if req.URL.Query().Get("sys") != "" {
		resp, err := am.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{ClusterType: clusterType})
		if err != nil {
			return apierrors.ErrListCluster.InternalError(err).ToResp(), nil
		}
		newClusters = resp.Data
	} else {
		orgID, err = GetOrgID(req)
		if err != nil {
			return apierrors.ErrListCluster.InvalidParameter(err).ToResp(), nil
		}

		clusterRelation, err := am.bundle.GetOrgClusterRelationsByOrg(orgID)
		if err != nil {
			return apierrors.ErrListCluster.InternalError(err).ToResp(), nil
		}
		resp, err := am.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{ClusterType: clusterType})
		if err != nil {
			return apierrors.ErrListCluster.InternalError(err).ToResp(), nil
		}

		clusters := resp.Data
		for _, cluster := range clusters {
			for _, relate := range clusterRelation {
				if relate.ClusterID == uint64(cluster.Id) && relate.OrgID == orgID {
					cluster.IsRelation = "Y"
					newClusters = append(newClusters, cluster)
				}
			}
		}
	}

	// remove sensitive info
	for i := range newClusters {
		removeSensitiveInfo(newClusters[i])
	}

	return httpserver.OkResp(newClusters)
}

func removeSensitiveInfo(cluster *clusterpb.ClusterInfo) {
	if cluster == nil {
		return
	}
	if cluster.SchedConfig != nil {
		removeScheduleConfigSensitiveInfo(cluster.SchedConfig)
	}
	cluster.OpsConfig = nil
	if cluster.System != nil {
		removeSysConfSensitiveInfo(cluster.System)
	}
	if cluster.ManageConfig != nil {
		cluster.ManageConfig = &clusterpb.ManageConfig{
			CredentialSource: cluster.ManageConfig.CredentialSource,
			Address:          cluster.ManageConfig.Address,
		}
	}
}

func removeScheduleConfigSensitiveInfo(csc *clusterpb.ClusterSchedConfig) {
	csc.AuthType = ""
	csc.AuthUsername = ""
	csc.AuthPassword = ""
	csc.CaCrt = ""
	csc.ClientKey = ""
	csc.ClientCrt = ""
	csc.AccessKey = ""
	csc.AccessSecret = ""
}

func removeSysConfSensitiveInfo(sc *clusterpb.SysConf) {
	sc.Ssh = &clusterpb.SSH{}
	sc.Platform = &clusterpb.Platform{}
	sc.MainPlatform = nil
}

func (am *AdminManager) DereferenceCluster(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDereferenceCluster.NotLogin().ToResp(), nil
	}

	orgID, err := GetOrgID(r)
	if err != nil {
		return apierrors.ErrListCluster.InvalidParameter(err).ToResp(), nil
	}

	clusterName := r.URL.Query().Get("clusterName")
	if clusterName == "" {
		return apierrors.ErrDereferenceCluster.MissingParameter("clusterName").ToResp(), nil
	}

	resp, err := am.bundle.DereferenceCluster(orgID, clusterName, userID.String(), false)
	if err != nil {
		return apierrors.ErrDereferenceCluster.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp)
}

func (am *AdminManager) InspectCluster(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	clusterName := vars["clusterName"]
	if clusterName == "" {
		return apierrors.ErrGetCluster.MissingParameter("clusterName").ToResp(), nil
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := am.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
	if err != nil {
		return apierrors.ErrGetCluster.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp.Data)
}
