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

package statesinformer

import (
	"context"
	"encoding/json"
	"reflect"
	"sync"
	"time"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiruntime "k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	koordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	nodeSLOInformerName pluginName = "nodeSLOInformer"
)

type nodeSLOInformer struct {
	nodeSLOInformer cache.SharedIndexInformer
	nodeSLORWMutex  sync.RWMutex
	nodeSLO         *slov1alpha1.NodeSLO

	callbackRunner *callbackRunner
}

func NewNodeSLOInformer() *nodeSLOInformer {
	return &nodeSLOInformer{}
}

func (s *nodeSLOInformer) GetNodeSLO() *slov1alpha1.NodeSLO {
	s.nodeSLORWMutex.RLock()
	defer s.nodeSLORWMutex.RUnlock()
	return s.nodeSLO.DeepCopy()
}

func (s *nodeSLOInformer) Setup(ctx *pluginOption, state *pluginState) {
	s.nodeSLOInformer = newNodeSLOInformer(ctx.KoordClient, ctx.NodeName)
	s.nodeSLOInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			nodeSLO, ok := obj.(*slov1alpha1.NodeSLO)
			if ok {
				s.updateNodeSLOSpec(nodeSLO)
				klog.Infof("create NodeSLO %v", util.DumpJSON(nodeSLO))
			} else {
				klog.Errorf("node slo informer add func parse nodeSLO failed")
			}
		},
		UpdateFunc: func(oldObj, newObj interface{}) {
			oldNodeSLO, oldOK := oldObj.(*slov1alpha1.NodeSLO)
			newNodeSLO, newOK := newObj.(*slov1alpha1.NodeSLO)
			if !oldOK || !newOK {
				klog.Errorf("unable to convert object to *slov1alpha1.NodeSLO, old %T, new %T", oldObj, newObj)
				return
			}
			if reflect.DeepEqual(oldNodeSLO.Spec, newNodeSLO.Spec) {
				klog.V(5).Infof("find NodeSLO spec %s has not changed", newNodeSLO.Name)
				return
			}
			klog.Infof("update NodeSLO spec %v", util.DumpJSON(newNodeSLO.Spec))
			s.updateNodeSLOSpec(newNodeSLO)
		},
	})
	s.callbackRunner = state.callbackRunner
}

func (s *nodeSLOInformer) Start(stopCh <-chan struct{}) {
	klog.V(2).Infof("starting node slo informer")
	go s.nodeSLOInformer.Run(stopCh)
	klog.V(2).Infof("node slo informer started")
}

func (s *nodeSLOInformer) HasSynced() bool {
	if s.nodeSLOInformer == nil {
		return false
	}
	synced := s.nodeSLOInformer.HasSynced()
	klog.V(5).Infof("node slo informer has synced %v", synced)
	return synced
}

func (s *nodeSLOInformer) updateNodeSLOSpec(nodeSLO *slov1alpha1.NodeSLO) {
	s.setNodeSLOSpec(nodeSLO)
	s.callbackRunner.SendCallback(RegisterTypeNodeSLOSpec)
}

func (s *nodeSLOInformer) setNodeSLOSpec(nodeSLO *slov1alpha1.NodeSLO) {
	s.nodeSLORWMutex.Lock()
	defer s.nodeSLORWMutex.Unlock()

	oldNodeSLOStr := util.DumpJSON(s.nodeSLO)

	if s.nodeSLO == nil {
		s.nodeSLO = nodeSLO.DeepCopy()
	} else {
		s.nodeSLO.Spec = nodeSLO.Spec
	}

	// merge nodeSLO spec with the default config
	s.mergeNodeSLOSpec(nodeSLO)

	newNodeSLOStr := util.DumpJSON(s.nodeSLO)
	klog.Infof("update nodeSLO content: old %s, new %s", oldNodeSLOStr, newNodeSLOStr)
}

func (s *nodeSLOInformer) mergeNodeSLOSpec(nodeSLO *slov1alpha1.NodeSLO) {
	if s.nodeSLO == nil || nodeSLO == nil {
		klog.Errorf("failed to merge with nil nodeSLO, old is nil: %v, new is nil: %v", s.nodeSLO == nil, nodeSLO == nil)
		return
	}

	// merge ResourceUsedThresholdWithBE individually for nil-ResourceUsedThresholdWithBE case
	mergedResourceUsedThresholdWithBESpec := mergeSLOSpecResourceUsedThresholdWithBE(util.DefaultNodeSLOSpecConfig().ResourceUsedThresholdWithBE,
		nodeSLO.Spec.ResourceUsedThresholdWithBE)
	if mergedResourceUsedThresholdWithBESpec != nil {
		s.nodeSLO.Spec.ResourceUsedThresholdWithBE = mergedResourceUsedThresholdWithBESpec
	}

	// merge ResourceQOSStrategy
	mergedResourceQOSStrategySpec := mergeSLOSpecResourceQOSStrategy(util.DefaultNodeSLOSpecConfig().ResourceQOSStrategy,
		nodeSLO.Spec.ResourceQOSStrategy)
	mergeNoneResourceQOSIfDisabled(mergedResourceQOSStrategySpec)
	if mergedResourceQOSStrategySpec != nil {
		s.nodeSLO.Spec.ResourceQOSStrategy = mergedResourceQOSStrategySpec
	}

	// merge CPUBurstStrategy
	mergedCPUBurstStrategySpec := mergeSLOSpecCPUBurstStrategy(util.DefaultNodeSLOSpecConfig().CPUBurstStrategy,
		nodeSLO.Spec.CPUBurstStrategy)
	if mergedCPUBurstStrategySpec != nil {
		s.nodeSLO.Spec.CPUBurstStrategy = mergedCPUBurstStrategySpec
	}
}

func newNodeSLOInformer(client koordclientset.Interface, nodeName string) cache.SharedIndexInformer {
	tweakListOptionFunc := func(opt *metav1.ListOptions) {
		opt.FieldSelector = "metadata.name=" + nodeName
	}
	return cache.NewSharedIndexInformer(
		&cache.ListWatch{
			ListFunc: func(options metav1.ListOptions) (apiruntime.Object, error) {
				tweakListOptionFunc(&options)
				return client.SloV1alpha1().NodeSLOs().List(context.TODO(), options)
			},
			WatchFunc: func(options metav1.ListOptions) (watch.Interface, error) {
				tweakListOptionFunc(&options)
				return client.SloV1alpha1().NodeSLOs().Watch(context.TODO(), options)
			},
		},
		&slov1alpha1.NodeSLO{},
		time.Hour*12,
		cache.Indexers{},
	)
}

// mergeSLOSpecResourceUsedThresholdWithBE merges the nodeSLO ResourceUsedThresholdWithBE with default configs
func mergeSLOSpecResourceUsedThresholdWithBE(defaultSpec, newSpec *slov1alpha1.ResourceThresholdStrategy) *slov1alpha1.ResourceThresholdStrategy {
	spec := &slov1alpha1.ResourceThresholdStrategy{}
	if newSpec != nil {
		spec = newSpec
	}
	// ignore err for serializing/deserializing the same struct type
	data, _ := json.Marshal(spec)
	// NOTE: use deepcopy to avoid a overwrite to the global default
	out := defaultSpec.DeepCopy()
	_ = json.Unmarshal(data, &out)
	return out
}

func mergeSLOSpecResourceQOSStrategy(defaultSpec,
	newSpec *slov1alpha1.ResourceQOSStrategy) *slov1alpha1.ResourceQOSStrategy {
	spec := &slov1alpha1.ResourceQOSStrategy{}
	if newSpec != nil {
		spec = newSpec
	}
	// ignore err for serializing/deserializing the same struct type
	data, _ := json.Marshal(spec)
	// NOTE: use deepcopy to avoid a overwrite to the global default
	out := defaultSpec.DeepCopy()
	_ = json.Unmarshal(data, &out)
	return out
}

func mergeSLOSpecCPUBurstStrategy(defaultSpec,
	newSpec *slov1alpha1.CPUBurstStrategy) *slov1alpha1.CPUBurstStrategy {
	spec := &slov1alpha1.CPUBurstStrategy{}
	if newSpec != nil {
		spec = newSpec
	}
	// ignore err for serializing/deserializing the same struct type
	data, _ := json.Marshal(spec)
	// NOTE: use deepcopy to avoid a overwrite to the global default
	out := defaultSpec.DeepCopy()
	_ = json.Unmarshal(data, &out)
	return out
}

// mergeNoneResourceQOSIfDisabled complete ResourceQOSStrategy according to enable statuses of qos features
func mergeNoneResourceQOSIfDisabled(resourceQOS *slov1alpha1.ResourceQOSStrategy) {
	mergeNoneCPUQOSIfDisabled(resourceQOS)
	mergeNoneResctrlQOSIfDisabled(resourceQOS)
	mergeNoneMemoryQOSIfDisabled(resourceQOS)
	klog.V(5).Infof("get merged node ResourceQOS %v", util.DumpJSON(resourceQOS))
}

// mergeNoneResctrlQOSIfDisabled completes node's resctrl qos config according to Enable options in ResctrlQOS
func mergeNoneResctrlQOSIfDisabled(resourceQOS *slov1alpha1.ResourceQOSStrategy) {
	if resourceQOS.LSRClass != nil && resourceQOS.LSRClass.ResctrlQOS != nil &&
		resourceQOS.LSRClass.ResctrlQOS.Enable != nil && !(*resourceQOS.LSRClass.ResctrlQOS.Enable) {
		resourceQOS.LSRClass.ResctrlQOS.ResctrlQOS = *util.NoneResctrlQOS()
	}
	if resourceQOS.LSClass != nil && resourceQOS.LSClass.ResctrlQOS != nil &&
		resourceQOS.LSClass.ResctrlQOS.Enable != nil && !(*resourceQOS.LSClass.ResctrlQOS.Enable) {
		resourceQOS.LSClass.ResctrlQOS.ResctrlQOS = *util.NoneResctrlQOS()
	}
	if resourceQOS.BEClass != nil && resourceQOS.BEClass.ResctrlQOS != nil &&
		resourceQOS.BEClass.ResctrlQOS.Enable != nil && !(*resourceQOS.BEClass.ResctrlQOS.Enable) {
		resourceQOS.BEClass.ResctrlQOS.ResctrlQOS = *util.NoneResctrlQOS()
	}
}

// mergeNoneMemoryQOSIfDisabled completes node's memory qos config according to Enable options in MemoryQOS
func mergeNoneMemoryQOSIfDisabled(resourceQOS *slov1alpha1.ResourceQOSStrategy) {
	// if MemoryQOS.Enable=false, merge with NoneMemoryQOS
	if resourceQOS.LSRClass != nil && resourceQOS.LSRClass.MemoryQOS != nil &&
		resourceQOS.LSRClass.MemoryQOS.Enable != nil && !(*resourceQOS.LSRClass.MemoryQOS.Enable) {
		resourceQOS.LSRClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	}
	if resourceQOS.LSClass != nil && resourceQOS.LSClass.MemoryQOS != nil &&
		resourceQOS.LSClass.MemoryQOS.Enable != nil && !(*resourceQOS.LSClass.MemoryQOS.Enable) {
		resourceQOS.LSClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	}
	if resourceQOS.BEClass != nil && resourceQOS.BEClass.MemoryQOS != nil &&
		resourceQOS.BEClass.MemoryQOS.Enable != nil && !(*resourceQOS.BEClass.MemoryQOS.Enable) {
		resourceQOS.BEClass.MemoryQOS.MemoryQOS = *util.NoneMemoryQOS()
	}
}

func mergeNoneCPUQOSIfDisabled(resourceQOS *slov1alpha1.ResourceQOSStrategy) {
	// if CPUQOS.Enabled=false, merge with NoneCPUQOS
	if resourceQOS.LSRClass != nil && resourceQOS.LSRClass.CPUQOS != nil &&
		resourceQOS.LSRClass.CPUQOS.Enable != nil && !(*resourceQOS.LSRClass.CPUQOS.Enable) {
		resourceQOS.LSRClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	}
	if resourceQOS.LSClass != nil && resourceQOS.LSClass.CPUQOS != nil &&
		resourceQOS.LSClass.CPUQOS.Enable != nil && !(*resourceQOS.LSClass.CPUQOS.Enable) {
		resourceQOS.LSClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	}
	if resourceQOS.BEClass != nil && resourceQOS.BEClass.CPUQOS != nil &&
		resourceQOS.BEClass.CPUQOS.Enable != nil && !(*resourceQOS.BEClass.CPUQOS.Enable) {
		resourceQOS.BEClass.CPUQOS.CPUQOS = *util.NoneCPUQOS()
	}
}
