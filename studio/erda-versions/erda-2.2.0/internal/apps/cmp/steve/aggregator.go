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

package steve

import (
	"context"
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/mux"
	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/steve/pkg/attributes"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	v1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apiserver/pkg/endpoints/request"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	apierrors2 "github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/apps/cmp/steve/predefined"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/k8sclient/config"
)

type group struct {
	ready  bool
	server *Server
	cancel context.CancelFunc
}

type Aggregator struct {
	Ctx        context.Context
	Bdl        *bundle.Bundle
	servers    sync.Map
	clusterSvc clusterpb.ClusterServiceServer
}

// NewAggregator new an aggregator with steve servers for all current clusters
func NewAggregator(ctx context.Context, bdl *bundle.Bundle, clusterSvc clusterpb.ClusterServiceServer) *Aggregator {
	a := &Aggregator{
		Ctx:        ctx,
		Bdl:        bdl,
		clusterSvc: clusterSvc,
	}
	a.init()
	go a.watchClusters(ctx)
	return a
}

func (a *Aggregator) GetAllClusters() []string {
	var clustersNames []string
	a.servers.Range(func(key, _ interface{}) bool {
		if clusterName, ok := key.(string); ok {
			clustersNames = append(clustersNames, clusterName)
		}
		return true
	})
	return clustersNames
}

// ListClusters list ready and unready clusters in steveAggregator
func (a *Aggregator) ListClusters() (ready, unready []string) {
	a.servers.Range(func(key, value interface{}) bool {
		g := value.(*group)
		if g.ready {
			ready = append(ready, key.(string))
		} else {
			unready = append(unready, key.(string))
		}
		return true
	})
	return
}

func (a *Aggregator) IsServerReady(clusterName string) bool {
	s, ok := a.servers.Load(clusterName)
	if !ok {
		return false
	}
	g := s.(*group)
	return g.ready
}

// HasAccess set schemas for apiOp and check access for user in apiOp
func (a *Aggregator) HasAccess(clusterName string, apiOp *types.APIRequest, verb string) (bool, error) {
	g, ok := a.servers.Load(clusterName)
	if !ok {
		return false, errors.Errorf("steve server not found for cluster %s", clusterName)
	}

	server := g.(*group).server
	if err := server.SetSchemas(apiOp); err != nil {
		return false, err
	}

	schema := apiOp.Schemas.LookupSchema(apiOp.Type)
	if schema == nil {
		return false, errors.Errorf("steve server for cluster %s is not ready", clusterName)
	}

	user, ok := request.UserFrom(apiOp.Context())
	if !ok {
		return false, nil
	}

	access := server.AccessSetLookup.AccessFor(user)
	gr := attributes.GR(schema)
	ns := apiOp.Namespace
	if ns == "" {
		ns = "*"
	}
	return access.Grants(verb, gr, ns, attributes.Resource(schema)), nil
}

// watchClusters watches whether there is a steve server for deleted cluster
func (a *Aggregator) watchClusters(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			return
		case <-time.Tick(time.Minute):
			clusters, err := a.listClusterByType("k8s", "edas")
			if err != nil {
				logrus.Errorf("failed to list clusters when watching: %v", err)
				continue
			}
			exists := make(map[string]struct{})
			for _, cluster := range clusters {
				if cluster.ManageConfig == nil {
					logrus.Infof("manage config for cluster %s is nil, skip it", cluster.Name)
					continue
				}
				exists[cluster.Name] = struct{}{}
				if _, ok := a.servers.Load(cluster.Name); ok {
					continue
				}
				a.Add(cluster)
			}

			var readyCluster []string
			checkDeleted := func(key interface{}, value interface{}) (res bool) {
				res = true
				g, _ := value.(*group)
				if g.ready {
					readyCluster = append(readyCluster, key.(string))
				}

				if _, ok := exists[key.(string)]; ok {
					return
				}
				a.Delete(key.(string))
				return
			}
			a.servers.Range(checkDeleted)
			logrus.Infof("Clusters with ready steve server: %v", readyCluster)
		}
	}
}

func (a *Aggregator) listClusterByType(types ...string) ([]*clusterpb.ClusterInfo, error) {
	ctx := transport.WithHeader(a.Ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	var result []*clusterpb.ClusterInfo
	for _, typ := range types {
		clusters, err := a.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{ClusterType: typ})
		if err != nil {
			return nil, err
		}
		result = append(result, clusters.Data...)
	}
	return result, nil
}

func (a *Aggregator) init() {
	clusters, err := a.listClusterByType("k8s", "edas")
	if err != nil {
		logrus.Errorf("failed to list clusters, %v", err)
		return
	}

	for i := range clusters {
		if clusters[i].ManageConfig == nil {
			logrus.Infof("manage config for cluster %s is nil, skip it", clusters[i].Name)
			continue
		}
		a.Add(clusters[i])
	}
}

// Add starts a steve server for k8s cluster with clusterName and add it into aggregator
func (a *Aggregator) Add(clusterInfo *clusterpb.ClusterInfo) {
	if clusterInfo.Type != "k8s" && clusterInfo.Type != "edas" {
		return
	}

	if _, ok := a.servers.Load(clusterInfo.Name); ok {
		logrus.Infof("cluster %s is already existed, skip adding cluster", clusterInfo.Name)
		return
	}

	g := &group{ready: false}
	a.servers.Store(clusterInfo.Name, g)
	go func() {
		logrus.Infof("creating predefined resource for cluster %s", clusterInfo.Name)
		if err := a.createPredefinedResource(clusterInfo.Name); err != nil {
			logrus.Infof("failed to create predefined resource for cluster %s, %v. Skip starting steve server",
				clusterInfo.Name, err)
			a.servers.Delete(clusterInfo.Name)
			return
		}
		logrus.Infof("starting steve server for cluster %s", clusterInfo.Name)
		var err error
		server, cancel, err := a.createSteve(clusterInfo)
		defer func() {
			if err != nil {
				if cancel != nil {
					cancel()
				}
				a.servers.Delete(clusterInfo.Name)
			}
		}()
		if err != nil {
			logrus.Errorf("failed to create steve server for cluster %s, %v", clusterInfo.Name, err)
			return
		}

		g := &group{
			ready:  true,
			server: server,
			cancel: cancel,
		}
		a.servers.Store(clusterInfo.Name, g)
		logrus.Infof("steve server for cluster %s started", clusterInfo.Name)
	}()
}

func (a *Aggregator) createPredefinedResource(clusterName string) error {
	client, err := k8sclient.New(clusterName)
	if err != nil {
		return err
	}

	if err := a.insureSystemNamespace(client); err != nil {
		return err
	}

	for _, sa := range predefined.PredefinedServiceAccount {
		saClient := client.ClientSet.CoreV1().ServiceAccounts(sa.Namespace)
		if err = saClient.Delete(a.Ctx, sa.Name, metav1.DeleteOptions{}); err != nil && !apierrors.IsNotFound(err) {
			return err
		}
		if _, err = saClient.Create(a.Ctx, sa, metav1.CreateOptions{}); err != nil && !apierrors.IsAlreadyExists(err) {
			return err
		}
	}

	crClient := client.ClientSet.RbacV1().ClusterRoles()
	for _, cr := range predefined.PredefinedClusterRole {
		if err = crClient.Delete(a.Ctx, cr.Name, metav1.DeleteOptions{}); err != nil && !apierrors.IsNotFound(err) {
			return err
		}
		if _, err = crClient.Create(a.Ctx, cr, metav1.CreateOptions{}); err != nil && !apierrors.IsAlreadyExists(err) {
			return err
		}
	}

	crbClient := client.ClientSet.RbacV1().ClusterRoleBindings()
	for _, crb := range predefined.PredefinedClusterRoleBinding {
		if err = crbClient.Delete(a.Ctx, crb.Name, metav1.DeleteOptions{}); err != nil && !apierrors.IsNotFound(err) {
			return err
		}
		if _, err = crbClient.Create(a.Ctx, crb, metav1.CreateOptions{}); err != nil && !apierrors.IsAlreadyExists(err) {
			return err
		}
	}
	return nil
}

func (a *Aggregator) insureSystemNamespace(client *k8sclient.K8sClient) error {
	nsClient := client.ClientSet.CoreV1().Namespaces()
	system := &v1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name: "erda-system",
		},
	}
	newNs, err := nsClient.Create(a.Ctx, system, metav1.CreateOptions{})
	if err != nil && !apierrors.IsAlreadyExists(err) {
		return err
	}

	for newNs.Status.Phase != v1.NamespaceActive {
		newNs, err = nsClient.Get(a.Ctx, "erda-system", metav1.GetOptions{})
		if err != nil {
			return err
		}
		select {
		case <-a.Ctx.Done():
			return errors.New("failed to watch system namespace, context canceled")
		case <-time.After(time.Second):
			logrus.Infof("creating erda system namespace...")
		}
	}
	return nil
}

// Delete closes a steve server for k8s cluster with clusterName and delete it from aggregator
func (a *Aggregator) Delete(clusterName string) {
	g, ok := a.servers.Load(clusterName)
	if !ok {
		logrus.Infof("steve server for cluster %s not existed, skip", clusterName)
		return
	}

	group, _ := g.(*group)
	if group.ready {
		group.cancel()
	}
	a.servers.Delete(clusterName)
	logrus.Infof("steve server for cluster %s stopped", clusterName)
}

// ServeHTTP forwards API request to corresponding steve server
func (a *Aggregator) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	vars := mux.Vars(req)
	clusterName := vars["clusterName"]

	if clusterName == "" {
		rw.WriteHeader(http.StatusNotFound)
		rw.Write(apistructs.NewSteveError(apistructs.NotFound, "cluster name is required").JSON())
		return
	}

	s, ok := a.servers.Load(clusterName)
	if !ok {
		ctx := transport.WithHeader(a.Ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		resp, err := a.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
		if err != nil {
			rw.WriteHeader(http.StatusInternalServerError)
			rw.Write(apistructs.NewSteveError(apistructs.ServerError,
				fmt.Sprintf("failed to get cluster %s, %v", clusterName, err)).JSON())
			return
		}

		clusterInfo := resp.Data
		if clusterInfo.Type != "k8s" && clusterInfo.Type != "edas" {
			rw.WriteHeader(http.StatusBadRequest)
			rw.Write(apistructs.NewSteveError(apistructs.BadRequest,
				fmt.Sprintf("cluster %s is not a k8s or edas cluster", clusterName)).JSON())
			return
		}

		logrus.Infof("steve for cluster %s not exist, starting a new server", clusterInfo.Name)
		a.Add(clusterInfo)
		if s, ok = a.servers.Load(clusterInfo.Name); !ok {
			rw.WriteHeader(http.StatusInternalServerError)
			rw.Write(apistructs.NewSteveError(apistructs.ServerError, "Internal server error").JSON())
			return
		}
	}

	group, _ := s.(*group)
	if !group.ready {
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write(apistructs.NewSteveError(apistructs.ServerError,
			fmt.Sprintf("API for cluster %s is not ready, please wait", clusterName)).JSON())
		return
	}
	group.server.ServeHTTP(rw, req)
}

func (a *Aggregator) Serve(clusterName string, apiOp *types.APIRequest) error {
	s, ok := a.servers.Load(clusterName)
	if !ok {
		ctx := transport.WithHeader(a.Ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		resp, err := a.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
		if err != nil {
			return apierrors2.ErrInvoke.InvalidParameter(errors.Errorf("failed to get cluster %s, %v", clusterName, err))
		}

		clusterInfo := resp.Data
		if clusterInfo.Type != "k8s" && clusterInfo.Type != "edas" {
			return apierrors2.ErrInvoke.InvalidParameter(errors.Errorf("cluster %s is not a k8s or edas cluster", clusterName))
		}

		logrus.Infof("steve for cluster %s not exist, starting a new server", clusterInfo.Name)
		a.Add(clusterInfo)
		if s, ok = a.servers.Load(clusterInfo.Name); !ok {
			return apierrors2.ErrInvoke.InternalError(errors.Errorf("failed to start steve server for cluster %s", clusterInfo.Name))
		}
	}

	group, _ := s.(*group)
	if !group.ready {
		return apierrors2.ErrInvoke.InternalError(errors.Errorf("k8s API for cluster %s is not ready, please wait", clusterName))
	}

	if apiOp.Schemas == nil {
		if err := group.server.SetSchemas(apiOp); err != nil {
			return err
		}
	}
	group.server.Handle(apiOp)
	return nil
}

func (a *Aggregator) createSteve(clusterInfo *clusterpb.ClusterInfo) (*Server, context.CancelFunc, error) {
	if clusterInfo.ManageConfig == nil {
		return nil, nil, fmt.Errorf("manageConfig of cluster %s is null", clusterInfo.Name)
	}

	restConfig, err := config.ParseManageConfigPb(clusterInfo.Name, clusterInfo.ManageConfig)
	if err != nil {
		return nil, nil, err
	}

	ctx, cancel := context.WithCancel(a.Ctx)
	prefix := GetURLPrefix(clusterInfo.Name)
	server, err := New(ctx, restConfig, &Options{
		AuthMiddleware: emptyMiddleware,
		Router:         RoutesWrapper(prefix),
		URLPrefix:      prefix,
		ClusterName:    clusterInfo.Name,
	})
	if err != nil {
		cancel()
		return nil, nil, err
	}
	go a.preloadCache(ctx, server, string(apistructs.K8SNode))
	go a.preloadCache(ctx, server, string(apistructs.K8SPod))
	return server, cancel, nil
}

func (a *Aggregator) preloadCache(ctx context.Context, server *Server, resType string) {
	for i := 0; i < 10; i++ {
		select {
		case <-ctx.Done():
			return
		default:
			logrus.Infof("preload cache for %s in cluster %s", resType, server.ClusterName)
			err := a.listAndSetCache(ctx, server, resType)
			if err == nil {
				logrus.Infof("preload cache for %s in cluster %s succeeded", resType, server.ClusterName)
				return
			}
			logrus.Infof("preload cache for %s in cluster %s failed, retry after 5 seconds, err: %v", resType, server.ClusterName, err)
			time.Sleep(time.Second * 5)
		}
	}
}

func (a *Aggregator) listAndSetCache(ctx context.Context, server *Server, resType string) error {
	apiOp, resp, err := a.getApiRequest(ctx, &apistructs.SteveRequest{
		NoAuthentication: true,
		Type:             apistructs.K8SResType(resType),
		ClusterName:      server.ClusterName,
	})
	if err != nil {
		return errors.Errorf("failed to get api request, %v", err)
	}

	if err = server.SetSchemas(apiOp); err != nil {
		logrus.Errorf("failed to preload cache for %s, %v", resType, err)
	}
	server.Handle(apiOp)
	list, err := convertResp(resp)
	if err != nil {
		return err
	}

	key := CacheKey{
		Kind:        resType,
		ClusterName: server.ClusterName,
	}
	if err = setCacheForList(key.GetKey(), list); err != nil {
		return errors.Errorf("failed to set cache for %s", resType)
	}
	return nil
}

func emptyMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
		next.ServeHTTP(resp, req)
	})
}
