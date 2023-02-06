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

package cmp_interface

import (
	"context"

	"github.com/rancher/apiserver/pkg/types"

	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
)

type Provider interface {
	SteveServer
	metrics.Interface
	ClusterInterface
}

type ClusterInterface interface {
	GetClustersResources(ctx context.Context, cReq *pb.GetClustersResourcesRequest) (*pb.GetClusterResourcesResponse, error)
	GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error)
}

type SteveServer interface {
	GetSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	ListSteveResource(context.Context, *apistructs.SteveRequest) ([]types.APIObject, error)
	UpdateSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	CreateSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	DeleteSteveResource(context.Context, *apistructs.SteveRequest) error
	PatchNode(context.Context, *apistructs.SteveRequest) error
	LabelNode(context.Context, *apistructs.SteveRequest, map[string]string) error
	UnlabelNode(context.Context, *apistructs.SteveRequest, []string) error
	CordonNode(context.Context, *apistructs.SteveRequest) error
	UnCordonNode(context.Context, *apistructs.SteveRequest) error
	DrainNode(context.Context, *apistructs.SteveRequest) error
	OfflineNode(context.Context, string, string, string, []string) error
	OnlineNode(context.Context, *apistructs.SteveRequest) error
}

type ClusterManagerInterface interface {
	ClusterInfo(ctx context.Context, orgID uint64, clusterNames []string) ([]map[string]map[string]apistructs.NameValue, error)
}
