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

package runtime

import (
	"context"
	"fmt"
	"reflect"
	"sort"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/errors"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/informers"
	clientset "k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/events"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

type frameworkImpl struct {
	clientSet                 clientset.Interface
	kubeConfig                *restclient.Config
	eventRecorder             events.EventRecorder
	sharedInformerFactory     informers.SharedInformerFactory
	getPodsAssignedToNodeFunc framework.GetPodsAssignedToNodeFunc
	deschedulePlugins         []framework.DeschedulePlugin
	balancePlugins            []framework.BalancePlugin
	evictorPlugins            []framework.Evictor
}

// Option for the frameworkImpl.
type Option func(*frameworkOptions)

type frameworkOptions struct {
	clientSet                 clientset.Interface
	kubeConfig                *restclient.Config
	eventRecorder             events.EventRecorder
	sharedInformerFactory     informers.SharedInformerFactory
	getPodsAssignedToNodeFunc framework.GetPodsAssignedToNodeFunc
	captureProfile            CaptureProfile
}

// WithClientSet sets clientSet for the scheduling Framework.
func WithClientSet(clientSet clientset.Interface) Option {
	return func(o *frameworkOptions) {
		o.clientSet = clientSet
	}
}

// WithKubeConfig sets kubeConfig for the scheduling frameworkImpl.
func WithKubeConfig(kubeConfig *restclient.Config) Option {
	return func(o *frameworkOptions) {
		o.kubeConfig = kubeConfig
	}
}

func WithSharedInformerFactory(sharedInformerFactory informers.SharedInformerFactory) Option {
	return func(o *frameworkOptions) {
		o.sharedInformerFactory = sharedInformerFactory
	}
}

func WithGetPodsAssignedToNodeFunc(fn framework.GetPodsAssignedToNodeFunc) Option {
	return func(opts *frameworkOptions) {
		opts.getPodsAssignedToNodeFunc = fn
	}
}

// CaptureProfile is a callback to capture a finalized profile.
type CaptureProfile func(profile deschedulerconfig.DeschedulerProfile)

// WithCaptureProfile sets a callback to capture the finalized profile.
func WithCaptureProfile(c CaptureProfile) Option {
	return func(o *frameworkOptions) {
		o.captureProfile = c
	}
}

// WithEventRecorder sets clientSet for the scheduling frameworkImpl.
func WithEventRecorder(recorder events.EventRecorder) Option {
	return func(o *frameworkOptions) {
		o.eventRecorder = recorder
	}
}

func NewFramework(r Registry, profile *deschedulerconfig.DeschedulerProfile, opts ...Option) (framework.Handle, error) {
	options := &frameworkOptions{}
	for _, optFnc := range opts {
		optFnc(options)
	}

	f := &frameworkImpl{
		clientSet:                 options.clientSet,
		kubeConfig:                options.kubeConfig,
		eventRecorder:             options.eventRecorder,
		sharedInformerFactory:     options.sharedInformerFactory,
		getPodsAssignedToNodeFunc: options.getPodsAssignedToNodeFunc,
	}

	if profile == nil || profile.Plugins == nil {
		return f, nil
	}

	pluginConfig := make(map[string]runtime.Object, len(profile.PluginConfig))
	for i := range profile.PluginConfig {
		name := profile.PluginConfig[i].Name
		if _, ok := pluginConfig[name]; ok {
			return nil, fmt.Errorf("repeated config for plugin %s", name)
		}
		pluginConfig[name] = profile.PluginConfig[i].Args
	}
	outputProfile := deschedulerconfig.DeschedulerProfile{
		Name:    profile.Name,
		Plugins: profile.Plugins,
	}

	pluginsMap := make(map[string]framework.Plugin)

	// Other plugins may depend on the evictor plugin, so we should initialize the evictor plugin first.
	evictorExtensionPoint := f.getEvictorExtensionPoint(profile.Plugins)
	outputPluginConfig, err := f.initPlugins(r, pluginConfig, evictorExtensionPoint, pluginsMap)
	if err != nil {
		return nil, err
	}

	if len(f.evictorPlugins) == 0 {
		return nil, fmt.Errorf("no evict plugin is enabled")
	}
	if len(f.evictorPlugins) > 1 {
		return nil, fmt.Errorf("only one evict plugin can be enabled")
	}

	outputProfile.PluginConfig = append(outputProfile.PluginConfig, outputPluginConfig...)

	otherExtensionPoints := f.getOtherExtensionPoints(profile.Plugins)
	outputPluginConfig, err = f.initPlugins(r, pluginConfig, otherExtensionPoints, pluginsMap)
	if err != nil {
		return nil, err
	}
	outputProfile.PluginConfig = append(outputProfile.PluginConfig, outputPluginConfig...)

	if options.captureProfile != nil {
		if len(outputProfile.PluginConfig) != 0 {
			sort.Slice(outputProfile.PluginConfig, func(i, j int) bool {
				return outputProfile.PluginConfig[i].Name < outputProfile.PluginConfig[j].Name
			})
		} else {
			outputProfile.PluginConfig = nil
		}
		options.captureProfile(outputProfile)
	}

	return f, nil
}

func (f *frameworkImpl) initPlugins(r Registry, pluginConfig map[string]runtime.Object, extensionPoints []extensionPoint, pluginsMap map[string]framework.Plugin) ([]deschedulerconfig.PluginConfig, error) {
	pg := sets.NewString()
	pluginsNeeded(pg, extensionPoints)

	var outputPluginConfig []deschedulerconfig.PluginConfig
	for name, factory := range r {
		// initialize only needed plugins.
		if !pg.Has(name) {
			continue
		}

		// initialize plugins that have not yet been created
		if _, ok := pluginsMap[name]; ok {
			continue
		}

		args := pluginConfig[name]
		if args != nil {
			outputPluginConfig = append(outputPluginConfig, deschedulerconfig.PluginConfig{
				Name: name,
				Args: args,
			})
		}
		p, err := factory(args, f)
		if err != nil {
			return nil, fmt.Errorf("initializing plugin %q: %w", name, err)
		}
		pluginsMap[name] = p
	}

	// initialize plugins per individual extension points
	for _, e := range extensionPoints {
		if err := updatePluginList(e.slicePtr, *e.plugins, pluginsMap); err != nil {
			return nil, err
		}
	}

	return outputPluginConfig, nil
}

func updatePluginList(pluginList interface{}, pluginSet deschedulerconfig.PluginSet, pluginsMap map[string]framework.Plugin) error {
	plugins := reflect.ValueOf(pluginList).Elem()
	pluginType := plugins.Type().Elem()
	set := sets.NewString()
	for _, ep := range pluginSet.Enabled {
		pg, ok := pluginsMap[ep.Name]
		if !ok {
			return fmt.Errorf("%s %q does not exist", pluginType.Name(), ep.Name)
		}

		if !reflect.TypeOf(pg).Implements(pluginType) {
			return fmt.Errorf("plugin %q does not extend %s plugin", ep.Name, pluginType.Name())
		}

		if set.Has(ep.Name) {
			return fmt.Errorf("plugin %q already registered as %q", ep.Name, pluginType.Name())
		}

		set.Insert(ep.Name)

		newPlugins := reflect.Append(plugins, reflect.ValueOf(pg))
		plugins.Set(newPlugins)
	}
	return nil
}

// extensionPoint encapsulates desired and applied set of plugins at a specific extension
// point. This is used to simplify iterating over all extension points supported by the
// frameworkImpl.
type extensionPoint struct {
	// the set of plugins to be configured at this extension point.
	plugins *deschedulerconfig.PluginSet
	// a pointer to the slice storing plugins implementations that will run at this
	// extension point.
	slicePtr interface{}
}

func (f *frameworkImpl) getEvictorExtensionPoint(plugins *deschedulerconfig.Plugins) []extensionPoint {
	if plugins == nil {
		return nil
	}

	return []extensionPoint{
		{&plugins.Evictor, &f.evictorPlugins},
	}
}

func (f *frameworkImpl) getOtherExtensionPoints(plugins *deschedulerconfig.Plugins) []extensionPoint {
	if plugins == nil {
		return nil
	}

	return []extensionPoint{
		{&plugins.Deschedule, &f.deschedulePlugins},
		{&plugins.Balance, &f.balancePlugins},
	}
}

func pluginsNeeded(pgSet sets.String, points []extensionPoint) {
	for _, e := range points {
		for _, pg := range e.plugins.Enabled {
			pgSet.Insert(pg.Name)
		}
	}
}

func (f *frameworkImpl) ClientSet() clientset.Interface {
	return f.clientSet
}

func (f *frameworkImpl) KubeConfig() *restclient.Config {
	return f.kubeConfig
}

func (f *frameworkImpl) EventRecorder() events.EventRecorder {
	return f.eventRecorder
}

func (f *frameworkImpl) Evictor() framework.Evictor {
	if len(f.evictorPlugins) == 0 {
		panic("No Evictor plugin is registered in the frameworkImpl.")
	}
	return f.evictorPlugins[0]
}

func (f *frameworkImpl) GetPodsAssignedToNodeFunc() framework.GetPodsAssignedToNodeFunc {
	return f.getPodsAssignedToNodeFunc
}

func (f *frameworkImpl) SharedInformerFactory() informers.SharedInformerFactory {
	return f.sharedInformerFactory
}

func (f *frameworkImpl) RunDeschedulePlugins(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	var errs []error
	for _, pl := range f.deschedulePlugins {
		childCtx := framework.PluginNameWithContext(ctx, pl.Name())
		status := pl.Deschedule(childCtx, nodes)
		if status != nil && status.Err != nil {
			errs = append(errs, status.Err)
		}
	}

	aggrErr := errors.NewAggregate(errs)
	if aggrErr == nil {
		return &framework.Status{}
	}

	return &framework.Status{
		Err: fmt.Errorf("%v", aggrErr.Error()),
	}
}

func (f *frameworkImpl) RunBalancePlugins(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	var errs []error
	for _, pl := range f.balancePlugins {
		childCtx := framework.PluginNameWithContext(ctx, pl.Name())
		status := pl.Balance(childCtx, nodes)
		if status != nil && status.Err != nil {
			errs = append(errs, status.Err)
		}
	}

	aggrErr := errors.NewAggregate(errs)
	if aggrErr == nil {
		return &framework.Status{}
	}

	return &framework.Status{
		Err: fmt.Errorf("%v", aggrErr.Error()),
	}
}
