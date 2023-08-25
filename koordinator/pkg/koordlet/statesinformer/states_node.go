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
	"reflect"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiruntime "k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/watch"
	clientset "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	nodeInformerName pluginName = "nodeInformer"
)

type nodeInformer struct {
	nodeInformer cache.SharedIndexInformer
	nodeRWMutex  sync.RWMutex
	node         *corev1.Node
}

func NewNodeInformer() *nodeInformer {
	return &nodeInformer{}
}

func (s *nodeInformer) GetNode() *corev1.Node {
	s.nodeRWMutex.RLock()
	defer s.nodeRWMutex.RUnlock()
	if s.node == nil {
		return nil
	}
	return s.node.DeepCopy()
}

func (s *nodeInformer) Setup(ctx *pluginOption, state *pluginState) {
	s.nodeInformer = newNodeInformer(ctx.KubeClient, ctx.NodeName)
	s.nodeInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			node, ok := obj.(*corev1.Node)
			if ok {
				s.syncNode(node)
			} else {
				klog.Errorf("node informer add func parse Node failed, obj %T", obj)
			}
		},
		UpdateFunc: func(oldObj, newObj interface{}) {
			oldNode, oldOK := oldObj.(*corev1.Node)
			newNode, newOK := newObj.(*corev1.Node)
			if !oldOK || !newOK {
				klog.Errorf("unable to convert object to *corev1.Node, old %T, new %T", oldObj, newObj)
				return
			}
			if reflect.DeepEqual(oldNode, newNode) {
				klog.V(5).Infof("find node %s has not changed", newNode.Name)
				return
			}
			s.syncNode(newNode)
		},
	})
}

func (s *nodeInformer) Start(stopCh <-chan struct{}) {
	klog.V(2).Infof("starting node informer")
	go s.nodeInformer.Run(stopCh)
	klog.V(2).Infof("node informer started")
}

func (s *nodeInformer) HasSynced() bool {
	if s.nodeInformer == nil {
		return false
	}
	synced := s.nodeInformer.HasSynced()
	klog.V(5).Infof("node informer has synced %v", synced)
	return synced
}

func newNodeInformer(client clientset.Interface, nodeName string) cache.SharedIndexInformer {
	tweakListOptionsFunc := func(opt *metav1.ListOptions) {
		opt.FieldSelector = "metadata.name=" + nodeName
	}

	return cache.NewSharedIndexInformer(
		&cache.ListWatch{
			ListFunc: func(options metav1.ListOptions) (apiruntime.Object, error) {
				tweakListOptionsFunc(&options)
				return client.CoreV1().Nodes().List(context.TODO(), options)
			},
			WatchFunc: func(options metav1.ListOptions) (watch.Interface, error) {
				tweakListOptionsFunc(&options)
				return client.CoreV1().Nodes().Watch(context.TODO(), options)
			},
		},
		&corev1.Node{},
		time.Hour*12,
		cache.Indexers{},
	)
}

func (s *nodeInformer) setupNodeInformer() {
	s.nodeInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			node, ok := obj.(*corev1.Node)
			if ok {
				s.syncNode(node)
			} else {
				klog.Errorf("node informer add func parse Node failed, obj %T", obj)
			}
		},
		UpdateFunc: func(oldObj, newObj interface{}) {
			oldNode, oldOK := oldObj.(*corev1.Node)
			newNode, newOK := newObj.(*corev1.Node)
			if !oldOK || !newOK {
				klog.Errorf("unable to convert object to *corev1.Node, old %T, new %T", oldObj, newObj)
				return
			}
			if reflect.DeepEqual(oldNode, newNode) {
				klog.V(5).Infof("find node %s has not changed", newNode.Name)
				return
			}
			s.syncNode(newNode)
		},
	})
}

func (s *nodeInformer) syncNode(newNode *corev1.Node) {
	klog.V(5).Infof("node update detail %v", newNode)
	s.nodeRWMutex.Lock()
	defer s.nodeRWMutex.Unlock()
	s.node = newNode.DeepCopy()

	// also register node for metrics
	recordNodeResourceMetrics(newNode)
}

func recordNodeResourceMetrics(node *corev1.Node) {
	// register node labels
	metrics.Register(node)
	// record node resource metrics
	recordNodeResources(node)

	klog.V(5).Info("record node prometheus metrics successfully")
}

func recordNodeResources(node *corev1.Node) {
	if node == nil || node.Status.Allocatable == nil {
		klog.V(4).Infof("failed to record node resources metrics, node is invalid: %v", node)
		return
	}

	// record node allocatable of BatchCPU & BatchMemory
	if q, ok := node.Status.Allocatable[apiext.BatchCPU]; ok {
		metrics.RecordNodeResourceAllocatable(string(apiext.BatchCPU), float64(util.QuantityPtr(q).Value()))
	} else {
		metrics.RecordNodeResourceAllocatable(string(apiext.BatchCPU), 0)
	}
	if q, ok := node.Status.Allocatable[apiext.BatchMemory]; ok {
		metrics.RecordNodeResourceAllocatable(string(apiext.BatchMemory), float64(util.QuantityPtr(q).Value()))
	} else {
		metrics.RecordNodeResourceAllocatable(string(apiext.BatchMemory), 0)
	}
}
