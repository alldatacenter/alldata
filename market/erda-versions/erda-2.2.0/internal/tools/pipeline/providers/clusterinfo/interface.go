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

package clusterinfo

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	ClusterHookApiPath = "/api/pipeline-clusters/actions/hook"
)

// Interface defines the interface for the clusterinfo provider
type Interface interface {
	// GetClusterByName return ClusterInfo query by name, contain cluster connection info and config map data
	GetClusterInfoByName(string) (apistructs.ClusterInfo, error)
	// ListAllClusterInfos return all clusters, contain cluster connection info and config map data
	ListAllClusterInfos() ([]apistructs.ClusterInfo, error)
	// DispatchClusterEvent dispatch cluster event received from eventbox
	// other modules can get event by channel if registered
	DispatchClusterEvent(apistructs.ClusterEvent)
	// BatchUpdateAndDispatchRefresh batch update all clusters, and dispatch refresh event if registered
	BatchUpdateAndDispatchRefresh() error
	// RegisterClusterEvent return channel to receive cluster event
	RegisterClusterEvent() <-chan apistructs.ClusterEvent
	// RegisterRefreshEvent return channel to receive refresh event
	RegisterRefreshEvent() <-chan struct{}
	// ListEdgeClusterInfos return all edge clusters, contain cluster connection info and config map data
	ListEdgeClusterInfos() ([]apistructs.ClusterInfo, error)
}

func (p *provider) registerClusterHook() error {
	ev := apistructs.CreateHookRequest{
		Name:   "pipeline_watch_cluster_changed",
		Events: []string{bundle.ClusterEvent},
		URL:    strutil.Concat("http://", discover.Pipeline(), ClusterHookApiPath),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}

	if err := p.bdl.CreateWebhook(ev); err != nil {
		p.Log.Errorf("failed to register watch cluster changed event, err: %v", err)
		return err
	}
	return nil
}

// DispatchClusterEvent update the cluster cache then dispatch the event
func (p *provider) DispatchClusterEvent(clusterEvent apistructs.ClusterEvent) {
	switch clusterEvent.Action {
	case apistructs.ClusterActionCreate:
		p.cache.UpdateClusterInfo(clusterEvent.Content)
	case apistructs.ClusterActionUpdate:
		p.cache.UpdateClusterInfo(clusterEvent.Content)
	case apistructs.ClusterActionDelete:
		p.cache.DeleteClusterInfo(clusterEvent.Content.Name)
	default:
		return
	}
	p.notifier.NotifyClusterEvent(clusterEvent)
}

// TriggerManualRefresh update the total cluster cache then dispatch the event
func (p *provider) BatchUpdateAndDispatchRefresh() error {
	if err := p.batchUpdateClusterInfo(); err != nil {
		return err
	}
	p.notifier.NotifyRefreshEvent()
	return nil
}

func (p *provider) batchUpdateClusterInfo() error {
	// first the edge and center both get their current cluster info from k8s configmap
	currentClusterInfo, err := p.GetCurrentClusterInfoFromK8sConfigMap()
	if err != nil {
		p.Log.Warnf("failed to get current cluster info(continue list clusters), err: %v", err)
	} else {
		p.cache.UpdateClusterInfo(currentClusterInfo)
	}
	if p.Cfg.IsEdge {
		p.Log.Info("edge pipeline only get current cluster info from k8s configmap")
		return nil
	}
	// then center continue get all clusters from cluster-manager
	// and will cover the current cluster info, it's acceptable
	clusterInfos, err := p.bdl.ListClusters("", 0)
	if err != nil {
		return err
	}
	for _, cluster := range clusterInfos {
		p.cache.UpdateClusterInfo(cluster)
	}
	return nil
}

func (p *provider) continueUpdateAndRefresh() {
	ticker := time.NewTicker(p.Cfg.RefreshClustersInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			if err := p.BatchUpdateAndDispatchRefresh(); err != nil {
				p.Log.Errorf("[provider clusterinfo] failed to update cluster info, err: %v", err)
			}
		}
	}
}

func (p *provider) GetClusterInfoByName(clusterName string) (apistructs.ClusterInfo, error) {
	cluster, ok := p.cache.GetClusterInfoByName(clusterName)
	if ok {
		return cluster, nil
	}
	// if pipeline is edge-deploy and cluster is current cluster, try to get from k8s configmap
	// otherwise, try to query cluster info from cluster-manager
	if clusterName == p.Cfg.ClusterName && p.Cfg.IsEdge {
		currentClusterInfo, err := p.GetCurrentClusterInfoFromK8sConfigMap()
		if err != nil {
			return apistructs.ClusterInfo{}, err
		}
		p.cache.UpdateClusterInfo(currentClusterInfo)
		return currentClusterInfo, nil
	}
	clusterInfo, err := p.bdl.GetCluster(clusterName)
	if err != nil {
		return apistructs.ClusterInfo{}, err
	}
	p.cache.UpdateClusterInfo(*clusterInfo)
	return *clusterInfo, nil
}

// ListAllClusterInfos firstly get all cluster from cache
// if cache is empty, try to get all from bundle and update the cache
func (p *provider) ListAllClusterInfos() ([]apistructs.ClusterInfo, error) {
	return p.listAllClusterInfos(false)
}

// ListEdgeClusterInfos firstly get all edge cluster from cache
// if cache is empty, try to get all from bundle and update the cache
func (p *provider) ListEdgeClusterInfos() ([]apistructs.ClusterInfo, error) {
	return p.listAllClusterInfos(true)
}

func (p *provider) listAllClusterInfos(onlyEdge bool) ([]apistructs.ClusterInfo, error) {
	clusters := p.cache.GetAllClusters()
	if len(clusters) != 0 {
		return p.filterClusters(clusters, onlyEdge), nil
	}
	if err := p.batchUpdateClusterInfo(); err != nil {
		return nil, err
	}
	return p.filterClusters(p.cache.GetAllClusters(), onlyEdge), nil
}

func (p *provider) filterClusters(clusters []apistructs.ClusterInfo, onlyEdge bool) []apistructs.ClusterInfo {
	if !onlyEdge {
		return clusters
	}

	var edgeCluster []apistructs.ClusterInfo
	wait := limit_sync_group.NewWorker(10)

	for index := range clusters {
		wait.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			index := i[0].(int)
			cluster := clusters[index]

			isEdge, err := p.EdgeRegister.ClusterIsEdge(cluster.Name)
			if err != nil {
				p.Log.Errorf("failed to get ClusterIsEdge cluster %v error %v", cluster.Name, err)
				return nil
			}
			if isEdge {
				locker.Lock()
				defer locker.Unlock()
				edgeCluster = append(edgeCluster, cluster)
			}
			return nil
		}, index)
	}

	_ = wait.Do().Error()
	return edgeCluster
}

func (p *provider) RegisterClusterEvent() <-chan apistructs.ClusterEvent {
	return p.notifier.RegisterClusterEvent()
}

func (p *provider) RegisterRefreshEvent() <-chan struct{} {
	return p.notifier.RegisterRefreshEvent()
}

// GetCurrentClusterInfoFromK8sConfigMap return current cluster info config map data by k8s in-cluster client
// because in edge cluster, we cloud not get the cluster info from cluster-manager
func (p *provider) GetCurrentClusterInfoFromK8sConfigMap() (apistructs.ClusterInfo, error) {
	client, err := k8sclient.New(p.Cfg.ClusterName, k8sclient.WithPreferredToUseInClusterConfig(), k8sclient.WithTimeout(5*time.Second))
	if err != nil {
		return apistructs.ClusterInfo{}, err
	}
	cm, err := client.ClientSet.CoreV1().ConfigMaps(p.Cfg.ErdaNamespace).Get(context.Background(), apistructs.ConfigMapNameOfClusterInfo, metav1.GetOptions{})
	if err != nil {
		return apistructs.ClusterInfo{}, err
	}
	cmBytes, err := json.Marshal(cm.Data)
	if err != nil {
		return apistructs.ClusterInfo{}, fmt.Errorf("failed to marshal configmap data, clusterName: %s, err: %v", p.Cfg.ClusterName, err)
	}
	var cmInfoData apistructs.ClusterInfoData
	if err := json.Unmarshal(cmBytes, &cmInfoData); err != nil {
		return apistructs.ClusterInfo{}, fmt.Errorf("failed to unmarshal configmap data, clusterName: %s, err: %v", p.Cfg.ClusterName, err)
	}
	return apistructs.ClusterInfo{
		Name: p.Cfg.ClusterName,
		CM:   cmInfoData,
	}, nil
}

// TODO: GetClusterInfoByName
// after action executor become provider, remove the following methods
func GetClusterInfoByName(clusterName string) (apistructs.ClusterInfo, error) {
	return pd.GetClusterInfoByName(clusterName)
}
