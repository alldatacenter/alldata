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
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes/fake"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/record"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

const (
	testPlugin1                 = "deschedule-plugin-1"
	deschedulePluginWithEvictor = "deschedule-plugin-with-evictor"
	evictorPluginName           = "test-evictor-plugin"
	evictorPluginName1          = "evictor-plugin-name-1"

	testProfileName = "test-profile"
)

type injectedResult struct {
	Err string
}

type evictFilterFn func(pod *corev1.Pod) bool

var _ framework.Evictor = &TestEvictorPlugin{}
var _ framework.DeschedulePlugin = &TestEvictorPlugin{}

type TestEvictorPlugin struct {
	handle  framework.Handle
	filters []evictFilterFn
}

func newTestEvictorPluginFactory(filters ...evictFilterFn) PluginFactory {
	return func(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
		return &TestEvictorPlugin{
			handle:  handle,
			filters: filters,
		}, nil
	}
}

func (pl *TestEvictorPlugin) Name() string {
	return evictorPluginName
}

func (pl *TestEvictorPlugin) Filter(pod *corev1.Pod) bool {
	for _, fn := range pl.filters {
		if !fn(pod) {
			return false
		}
	}
	return true
}

func (pl *TestEvictorPlugin) Evict(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions) bool {
	return false
}

func (pl *TestEvictorPlugin) Deschedule(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	return &framework.Status{}
}

var _ framework.DeschedulePlugin = &TestDeschedulePluginWithEvictor{}

type TestDeschedulePluginWithEvictor struct {
	evictor framework.Evictor
}

func newTestDeschedulePluginWithEvictor(injArgs runtime.Object, f framework.Handle) (framework.Plugin, error) {
	return &TestDeschedulePluginWithEvictor{evictor: f.Evictor()}, nil
}

func (pl *TestDeschedulePluginWithEvictor) Name() string {
	return deschedulePluginWithEvictor
}

func (pl *TestDeschedulePluginWithEvictor) Deschedule(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	return &framework.Status{}
}

var _ framework.DeschedulePlugin = &TestPlugin{}
var _ framework.BalancePlugin = &TestPlugin{}

type TestPlugin struct {
	name string
	inj  injectedResult
}

func newDeschedulePlugin(injArgs runtime.Object, f framework.Handle) (framework.Plugin, error) {
	var inj injectedResult
	if err := DecodeInto(injArgs, &inj); err != nil {
		return nil, err
	}
	return &TestPlugin{name: testPlugin1, inj: inj}, nil
}

func (pl *TestPlugin) Name() string {
	return pl.name
}

func (pl *TestPlugin) Deschedule(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	if pl.inj.Err == "" {
		return &framework.Status{}
	}
	return &framework.Status{Err: errors.New(pl.inj.Err)}
}

func (pl *TestPlugin) Balance(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	if pl.inj.Err == "" {
		return &framework.Status{}
	}
	return &framework.Status{Err: errors.New(pl.inj.Err)}
}

var registry = Registry{
	evictorPluginName:           newTestEvictorPluginFactory(),
	evictorPluginName1:          newTestEvictorPluginFactory(),
	testPlugin1:                 newDeschedulePlugin,
	deschedulePluginWithEvictor: newTestDeschedulePluginWithEvictor,
}

func TestNewFramework(t *testing.T) {
	tests := []struct {
		name    string
		profile *deschedulerconfig.DeschedulerProfile
		wantErr bool
	}{
		{
			name: "normal register",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Deschedule: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: deschedulePluginWithEvictor},
						},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "missing evictor",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Deschedule: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
						},
					},
				},
			},
			wantErr: true,
		},
		{
			name: "many evictors",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
							{Name: evictorPluginName1},
						},
					},
				},
			},
			wantErr: true,
		},
		{
			name: "duplicate plugins",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Deschedule: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: evictorPluginName},
						},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			registryClone := Registry{}
			err := registryClone.Merge(registry)
			if err != nil {
				t.Fatalf("failed to merge registry, err: %v", err)
			}

			initTimes := map[string]int{}
			for k, v := range registryClone {
				pluginName, pluginFactory := k, v
				registryClone[pluginName] = func(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
					initTimes[pluginName]++
					return pluginFactory(args, handle)
				}
			}

			fakeClient := fake.NewSimpleClientset()
			sharedInformerFactory := informers.NewSharedInformerFactory(fakeClient, 0)

			fakeRecorder := record.NewFakeRecorder(1024)
			eventRecorderAdapter := record.NewEventRecorderAdapter(fakeRecorder)
			kubeConfig := &restclient.Config{}

			f, err := NewFramework(registryClone, tt.profile,
				WithClientSet(fakeClient),
				WithSharedInformerFactory(sharedInformerFactory),
				WithKubeConfig(kubeConfig),
				WithGetPodsAssignedToNodeFunc(func(s string, filterFunc framework.FilterFunc) ([]*corev1.Pod, error) {
					return nil, errors.New("unsupported")
				}),
				WithEventRecorder(eventRecorderAdapter),
				WithCaptureProfile(func(profile deschedulerconfig.DeschedulerProfile) {
					t.Log(profile)
				}))
			if tt.wantErr && err == nil {
				t.Fatalf("expect err but got nil")
			}
			if !tt.wantErr && f == nil {
				t.Fatalf("expect create framework but got nil")
			}

			for pluginName, count := range initTimes {
				if count == 0 {
					t.Fatalf("expect init plugin %s but not", pluginName)
				}
				if count > 1 {
					t.Fatalf("expect init plugin %s once, but too many times %d", pluginName, count)
				}
			}

			if f != nil && !tt.wantErr {
				assert.Equal(t, fakeClient, f.ClientSet())
				assert.Equal(t, sharedInformerFactory, f.SharedInformerFactory())
				assert.Equal(t, kubeConfig, f.KubeConfig())
				assert.Equal(t, eventRecorderAdapter, f.EventRecorder())
				indexFn := f.GetPodsAssignedToNodeFunc()
				_, err = indexFn("", nil)
				assert.Equal(t, "unsupported", err.Error())
			}

		})
	}
}

func TestRunDeschedulePlugins(t *testing.T) {
	testDeschedulePluginName := "internal-deschedule-plugins"

	tests := []struct {
		name    string
		profile *deschedulerconfig.DeschedulerProfile
		wantErr error
	}{
		{
			name: "run plugins with one error",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				PluginConfig: []deschedulerconfig.PluginConfig{
					{
						Name: testDeschedulePluginName,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
				},
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Deschedule: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: testDeschedulePluginName},
						},
					},
				},
			},
			wantErr: errors.New("expected failed"),
		},
		{
			name: "run plugins with many errors",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				PluginConfig: []deschedulerconfig.PluginConfig{
					{
						Name: testDeschedulePluginName,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
					{
						Name: testPlugin1,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed with testPlugin1"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
				},
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Deschedule: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: testDeschedulePluginName},
						},
					},
				},
			},
			wantErr: errors.New("[expected failed with testPlugin1, expected failed]"),
		},
		{
			name: "run plugins with empty plugins",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
				},
			},
			wantErr: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			registryClone := Registry{}
			err := registryClone.Merge(registry)
			if err != nil {
				t.Fatalf("failed to merge registry, err: %v", err)
			}

			registryClone[testDeschedulePluginName] = newDeschedulePlugin

			f, err := NewFramework(registryClone, tt.profile)
			if err != nil {
				t.Fatalf("failed to NewFramework, err %v", err)
			}

			status := f.RunDeschedulePlugins(context.TODO(), nil)
			err = nil
			if status != nil {
				err = status.Err
			}

			if (tt.wantErr != nil) && err == nil {
				t.Fatalf("expect err but got nil")
			}
			if tt.wantErr != nil && tt.wantErr.Error() != err.Error() {
				t.Fatalf("expect err %v but got %v", tt.wantErr, err)
			}
			if tt.wantErr != nil && f == nil {
				t.Fatalf("expect create framework but got nil")
			}
		})
	}
}

func TestRunBalancePlugins(t *testing.T) {
	testBalancePluginName := "internal-balance-plugins"

	tests := []struct {
		name    string
		profile *deschedulerconfig.DeschedulerProfile
		wantErr error
	}{
		{
			name: "run plugins with one error",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				PluginConfig: []deschedulerconfig.PluginConfig{
					{
						Name: testBalancePluginName,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
				},
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Balance: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: testBalancePluginName},
						},
					},
				},
			},
			wantErr: errors.New("expected failed"),
		},
		{
			name: "run plugins with many errors",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				PluginConfig: []deschedulerconfig.PluginConfig{
					{
						Name: testBalancePluginName,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
					{
						Name: testPlugin1,
						Args: &runtime.Unknown{
							Raw: []byte(`{"Err": "expected failed with testPlugin1"}`),
							// TODO: Set ContentEncoding and ContentType appropriately.
							// Currently we set ContentTypeJSON to make tests passing.
							ContentType: runtime.ContentTypeJSON,
						},
					},
				},
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
					Balance: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: testPlugin1},
							{Name: testBalancePluginName},
						},
					},
				},
			},
			wantErr: errors.New("[expected failed with testPlugin1, expected failed]"),
		},
		{
			name: "run plugins with empty plugins",
			profile: &deschedulerconfig.DeschedulerProfile{
				Name: testProfileName,
				Plugins: &deschedulerconfig.Plugins{
					Evictor: deschedulerconfig.PluginSet{
						Enabled: []deschedulerconfig.Plugin{
							{Name: evictorPluginName},
						},
					},
				},
			},
			wantErr: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			registryClone := Registry{}
			err := registryClone.Merge(registry)
			if err != nil {
				t.Fatalf("failed to merge registry, err: %v", err)
			}

			registryClone[testBalancePluginName] = newDeschedulePlugin

			f, err := NewFramework(registryClone, tt.profile)
			if err != nil {
				t.Fatalf("failed to NewFramework, err %v", err)
			}

			status := f.RunBalancePlugins(context.TODO(), nil)
			err = nil
			if status != nil {
				err = status.Err
			}

			if (tt.wantErr != nil) && err == nil {
				t.Fatalf("expect err but got nil")
			}
			if tt.wantErr != nil && tt.wantErr.Error() != err.Error() {
				t.Fatalf("expect err %v but got %v", tt.wantErr, err)
			}
			if tt.wantErr != nil && f == nil {
				t.Fatalf("expect create framework but got nil")
			}
		})
	}
}
