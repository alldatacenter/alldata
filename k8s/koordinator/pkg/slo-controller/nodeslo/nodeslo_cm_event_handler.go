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

package nodeslo

import (
	"context"
	"encoding/json"
	"reflect"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var _ handler.EventHandler = &SLOCfgHandlerForConfigMapEvent{}

type SLOCfgCache interface {
	GetCfgCopy() *SLOCfg
	IsCfgAvailable() bool
}

type SLOCfg struct {
	ThresholdCfgMerged   extension.ResourceThresholdCfg `json:"thresholdCfgMerged,omitempty"`
	ResourceQOSCfgMerged extension.ResourceQOSCfg       `json:"resourceQOSCfgMerged,omitempty"`
	CPUBurstCfgMerged    extension.CPUBurstCfg          `json:"cpuBurstCfgMerged,omitempty"`
}

func (in *SLOCfg) DeepCopy() *SLOCfg {
	out := &SLOCfg{}
	out.ThresholdCfgMerged = *in.ThresholdCfgMerged.DeepCopy()
	out.CPUBurstCfgMerged = *in.CPUBurstCfgMerged.DeepCopy()
	out.ResourceQOSCfgMerged = *in.ResourceQOSCfgMerged.DeepCopy()
	return out
}

type sLOCfgCache struct {
	lock sync.RWMutex
	// Config could be concurrently used by the Reconciliation and EventHandler
	sloCfg    SLOCfg
	available bool
}

func DefaultSLOCfg() SLOCfg {
	return SLOCfg{
		ThresholdCfgMerged:   extension.ResourceThresholdCfg{ClusterStrategy: util.DefaultResourceThresholdStrategy()},
		ResourceQOSCfgMerged: extension.ResourceQOSCfg{ClusterStrategy: &slov1alpha1.ResourceQOSStrategy{}},
		CPUBurstCfgMerged:    extension.CPUBurstCfg{ClusterStrategy: util.DefaultCPUBurstStrategy()},
	}
}

type SLOCfgHandlerForConfigMapEvent struct {
	config.EnqueueRequestForConfigMap

	Client   client.Client
	cfgCache sLOCfgCache
	recorder record.EventRecorder
}

func NewSLOCfgHandlerForConfigMapEvent(client client.Client, initCfg SLOCfg, recorder record.EventRecorder) *SLOCfgHandlerForConfigMapEvent {
	sloHandler := &SLOCfgHandlerForConfigMapEvent{cfgCache: sLOCfgCache{sloCfg: initCfg}, Client: client, recorder: recorder}
	sloHandler.SyncCacheIfChanged = sloHandler.syncNodeSLOSpecIfChanged
	sloHandler.EnqueueRequest = sloHandler.triggerAllNodeEnqueue
	return sloHandler
}

func (p *SLOCfgHandlerForConfigMapEvent) triggerAllNodeEnqueue(q *workqueue.RateLimitingInterface) {
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

func (p *SLOCfgHandlerForConfigMapEvent) syncNodeSLOSpecIfChanged(configMap *corev1.ConfigMap) bool {
	p.cfgCache.lock.Lock()
	defer p.cfgCache.lock.Unlock()
	return p.syncConfig(configMap)
}

func (p *SLOCfgHandlerForConfigMapEvent) syncConfig(configMap *corev1.ConfigMap) bool {
	if configMap == nil {
		klog.Warningf("config map is deleted!,use default config")
		return p.updateCacheIfChanged(DefaultSLOCfg())
	}

	var newSLOCfg SLOCfg
	oldSLOCfgCopy := p.cfgCache.sloCfg.DeepCopy()
	var err error
	newSLOCfg.ThresholdCfgMerged, err = calculateResourceThresholdCfgMerged(oldSLOCfgCopy.ThresholdCfgMerged, configMap)
	if err != nil {
		klog.V(5).Infof("failed to get ThresholdCfg, err: %s", err)
		p.recorder.Eventf(configMap, "Warning", config.ReasonSLOConfigUnmarshalFailed, "failed to unmarshal ThresholdCfg, err: %s", err)
	}
	newSLOCfg.ResourceQOSCfgMerged, err = calculateResourceQOSCfgMerged(oldSLOCfgCopy.ResourceQOSCfgMerged, configMap)
	if err != nil {
		klog.V(5).Infof("failed to get ResourceQOSCfg, err: %s", err)
		p.recorder.Eventf(configMap, "Warning", config.ReasonSLOConfigUnmarshalFailed, "failed to unmarshal ResourceQOSCfg, err: %s", err)
	}
	newSLOCfg.CPUBurstCfgMerged, err = calculateCPUBurstCfgMerged(oldSLOCfgCopy.CPUBurstCfgMerged, configMap)
	if err != nil {
		klog.V(5).Infof("failed to get CPUBurstCfg, err: %s", err)
		p.recorder.Eventf(configMap, "Warning", config.ReasonSLOConfigUnmarshalFailed, "failed to unmarshal CPUBurstCfg, err: %s", err)
	}

	return p.updateCacheIfChanged(newSLOCfg)
}

func (p *SLOCfgHandlerForConfigMapEvent) updateCacheIfChanged(newSLOCfg SLOCfg) bool {
	changed := !reflect.DeepEqual(p.cfgCache.sloCfg, newSLOCfg)

	if changed {
		oldInfoFmt, _ := json.MarshalIndent(p.cfgCache.sloCfg, "", "\t")
		newInfoFmt, _ := json.MarshalIndent(newSLOCfg, "", "\t")
		klog.Infof("NodeSLO config Changed success! oldCfg:%s\n,newCfg:%s", string(oldInfoFmt), string(newInfoFmt))
		p.cfgCache.sloCfg = newSLOCfg
	}
	// set the available flag and never change it
	p.cfgCache.available = true
	return changed
}

func (p *SLOCfgHandlerForConfigMapEvent) GetCfgCopy() *SLOCfg {
	p.cfgCache.lock.RLock()
	defer p.cfgCache.lock.RUnlock()
	return p.cfgCache.sloCfg.DeepCopy()
}

func (p *SLOCfgHandlerForConfigMapEvent) IsCfgAvailable() bool {
	p.cfgCache.lock.RLock()
	defer p.cfgCache.lock.RUnlock()
	// if config is available, just return
	if p.cfgCache.available {
		return true
	}
	// if config is not available, try to get the configmap from informer cache;
	// set available if configmap is found or get not found error
	configMap, err := config.GetConfigMapForCache(p.Client)
	if err != nil {
		klog.Errorf("failed to get configmap %s/%s, slo cache is unavailable, err: %s",
			config.ConfigNameSpace, config.SLOCtrlConfigMap, err)
		return false
	}
	p.syncConfig(configMap)
	klog.V(5).Infof("sync slo cache from configmap %s/%s, available %v", config.ConfigNameSpace, config.SLOCtrlConfigMap, p.cfgCache.available)
	return p.cfgCache.available
}
