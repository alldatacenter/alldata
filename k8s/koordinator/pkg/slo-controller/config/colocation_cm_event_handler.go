/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package config

import (
	"context"
	"encoding/json"
	"reflect"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	ReasonColocationConfigUnmarshalFailed = "ColocationCfgUnmarshalFailed"
	ReasonSLOConfigUnmarshalFailed        = "SLOCfgUnmarshalFailed"
)

var _ handler.EventHandler = &ColocationHandlerForConfigMapEvent{}

type ColocationCfgCache interface {
	GetCfgCopy() *extension.ColocationCfg
	IsCfgAvailable() bool
	IsErrorStatus() bool
}

type colocationCfgCache struct {
	lock          sync.RWMutex
	colocationCfg extension.ColocationCfg
	available     bool
	errorStatus   bool
}

type ColocationHandlerForConfigMapEvent struct {
	EnqueueRequestForConfigMap

	Client   client.Client
	cfgCache colocationCfgCache
	recorder record.EventRecorder
}

func NewColocationHandlerForConfigMapEvent(client client.Client, initCfg extension.ColocationCfg, recorder record.EventRecorder) *ColocationHandlerForConfigMapEvent {
	colocationHandler := &ColocationHandlerForConfigMapEvent{cfgCache: colocationCfgCache{colocationCfg: initCfg}, Client: client, recorder: recorder}
	colocationHandler.SyncCacheIfChanged = colocationHandler.syncColocationCfgIfChanged
	colocationHandler.EnqueueRequest = colocationHandler.triggerAllNodeEnqueue
	return colocationHandler
}

// syncColocationCfgIfChanged is a locked version of syncConfig
func (p *ColocationHandlerForConfigMapEvent) syncColocationCfgIfChanged(configMap *corev1.ConfigMap) bool {
	// get co-location config from the configmap
	// if the configmap does not exist, use the default
	p.cfgCache.lock.Lock()
	defer p.cfgCache.lock.Unlock()
	return p.syncConfig(configMap)
}

// syncConfig syncs valid colocation config from the configmap request
func (p *ColocationHandlerForConfigMapEvent) syncConfig(configMap *corev1.ConfigMap) bool {
	// get co-location config from the configmap
	// if the configmap does not exist, use the default
	if configMap == nil {
		klog.Errorf("configmap is deleted!,use default config")
		return p.updateCacheIfChanged(NewDefaultColocationCfg(), true)
	}

	newCfg := &extension.ColocationCfg{}
	configStr := configMap.Data[extension.ColocationConfigKey]
	if configStr == "" {
		klog.Warningf("colocation config is empty!,use default config")
		return p.updateCacheIfChanged(NewDefaultColocationCfg(), false)
	}

	err := json.Unmarshal([]byte(configStr), &newCfg)
	if err != nil {
		//if controller restart ,cache will unavailable, else use old cfg
		klog.Errorf("syncConfig failed! parse colocation error then use old Cfg ,configmap %s/%s, err: %s",
			ConfigNameSpace, SLOCtrlConfigMap, err)
		p.recorder.Eventf(configMap, "Warning", ReasonColocationConfigUnmarshalFailed, "failed to unmarshal colocation config, err: %s", err)
		p.cfgCache.errorStatus = true
		return false
	}

	defaultCfg := NewDefaultColocationCfg()
	// merge default cluster strategy
	mergedClusterCfg := defaultCfg.ColocationStrategy.DeepCopy()
	mergedInterface, _ := util.MergeCfg(mergedClusterCfg, &newCfg.ColocationStrategy)
	newCfg.ColocationStrategy = *(mergedInterface.(*extension.ColocationStrategy))

	if !IsColocationStrategyValid(&newCfg.ColocationStrategy) {
		//if controller restart ,cache will unavailable, else use old cfg
		klog.Errorf("syncConfig failed!  invalid cluster config,%+v", newCfg.ColocationStrategy)
		p.cfgCache.errorStatus = true
		return false
	}

	for index, nodeStrategy := range newCfg.NodeConfigs {
		// merge with clusterStrategy
		clusteStrategyCopy := newCfg.ColocationStrategy.DeepCopy()
		mergedNodeStrategyInterface, _ := util.MergeCfg(clusteStrategyCopy, &nodeStrategy.ColocationStrategy)
		newNodeStrategy := *mergedNodeStrategyInterface.(*extension.ColocationStrategy)
		if !IsColocationStrategyValid(&newNodeStrategy) {
			klog.Errorf("syncConfig failed! invalid node config,then use clusterCfg,nodeCfg:%+v", nodeStrategy)
			newCfg.NodeConfigs[index].ColocationStrategy = *newCfg.ColocationStrategy.DeepCopy()
		} else {
			newCfg.NodeConfigs[index].ColocationStrategy = newNodeStrategy
		}
	}

	changed := p.updateCacheIfChanged(newCfg, false)
	return changed
}

func (p *ColocationHandlerForConfigMapEvent) updateCacheIfChanged(newCfg *extension.ColocationCfg, errorStatus bool) bool {
	changed := !reflect.DeepEqual(&p.cfgCache.colocationCfg, newCfg)
	if changed {
		oldInfoFmt, _ := json.MarshalIndent(p.cfgCache.colocationCfg, "", "\t")
		newInfoFmt, _ := json.MarshalIndent(newCfg, "", "\t")
		klog.V(3).Infof("ColocationCfg changed success! oldCfg:%s\n,newCfg:%s", string(oldInfoFmt), string(newInfoFmt))
		p.cfgCache.colocationCfg = *newCfg
	}
	p.cfgCache.available = true
	p.cfgCache.errorStatus = errorStatus
	return changed
}

func (p *ColocationHandlerForConfigMapEvent) triggerAllNodeEnqueue(q *workqueue.RateLimitingInterface) {
	nodeList := &corev1.NodeList{}
	if err := p.Client.List(context.TODO(), nodeList); err != nil {
		return
	}
	for _, node := range nodeList.Items {
		(*q).Add(reconcile.Request{
			NamespacedName: types.NamespacedName{
				Name: node.Name,
			},
		})
	}
}

func (p *ColocationHandlerForConfigMapEvent) GetCfgCopy() *extension.ColocationCfg {
	p.cfgCache.lock.RLock()
	defer p.cfgCache.lock.RUnlock()
	return p.cfgCache.colocationCfg.DeepCopy()
}

func (p *ColocationHandlerForConfigMapEvent) IsErrorStatus() bool {
	p.cfgCache.lock.RLock()
	defer p.cfgCache.lock.RUnlock()
	return p.cfgCache.errorStatus
}

func (p *ColocationHandlerForConfigMapEvent) IsCfgAvailable() bool {
	p.cfgCache.lock.RLock()
	defer p.cfgCache.lock.RUnlock()
	// if config is available, just return
	if p.cfgCache.available {
		return true
	}
	// if config is not available, try to get the configmap from informer cache;
	// set available if configmap is found or get not found error
	configMap, err := GetConfigMapForCache(p.Client)
	if err != nil {
		klog.Errorf("failed to get configmap %s/%s, colocation cache is unavailable, err: %s",
			ConfigNameSpace, SLOCtrlConfigMap, err)
		return false
	}
	p.syncConfig(configMap)
	klog.V(5).Infof("sync colocation cache from configmap %s/%s, available %v", ConfigNameSpace, SLOCtrlConfigMap, p.cfgCache.available)
	return p.cfgCache.available
}
func GetConfigMapForCache(client client.Client) (*corev1.ConfigMap, error) {
	// try to get the configmap from informer cache;
	// if not found, set configmap to nil and ignore error
	configMap := &corev1.ConfigMap{}
	err := client.Get(context.TODO(), types.NamespacedName{Namespace: ConfigNameSpace, Name: SLOCtrlConfigMap}, configMap)
	if err != nil {
		if !errors.IsNotFound(err) {
			return nil, err
		}
		configMap = nil
	}
	return configMap, nil
}
