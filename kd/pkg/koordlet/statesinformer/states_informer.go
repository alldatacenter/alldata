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
	"fmt"
	"sync"

	topov1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	topologyclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned"
	_ "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned/scheme"
	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	clientset "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	koordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	schedv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
)

const (
	HTTPScheme  = "http"
	HTTPSScheme = "https"
)

type StatesInformer interface {
	Run(stopCh <-chan struct{}) error
	HasSynced() bool

	GetNode() *corev1.Node
	GetNodeSLO() *slov1alpha1.NodeSLO

	GetAllPods() []*PodMeta

	GetNodeTopo() *topov1alpha1.NodeResourceTopology

	RegisterCallbacks(objType RegisterType, name, description string, callbackFn UpdateCbFn)
}

type pluginName string

type pluginOption struct {
	config      *Config
	KubeClient  clientset.Interface
	KoordClient koordclientset.Interface
	TopoClient  topologyclientset.Interface
	NodeName    string
}

type pluginState struct {
	metricCache     metriccache.MetricCache
	callbackRunner  *callbackRunner
	informerPlugins map[pluginName]informerPlugin
}

type GetGPUDriverAndModelFunc func() (string, string)

type statesInformer struct {
	// TODO refactor device as plugin
	config       *Config
	metricsCache metriccache.MetricCache
	deviceClient schedv1alpha1.DeviceInterface
	unhealthyGPU map[string]struct{}
	gpuMutex     sync.RWMutex

	option  *pluginOption
	states  *pluginState
	started *atomic.Bool

	getGPUDriverAndModelFunc GetGPUDriverAndModelFunc
}

type informerPlugin interface {
	Setup(ctx *pluginOption, state *pluginState)
	Start(stopCh <-chan struct{})
	HasSynced() bool
}

// TODO merge all clients into one struct
func NewStatesInformer(config *Config, kubeClient clientset.Interface, crdClient koordclientset.Interface, topologyClient topologyclientset.Interface, metricsCache metriccache.MetricCache, nodeName string, schedulingClient schedv1alpha1.SchedulingV1alpha1Interface) StatesInformer {
	opt := &pluginOption{
		config:      config,
		KubeClient:  kubeClient,
		KoordClient: crdClient,
		TopoClient:  topologyClient,
		NodeName:    nodeName,
	}
	stat := &pluginState{
		metricCache:     metricsCache,
		informerPlugins: map[pluginName]informerPlugin{},
		callbackRunner:  NewCallbackRunner(),
	}
	s := &statesInformer{
		config:       config,
		metricsCache: metricsCache,
		deviceClient: schedulingClient.Devices(),
		unhealthyGPU: make(map[string]struct{}),

		option:  opt,
		states:  stat,
		started: atomic.NewBool(false),
	}
	s.getGPUDriverAndModelFunc = s.getGPUDriverAndModel
	s.initInformerPlugins()
	return s
}

func (s *statesInformer) setupPlugins() {
	for name, plugin := range s.states.informerPlugins {
		plugin.Setup(s.option, s.states)
		klog.V(2).Infof("plugin %v has been setup", name)
	}
}

func (s *statesInformer) Run(stopCh <-chan struct{}) error {
	defer utilruntime.HandleCrash()
	klog.V(2).Infof("setup statesInformer")

	klog.V(2).Infof("starting callback runner")
	s.states.callbackRunner.Setup(s)
	go s.states.callbackRunner.Start(stopCh)

	klog.V(2).Infof("starting informer plugins")
	s.setupPlugins()
	s.startPlugins(stopCh)

	// waiting for node synced.
	klog.V(2).Infof("waiting for informer syncing")
	waitInformersSynced := s.waitForSyncFunc()
	if !cache.WaitForCacheSync(stopCh, waitInformersSynced...) {
		return fmt.Errorf("timed out waiting for states informer caches to sync")
	}

	if features.DefaultKoordletFeatureGate.Enabled(features.Accelerators) {
		go wait.Until(s.reportDevice, s.config.NodeTopologySyncInterval, stopCh)
		// check is nvml is available
		if s.initGPU() {
			go s.gpuHealCheck(stopCh)
		}
	}

	klog.Infof("start states informer successfully")
	s.started.Store(true)
	<-stopCh
	klog.Infof("shutting down states informer daemon")
	return nil
}

func (s *statesInformer) waitForSyncFunc() []cache.InformerSynced {
	waitInformersSynced := make([]cache.InformerSynced, 0, len(s.states.informerPlugins))
	for _, p := range s.states.informerPlugins {
		waitInformersSynced = append(waitInformersSynced, p.HasSynced)
	}
	return waitInformersSynced
}

func (s *statesInformer) startPlugins(stopCh <-chan struct{}) {
	for name, p := range s.states.informerPlugins {
		klog.V(4).Infof("starting informer plugin %v", name)
		go p.Start(stopCh)
	}
}

func (s *statesInformer) HasSynced() bool {
	for _, p := range s.states.informerPlugins {
		if !p.HasSynced() {
			return false
		}
	}
	return true
}

func (s *statesInformer) GetNode() *corev1.Node {
	nodeInformerIf := s.states.informerPlugins[nodeInformerName]
	nodeInformer, ok := nodeInformerIf.(*nodeInformer)
	if !ok {
		klog.Fatalf("node informer format error")
	}
	return nodeInformer.GetNode()
}

func (s *statesInformer) GetNodeSLO() *slov1alpha1.NodeSLO {
	nodeSLOInformerIf := s.states.informerPlugins[nodeSLOInformerName]
	nodeSLOInformer, ok := nodeSLOInformerIf.(*nodeSLOInformer)
	if !ok {
		klog.Fatalf("node slo informer format error")
	}
	return nodeSLOInformer.GetNodeSLO()
}

func (s *statesInformer) GetNodeTopo() *topov1alpha1.NodeResourceTopology {
	nodeTopoInformerIf := s.states.informerPlugins[nodeTopoInformerName]
	nodeTopoInformer, ok := nodeTopoInformerIf.(*nodeTopoInformer)
	if !ok {
		klog.Fatalf("node topo informer format error")
	}
	return nodeTopoInformer.GetNodeTopo()
}

func (s *statesInformer) GetAllPods() []*PodMeta {
	podsInformerIf := s.states.informerPlugins[podsInformerName]
	podsInformer, ok := podsInformerIf.(*podsInformer)
	if !ok {
		klog.Fatalf("pods informer format error")
	}
	return podsInformer.GetAllPods()
}

func (s *statesInformer) RegisterCallbacks(rType RegisterType, name, description string, callbackFn UpdateCbFn) {
	s.states.callbackRunner.RegisterCallbacks(rType, name, description, callbackFn)
}
