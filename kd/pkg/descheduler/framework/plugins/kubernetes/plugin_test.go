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

package kubernetes

import (
	"context"
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/sets"
	k8sdeschedulerframework "sigs.k8s.io/descheduler/pkg/framework"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/nodeutilization"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/podlifetime"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removeduplicates"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removefailedpods"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removepodshavingtoomanyrestarts"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removepodsviolatinginterpodantiaffinity"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removepodsviolatingnodeaffinity"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removepodsviolatingnodetaints"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/removepodsviolatingtopologyspreadconstraint"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework/plugins/kubernetes/defaultevictor"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
	frameworktesting "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/testing"
)

type TestPluginArgs struct {
	metav1.TypeMeta `json:",inline"`
	Err             string
}

func (i *TestPluginArgs) DeepCopyObject() runtime.Object {
	return i
}

const (
	testPluginName        = "TestPlugin"
	testEvictorPluginName = "TestEvictor"
)

var _ k8sdeschedulerframework.DeschedulePlugin = &TestPlugin{}
var _ k8sdeschedulerframework.BalancePlugin = &TestPlugin{}

type TestPlugin struct {
	name            string
	args            TestPluginArgs
	descheduleCount *int
	balanceCount    *int
}

func newDeschedulePluginFactory(descheduleCount, balanceCount *int) PluginFactory {
	return func(obj runtime.Object, handle k8sdeschedulerframework.Handle) (k8sdeschedulerframework.Plugin, error) {
		args := obj.(*TestPluginArgs)
		return &TestPlugin{name: testPluginName, args: *args, descheduleCount: descheduleCount, balanceCount: balanceCount}, nil
	}
}

func (pl *TestPlugin) Name() string {
	return pl.name
}

func (pl *TestPlugin) Deschedule(ctx context.Context, nodes []*corev1.Node) *k8sdeschedulerframework.Status {
	if pl.descheduleCount != nil {
		*pl.descheduleCount += 1
	}

	if pl.args.Err == "" {
		return &k8sdeschedulerframework.Status{}
	}
	return &k8sdeschedulerframework.Status{Err: errors.New(pl.args.Err)}
}

func (pl *TestPlugin) Balance(ctx context.Context, nodes []*corev1.Node) *k8sdeschedulerframework.Status {
	if pl.balanceCount != nil {
		*pl.balanceCount += 1
	}
	if pl.args.Err == "" {
		return &k8sdeschedulerframework.Status{}
	}
	return &k8sdeschedulerframework.Status{Err: errors.New(pl.args.Err)}
}

var _ framework.EvictPlugin = &TestEvictorPlugin{}
var _ framework.FilterPlugin = &TestEvictorPlugin{}

type TestEvictorPlugin struct {
	handle framework.Handle
}

func newTestEvictorPlugin(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	return &TestEvictorPlugin{
		handle: handle,
	}, nil
}

func (pl *TestEvictorPlugin) Name() string {
	return testEvictorPluginName
}

func (pl *TestEvictorPlugin) Filter(pod *corev1.Pod) bool {
	return true
}

func (pl *TestEvictorPlugin) PreEvictionFilter(pod *corev1.Pod) bool {
	return true
}

func (pl *TestEvictorPlugin) Evict(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions) bool {
	return false
}

func TestSetupK8sDeschedulerPlugins(t *testing.T) {
	registry := frameworkruntime.Registry{}
	SetupK8sDeschedulerPlugins(registry)
	pluginNames := sets.StringKeySet(registry)
	expectPluginNames := sets.NewString(
		defaultevictor.PluginName,
		nodeutilization.LowNodeUtilizationPluginName,
		nodeutilization.HighNodeUtilizationPluginName,
		podlifetime.PluginName,
		removefailedpods.PluginName,
		removeduplicates.PluginName,
		removepodsviolatingnodetaints.PluginName,
		removepodshavingtoomanyrestarts.PluginName,
		removepodsviolatingnodeaffinity.PluginName,
		removepodsviolatinginterpodantiaffinity.PluginName,
		removepodsviolatingtopologyspreadconstraint.PluginName,
	)
	assert.Equal(t, expectPluginNames, pluginNames)
}

func TestPluginDescriptor(t *testing.T) {
	var descheduleCount, balanceCount int
	descriptor := &PluginDescriptor{
		Name:          testPluginName,
		Factory:       newDeschedulePluginFactory(&descheduleCount, &balanceCount),
		ArgsPrototype: &TestPluginArgs{},
		ArgsDefaulter: func(obj runtime.Object) {
			_, ok := obj.(*TestPluginArgs)
			assert.True(t, ok)
		},
		ArgsValidator: func(obj runtime.Object) error {
			_, ok := obj.(*TestPluginArgs)
			assert.True(t, ok)
			return nil
		},
	}
	fh, err := frameworktesting.NewFramework(
		[]frameworktesting.RegisterPluginFunc{
			func(reg *frameworkruntime.Registry, profile *deschedulerconfig.DeschedulerProfile) {
				reg.Register(testEvictorPluginName, newTestEvictorPlugin)
				profile.Plugins.Evict.Enabled = append(profile.Plugins.Evict.Enabled, deschedulerconfig.Plugin{Name: testEvictorPluginName})
				profile.Plugins.Filter.Enabled = append(profile.Plugins.Filter.Enabled, deschedulerconfig.Plugin{Name: testEvictorPluginName})
			},
			func(reg *frameworkruntime.Registry, profile *deschedulerconfig.DeschedulerProfile) {
				reg.Register(testPluginName, descriptor.New)
				profile.Plugins.Deschedule.Enabled = append(profile.Plugins.Deschedule.Enabled, deschedulerconfig.Plugin{Name: testPluginName})
				profile.Plugins.Balance.Enabled = append(profile.Plugins.Balance.Enabled, deschedulerconfig.Plugin{Name: testPluginName})
				profile.PluginConfig = append(profile.PluginConfig, deschedulerconfig.PluginConfig{
					Name: testPluginName,
					Args: &runtime.Unknown{
						Raw:         []byte(`{}`),
						ContentType: runtime.ContentTypeJSON,
					},
				})
			},
		},
		"test",
	)
	assert.NoError(t, err)

	s := fh.RunDeschedulePlugins(context.TODO(), nil)
	assert.True(t, s == nil || s.Err == nil)
	s = fh.RunBalancePlugins(context.TODO(), nil)
	assert.True(t, s == nil || s.Err == nil)
	assert.Equal(t, 1, descheduleCount)
	assert.Equal(t, 1, balanceCount)
}
