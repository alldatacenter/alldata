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

package edge

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/services/kubernetes"
	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
	"github.com/erda-project/erda/pkg/http/httputil"
)

const (
	siteNodeCountFormat        = "%d/%d"
	DeploymentType             = "Deployment"
	StatefulSetType            = "StatefulSet"
	UnitedDeploymentAPIVersion = "apps.openyurt.io/v1alpha1"
	UnitedDeploymentKind       = "UnitedDeployment"
	SecretKind                 = "Secret"
	SecretApiVersion           = "v1"
	EdgeAppPrefix              = "edgeapp"
	EdgeAppDeployingStatus     = "deploying"
	EdgeAppSucceedStatus       = "succeed"
)

var (
	// Cluster ID/Name/RequestAddress which fixed param will cache in memory.
	clusterInfos = make(map[int64]*clusterpb.ClusterInfo, 0)
	orgInfos     = make(map[int64]*apistructs.OrgDTO, 0)
)

// NodePools clusterName: nodePools
type NodePools = map[string]*v1alpha1.NodePool

type Edge struct {
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	k8s        *kubernetes.Kubernetes
	clusterSvc clusterpb.ClusterServiceServer
}

type Option func(*Edge)

func New(options ...Option) *Edge {
	site := &Edge{}
	for _, op := range options {
		op(site)
	}
	return site
}

// WithDBClient With db client.
func WithDBClient(db *dbclient.DBClient) Option {
	return func(e *Edge) {
		e.db = db
	}
}

// WithKubernetes With kubernetes client.
func WithKubernetes(k *kubernetes.Kubernetes) Option {
	return func(e *Edge) {
		e.k8s = k
	}
}

// WithBundle With bundle module.
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Edge) {
		e.bdl = bdl
	}
}

// WithClusterSvc With cluster-manager ClusterService
func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(e *Edge) {
		e.clusterSvc = clusterSvc
	}
}

// getClusterInfo Get or cache clusterInfo.
func (e *Edge) getClusterInfo(ctx context.Context, clusterID int64) (*clusterpb.ClusterInfo, error) {
	if clusterInfo, ok := clusterInfos[clusterID]; ok {
		return clusterInfo, nil
	}
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := e.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: strconv.FormatInt(clusterID, 10)})
	if err != nil {
		logrus.Errorf("query cluster info failed, cluster:%d, err:%v", clusterID, err)
		return nil, fmt.Errorf("query cluster info failed, cluster:%d, err:%v", clusterID, err)
	}
	clusterInfo := resp.Data
	clusterInfos[clusterID] = clusterInfo
	return clusterInfo, nil
}

// getOrgInfo Get or cache orgInfo.
func (e *Edge) getOrgInfo(orgID int64) (*apistructs.OrgDTO, error) {
	if orgInfo, ok := orgInfos[orgID]; ok {
		return orgInfo, nil
	}
	orgInfo, err := e.bdl.GetOrg(strconv.FormatInt(orgID, 10))
	if err != nil {
		return nil, fmt.Errorf("query org info failed, org:%d, err:%v", orgID, err)
	}
	orgInfos[orgID] = orgInfo
	return orgInfo, nil
}

func (e *Edge) IsContain(items []string, item string) bool {
	for _, eachItem := range items {
		if eachItem == item {
			return true
		}
	}
	return false
}

// ConvertToEdgeAppInfo Convert type EdgeApp to type EdgeAppInfo.
func (e *Edge) ConvertToEdgeAppInfo(edgeApp *dbclient.EdgeApp) (*apistructs.EdgeAppInfo, error) {
	var edgeSites []string
	var dependApp []string
	var portMaps []apistructs.PortMap
	var extraData map[string]string
	if len(edgeApp.EdgeSites) != 0 {
		if err := json.Unmarshal([]byte(edgeApp.EdgeSites), &edgeSites); err != nil {
			logrus.Errorf("fff, %+v", err)
			return nil, err
		}
	}
	if len(edgeApp.DependApp) != 0 {
		if err := json.Unmarshal([]byte(edgeApp.DependApp), &dependApp); err != nil {
			logrus.Errorf("gff, %+v", err)
			return nil, err
		}
	}
	if len(edgeApp.PortMaps) != 0 {
		if err := json.Unmarshal([]byte(edgeApp.PortMaps), &portMaps); err != nil {
			logrus.Errorf("cff, %+v", err)
			return nil, err
		}
	}
	if len(edgeApp.ExtraData) != 0 {
		if err := json.Unmarshal([]byte(edgeApp.ExtraData), &extraData); err != nil {
			logrus.Errorf("gff, %+v", err)
			return nil, err
		}
	}
	return &apistructs.EdgeAppInfo{
		ID:                  edgeApp.ID,
		OrgID:               edgeApp.OrgID,
		Name:                edgeApp.Name,
		ClusterID:           edgeApp.ClusterID,
		Type:                edgeApp.Type,
		Image:               edgeApp.Image,
		RegistryAddr:        edgeApp.RegistryAddr,
		RegistryUser:        edgeApp.RegistryUser,
		RegistryPassword:    edgeApp.RegistryPassword,
		HealthCheckType:     edgeApp.HealthCheckType,
		HealthCheckHttpPort: edgeApp.HealthCheckHttpPort,
		HealthCheckHttpPath: edgeApp.HealthCheckHttpPath,
		HealthCheckExec:     edgeApp.HealthCheckExec,
		ProductID:           edgeApp.ProductID,
		AddonName:           edgeApp.AddonName,
		AddonVersion:        edgeApp.AddonVersion,
		ConfigSetName:       edgeApp.ConfigSetName,
		Replicas:            edgeApp.Replicas,
		Description:         edgeApp.Description,
		EdgeSites:           edgeSites,
		DependApp:           dependApp,
		LimitCpu:            edgeApp.LimitCpu,
		RequestCpu:          edgeApp.RequestCpu,
		LimitMem:            edgeApp.LimitMem,
		RequestMem:          edgeApp.RequestMem,
		PortMaps:            portMaps,
		ExtraData:           extraData,
	}, nil
}
