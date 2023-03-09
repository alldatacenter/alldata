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

package descheduler

import (
	"context"
	"errors"
	"fmt"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/dynamic/dynamicinformer"
	"k8s.io/client-go/informers"
	corev1informers "k8s.io/client-go/informers/core/v1"
	clientset "k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/klog/v2"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/scheme"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	frameworkplugins "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/plugins"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/metrics"
	nodeutil "github.com/koordinator-sh/koordinator/pkg/descheduler/node"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/profile"
)

type Descheduler struct {
	// Profiles are the descheduling profiles.
	Profiles profile.Map

	// Close this to shut down the scheduler.
	StopEverything <-chan struct{}

	clientSet    clientset.Interface
	nodeInformer corev1informers.NodeInformer

	dryRun               bool
	deschedulingInterval time.Duration
	nodeSelector         string
}

type deschedulerOptions struct {
	componentConfigVersion string
	kubeConfig             *restclient.Config
	frameworkCapturer      FrameworkCapturer
	podAssignedToNodeFn    PodAssignedToNodeFn
	outOfTreeRegistry      frameworkruntime.Registry
	profiles               []deschedulerconfig.DeschedulerProfile
	applyDefaultProfile    bool
	dryRun                 bool
	deschedulingInterval   time.Duration
	nodeSelector           *metav1.LabelSelector
}

// Option configures a Scheduler
type Option func(*deschedulerOptions)

// WithComponentConfigVersion sets the component config version to the
// DeschedulerConfiguration version used. The string should be the full
// scheme group/version of the external type we converted from (for example
// "descheduler/v1alpha2")
func WithComponentConfigVersion(apiVersion string) Option {
	return func(o *deschedulerOptions) {
		o.componentConfigVersion = apiVersion
	}
}

func WithKubeConfig(cfg *restclient.Config) Option {
	return func(o *deschedulerOptions) {
		o.kubeConfig = cfg
	}
}

func WithProfiles(p ...deschedulerconfig.DeschedulerProfile) Option {
	return func(o *deschedulerOptions) {
		o.profiles = p
		o.applyDefaultProfile = false
	}
}

func WithDryRun(dryRun bool) Option {
	return func(options *deschedulerOptions) {
		options.dryRun = dryRun
	}
}

func WithNodeSelector(nodeSelector *metav1.LabelSelector) Option {
	return func(options *deschedulerOptions) {
		options.nodeSelector = nodeSelector
	}
}

func WithDeschedulingInterval(interval time.Duration) Option {
	return func(options *deschedulerOptions) {
		options.deschedulingInterval = interval
	}
}

// WithFrameworkOutOfTreeRegistry sets the registry for out-of-tree plugins. Those plugins
// will be appended to the default registry.
func WithFrameworkOutOfTreeRegistry(registry frameworkruntime.Registry) Option {
	return func(o *deschedulerOptions) {
		o.outOfTreeRegistry = registry
	}
}

// FrameworkCapturer is used for registering a notify function in building framework.
type FrameworkCapturer func(deschedulerconfig.DeschedulerProfile)

// WithBuildFrameworkCapturer sets a notify function for getting buildFramework details.
func WithBuildFrameworkCapturer(fc FrameworkCapturer) Option {
	return func(o *deschedulerOptions) {
		o.frameworkCapturer = fc
	}
}

type PodAssignedToNodeFn func(nodeName string) ([]*corev1.Pod, error)

func WithPodAssignedToNodeFn(fn PodAssignedToNodeFn) Option {
	return func(options *deschedulerOptions) {
		options.podAssignedToNodeFn = fn
	}
}

var defaultDeschedulerOptions = deschedulerOptions{
	applyDefaultProfile: true,
}

func New(client clientset.Interface,
	informerFactory informers.SharedInformerFactory,
	dynInformerFactory dynamicinformer.DynamicSharedInformerFactory,
	recorderFactory profile.RecorderFactory,
	stopCh <-chan struct{},
	opts ...Option,
) (*Descheduler, error) {
	stopEverything := stopCh
	if stopEverything == nil {
		stopEverything = wait.NeverStop
	}

	options := defaultDeschedulerOptions
	for _, opt := range opts {
		opt(&options)
	}

	if options.applyDefaultProfile {
		var versionedCfg v1alpha2.DeschedulerConfiguration
		scheme.Scheme.Default(&versionedCfg)
		cfg := deschedulerconfig.DeschedulerConfiguration{}
		if err := scheme.Scheme.Convert(&versionedCfg, &cfg, nil); err != nil {
			return nil, err
		}
		options.profiles = cfg.Profiles
	}

	var nodeSelector string
	if options.nodeSelector != nil {
		selector, err := metav1.LabelSelectorAsSelector(options.nodeSelector)
		if err != nil {
			return nil, err
		}
		nodeSelector = selector.String()
	}

	nodeInformer := informerFactory.Core().V1().Nodes()
	podInformer := informerFactory.Core().V1().Pods()
	namespaceInformer := informerFactory.Core().V1().Namespaces()
	priorityClassInformer := informerFactory.Scheduling().V1().PriorityClasses()

	// create the informers before starting the informer factory
	nodeInformer.Informer()
	podInformer.Informer()
	namespaceInformer.Informer()
	priorityClassInformer.Informer()

	registry := frameworkplugins.NewInTreeRegistry()
	if err := registry.Merge(options.outOfTreeRegistry); err != nil {
		return nil, err
	}

	metrics.Register()

	profiles, err := profile.NewMap(
		options.profiles,
		registry,
		recorderFactory,
		frameworkruntime.WithClientSet(client),
		frameworkruntime.WithKubeConfig(options.kubeConfig),
		frameworkruntime.WithSharedInformerFactory(informerFactory),
		frameworkruntime.WithGetPodsAssignedToNodeFunc(podAssignedToNodeAdaptor(options.podAssignedToNodeFn)),
		frameworkruntime.WithCaptureProfile(frameworkruntime.CaptureProfile(options.frameworkCapturer)),
	)
	if err != nil {
		return nil, fmt.Errorf("initializing profiles: %v", err)
	}

	if len(profiles) == 0 {
		return nil, errors.New("at least one profile is required")
	}

	descheduler := &Descheduler{
		Profiles:             profiles,
		StopEverything:       stopEverything,
		clientSet:            client,
		nodeInformer:         nodeInformer,
		dryRun:               options.dryRun,
		deschedulingInterval: options.deschedulingInterval,
		nodeSelector:         nodeSelector,
	}
	return descheduler, nil
}

func (d *Descheduler) Start(ctx context.Context) error {
	ctx, cancel := context.WithCancel(ctx)
	defer cancel()

	wait.NonSlidingUntil(func() {
		if err := d.deschedulerOnce(ctx); err != nil {
			klog.Errorf("Error descheduling pods: %v", err)
		}

		// If there was no interval specified, send a signal to the stopChannel to end the wait.Until loop after 1 iteration
		if d.deschedulingInterval == 0 {
			cancel()
			return
		}
	}, d.deschedulingInterval, ctx.Done())
	return nil
}

func (d *Descheduler) deschedulerOnce(ctx context.Context) error {
	nodes, err := nodeutil.ReadyNodes(ctx, d.clientSet, d.nodeInformer, d.nodeSelector)
	if err != nil {
		return fmt.Errorf("unable to get ready nodes: %v", err)
	}

	if len(nodes) <= 1 {
		return fmt.Errorf("the cluster size is 0 or 1 meaning eviction causes service disruption or degradation")
	}

	for _, p := range d.Profiles {
		status := p.RunDeschedulePlugins(ctx, nodes)
		if status != nil && status.Err != nil {
			return status.Err
		}
	}

	for _, p := range d.Profiles {
		status := p.RunBalancePlugins(ctx, nodes)
		if status != nil && status.Err != nil {
			return status.Err
		}
	}

	return nil
}

func podAssignedToNodeAdaptor(fn PodAssignedToNodeFn) framework.GetPodsAssignedToNodeFunc {
	return func(nodeName string, filterFunc framework.FilterFunc) ([]*corev1.Pod, error) {
		if fn == nil {
			return nil, nil
		}
		pods, err := fn(nodeName)
		if err != nil {
			return nil, err
		}
		if len(pods) == 0 {
			return nil, nil
		}
		result := make([]*corev1.Pod, 0, len(pods))
		for _, v := range pods {
			if filterFunc(v) {
				result = append(result, v)
			}
		}
		return result, nil
	}
}
