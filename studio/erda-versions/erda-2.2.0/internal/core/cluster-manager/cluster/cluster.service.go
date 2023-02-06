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

package cluster

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/cluster/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

type ClusterService struct {
	db  *db.ClusterDB
	bdl *bundle.Bundle
}

type Option = func(c *ClusterService)

func NewClusterService(options ...Option) *ClusterService {
	svc := &ClusterService{}
	for _, option := range options {
		option(svc)
	}
	return svc
}

func WithDB(db *db.ClusterDB) Option {
	return func(c *ClusterService) {
		c.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(c *ClusterService) {
		c.bdl = bdl
	}
}

func (c *ClusterService) ListCluster(ctx context.Context, req *pb.ListClusterRequest) (*pb.ListClusterResponse, error) {
	var (
		clusters []*pb.ClusterInfo
		err      error
	)
	if err = auth(ctx); err != nil {
		return nil, err
	}

	clusterType := req.ClusterType
	if clusterType != "" {
		clusters, err = c.ListClusterByType(clusterType)
	} else {
		clusters, err = c.List()
	}
	if err != nil {
		return nil, ErrListCluster.InternalError(err)
	}

	if req.OrgID == 0 {
		return &pb.ListClusterResponse{
			Success: true,
			Data:    clusters,
		}, nil
	}

	clusterRelation, err := c.bdl.GetOrgClusterRelationsByOrg(uint64(req.OrgID))
	if err != nil {
		return nil, ErrListCluster.InternalError(err)
	}

	inOrgIDMap := make(map[uint64]struct{})
	for i := 0; i < len(clusterRelation); i++ {
		inOrgIDMap[clusterRelation[i].ClusterID] = struct{}{}
	}
	var clustersInOrg []*pb.ClusterInfo
	for _, cluster := range clusters {
		if _, ok := inOrgIDMap[uint64(cluster.Id)]; ok {
			clustersInOrg = append(clustersInOrg, cluster)
		}
	}
	return &pb.ListClusterResponse{
		Data:    clustersInOrg,
		Success: true,
	}, nil
}

func (c *ClusterService) GetCluster(ctx context.Context, req *pb.GetClusterRequest) (*pb.GetClusterResponse, error) {
	if err := auth(ctx); err != nil {
		logrus.Errorf("failed to auth, %v", err)
		return nil, err
	}

	cluster, err := c.Get(req.IdOrName)
	if err != nil {
		logrus.Errorf("failed to get cluster %s, %v", req.IdOrName, err)
		if strutil.Contains(err.Error(), "not found") {
			return nil, ErrGetCluster.NotFound()
		}
		return nil, ErrGetCluster.InternalError(err)
	}

	return &pb.GetClusterResponse{
		Data:    cluster,
		Success: true,
	}, nil
}

func (c *ClusterService) CreateCluster(ctx context.Context, req *pb.CreateClusterRequest) (*pb.CreateClusterResponse, error) {
	logrus.Infof("request body: %+v", req)
	if err := auth(ctx); err != nil {
		return nil, err
	}
	if req.UserID == "" {
		return nil, ErrCreateCluster.MissingParameter("userID")
	}

	if err := c.CreateWithEvent(req); err != nil {
		return nil, ErrCreateCluster.InternalError(err)
	}

	if err := c.bdl.CreateOrgClusterRelationsByOrg(req.Name, req.UserID, uint64(req.OrgID)); err != nil {
		return nil, ErrCreateCluster.InternalError(err)
	}
	return &pb.CreateClusterResponse{Success: true}, nil
}

func (c *ClusterService) UpdateCluster(ctx context.Context, req *pb.UpdateClusterRequest) (*pb.UpdateClusterResponse, error) {
	logrus.Infof("request body: %+v", req)
	if err := auth(ctx); err != nil {
		return nil, err
	}

	if err := c.UpdateWithEvent(req); err != nil {
		if strutil.Contains(err.Error(), "not found") {
			return nil, ErrGetCluster.NotFound()
		}
		return nil, ErrUpdateCluster.InvalidParameter(err)
	}

	return &pb.UpdateClusterResponse{Success: true}, nil
}

func (c *ClusterService) DeleteCluster(ctx context.Context, req *pb.DeleteClusterRequest) (*pb.DeleteClusterResponse, error) {
	if err := auth(ctx); err != nil {
		return nil, err
	}

	if err := c.DeleteWithEvent(req.ClusterName); err != nil {
		return nil, err
	}

	return &pb.DeleteClusterResponse{Success: true}, nil
}

func (c *ClusterService) PatchCluster(ctx context.Context, req *pb.PatchClusterRequest) (*pb.PatchClusterResponse, error) {
	logrus.Infof("request body: %+v", req)
	if err := auth(ctx); err != nil {
		return nil, err
	}

	if err := c.PatchWithEvent(req); err != nil {
		if strutil.Contains(err.Error(), "not found") {
			return nil, ErrGetCluster.NotFound()
		}
		return nil, ErrPatchCluster.InvalidParameter(err)
	}

	return &pb.PatchClusterResponse{}, nil
}

func auth(ctx context.Context) error {
	internalClient := apis.GetHeader(ctx, httputil.InternalHeader)
	if internalClient == "" {
		return ErrPreCheckCluster.AccessDenied()
	}
	return nil
}
