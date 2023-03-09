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

package frameworkext

import (
	"context"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	frameworkruntime "k8s.io/kubernetes/pkg/scheduler/framework/runtime"

	koordinatorclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/services"
)

// ExtendedHandle extends the k8s scheduling framework Handle interface
// to facilitate plugins to access Koordinator's resources and states.
type ExtendedHandle interface {
	framework.Handle
	KoordinatorClientSet() koordinatorclientset.Interface
	KoordinatorSharedInformerFactory() koordinatorinformers.SharedInformerFactory
	SnapshotSharedLister() framework.SharedLister
	Run()
}

type extendedHandleOptions struct {
	servicesEngine                   *services.Engine
	koordinatorClientSet             koordinatorclientset.Interface
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory
	sharedListerAdapter              SharedListerAdapter
}

type SharedListerAdapter func(lister framework.SharedLister) framework.SharedLister

type Option func(*extendedHandleOptions)

func WithServicesEngine(engine *services.Engine) Option {
	return func(options *extendedHandleOptions) {
		options.servicesEngine = engine
	}
}

func WithKoordinatorClientSet(koordinatorClientSet koordinatorclientset.Interface) Option {
	return func(options *extendedHandleOptions) {
		options.koordinatorClientSet = koordinatorClientSet
	}
}

func WithKoordinatorSharedInformerFactory(informerFactory koordinatorinformers.SharedInformerFactory) Option {
	return func(options *extendedHandleOptions) {
		options.koordinatorSharedInformerFactory = informerFactory
	}
}

func WithSharedListerFactory(adapter SharedListerAdapter) Option {
	return func(options *extendedHandleOptions) {
		options.sharedListerAdapter = adapter
	}
}

type frameworkExtendedHandleImpl struct {
	once sync.Once
	framework.Handle
	servicesEngine                   *services.Engine
	koordinatorClientSet             koordinatorclientset.Interface
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory
	sharedListerAdapter              SharedListerAdapter
	controllerMaps                   *ControllersMap
}

func NewExtendedHandle(options ...Option) ExtendedHandle {
	handleOptions := &extendedHandleOptions{}
	for _, opt := range options {
		opt(handleOptions)
	}

	return &frameworkExtendedHandleImpl{
		servicesEngine:                   handleOptions.servicesEngine,
		koordinatorClientSet:             handleOptions.koordinatorClientSet,
		koordinatorSharedInformerFactory: handleOptions.koordinatorSharedInformerFactory,
		sharedListerAdapter:              handleOptions.sharedListerAdapter,
		controllerMaps:                   NewControllersMap(),
	}
}

func (ext *frameworkExtendedHandleImpl) Run() {
	go ext.controllerMaps.Start()
}

func (ext *frameworkExtendedHandleImpl) KoordinatorClientSet() koordinatorclientset.Interface {
	return ext.koordinatorClientSet
}

func (ext *frameworkExtendedHandleImpl) KoordinatorSharedInformerFactory() koordinatorinformers.SharedInformerFactory {
	return ext.koordinatorSharedInformerFactory
}

func (ext *frameworkExtendedHandleImpl) SnapshotSharedLister() framework.SharedLister {
	if ext.sharedListerAdapter != nil {
		return ext.sharedListerAdapter(ext.Handle.SnapshotSharedLister())
	}
	return ext.Handle.SnapshotSharedLister()

}

type FrameworkExtender interface {
	framework.Framework
}

type FrameworkExtenderFactory interface {
	New(f framework.Framework) FrameworkExtender
}

type SchedulingPhaseHook interface {
	Name() string
}

type PreFilterPhaseHook interface {
	SchedulingPhaseHook
	PreFilterHook(handle ExtendedHandle, state *framework.CycleState, pod *corev1.Pod) (*corev1.Pod, bool)
}

type FilterPhaseHook interface {
	SchedulingPhaseHook
	FilterHook(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool)
}

type ScorePhaseHook interface {
	SchedulingPhaseHook
	ScoreHook(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) (*corev1.Pod, []*corev1.Node, bool)
}

type frameworkExtenderFactoryImpl struct {
	handle ExtendedHandle

	// extend framework with SchedulingPhaseHook
	preFilterHooks []PreFilterPhaseHook
	filterHooks    []FilterPhaseHook
	scoreHooks     []ScorePhaseHook
}

func NewFrameworkExtenderFactory(handle ExtendedHandle, hooks ...SchedulingPhaseHook) FrameworkExtenderFactory {
	i := &frameworkExtenderFactoryImpl{
		handle: handle,
	}
	for _, h := range hooks {
		// a hook may register in multiple phases
		preFilter, ok := h.(PreFilterPhaseHook)
		if ok {
			i.preFilterHooks = append(i.preFilterHooks, preFilter)
			klog.V(4).InfoS("framework extender got scheduling hooks registered", "preFilter", preFilter.Name())
		}
		filter, ok := h.(FilterPhaseHook)
		if ok {
			i.filterHooks = append(i.filterHooks, filter)
			klog.V(4).InfoS("framework extender got scheduling hooks registered", "filter", filter.Name())
		}
		score, ok := h.(ScorePhaseHook)
		if ok {
			i.scoreHooks = append(i.scoreHooks, score)
			klog.V(4).InfoS("framework extender got scheduling hooks registered", "score", score.Name())
		}
	}
	return i
}

func (i *frameworkExtenderFactoryImpl) New(f framework.Framework) FrameworkExtender {
	return &frameworkExtenderImpl{
		Framework:      f,
		handle:         i.handle,
		preFilterHooks: i.preFilterHooks,
		filterHooks:    i.filterHooks,
		scoreHooks:     i.scoreHooks,
	}
}

var _ framework.Framework = &frameworkExtenderImpl{}

type frameworkExtenderImpl struct {
	framework.Framework
	handle ExtendedHandle

	preFilterHooks []PreFilterPhaseHook
	filterHooks    []FilterPhaseHook
	scoreHooks     []ScorePhaseHook
}

// RunPreFilterPlugins hooks the PreFilter phase of framework with pre-filter hooks.
func (ext *frameworkExtenderImpl) RunPreFilterPlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	for _, hook := range ext.preFilterHooks {
		newPod, hooked := hook.PreFilterHook(ext.handle, cycleState, pod)
		if hooked {
			klog.V(5).InfoS("RunPreFilterPlugins hooked", "hook", hook.Name(), "pod", klog.KObj(pod))
			pod = newPod
		}
	}
	return ext.Framework.RunPreFilterPlugins(ctx, cycleState, pod)
}

// RunFilterPluginsWithNominatedPods hooks the Filter phase of framework with filter hooks.
// We don't hook RunFilterPlugins since framework's RunFilterPluginsWithNominatedPods just calls its RunFilterPlugins.
func (ext *frameworkExtenderImpl) RunFilterPluginsWithNominatedPods(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	for _, hook := range ext.filterHooks {
		// hook can change the args (cycleState, pod, nodeInfo) for filter plugins
		newPod, newNodeInfo, hooked := hook.FilterHook(ext.handle, cycleState, pod, nodeInfo)
		if hooked {
			klog.V(5).InfoS("RunFilterPluginsWithNominatedPods hooked", "hook", hook.Name(), "pod", klog.KObj(pod))
			pod = newPod
			nodeInfo = newNodeInfo
		}
	}
	status := ext.Framework.RunFilterPluginsWithNominatedPods(ctx, cycleState, pod, nodeInfo)
	if !status.IsSuccess() && debugFilterFailure {
		klog.Infof("Failed to filter for Pod %q on Node %q, failedPlugin: %s, reason: %s", klog.KObj(pod), klog.KObj(nodeInfo.Node()), status.FailedPlugin(), status.Message())
	}
	return status
}

func (ext *frameworkExtenderImpl) RunScorePlugins(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) (framework.PluginToNodeScores, *framework.Status) {
	for _, hook := range ext.scoreHooks {
		// hook can change the args (cycleState, pod, nodeInfo) for score plugins
		newPod, newNodes, hooked := hook.ScoreHook(ext.handle, state, pod, nodes)
		if hooked {
			klog.V(5).InfoS("RunScorePlugins hooked", "hook", hook.Name(), "pod", klog.KObj(pod))
			pod = newPod
			nodes = newNodes
		}
	}
	pluginToNodeScores, status := ext.Framework.RunScorePlugins(ctx, state, pod, nodes)
	if status.IsSuccess() && debugTopNScores > 0 {
		debugScores(debugTopNScores, pod, pluginToNodeScores, nodes)
	}
	return pluginToNodeScores, status
}

// PluginFactoryProxy is used to proxy the call to the PluginFactory function and pass in the ExtendedHandle for the custom plugin
func PluginFactoryProxy(extendHandle ExtendedHandle, factoryFn frameworkruntime.PluginFactory) frameworkruntime.PluginFactory {
	return func(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
		impl := extendHandle.(*frameworkExtendedHandleImpl)
		impl.once.Do(func() {
			impl.Handle = handle
		})
		plugin, err := factoryFn(args, extendHandle)
		if err != nil {
			return nil, err
		}
		if impl.servicesEngine != nil {
			impl.servicesEngine.RegisterPluginService(plugin)
		}
		if impl.controllerMaps != nil {
			impl.controllerMaps.RegisterControllers(plugin)
		}
		return plugin, nil
	}
}
