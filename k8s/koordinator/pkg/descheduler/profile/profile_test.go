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

package profile

import (
	"context"
	"fmt"
	"strings"
	"testing"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/tools/events"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/names"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
)

var fakeRegistry = frameworkruntime.Registry{
	"MigrationController": newFakePlugin("MigrationController"),
}

func TestNewMap(t *testing.T) {
	cases := []struct {
		name     string
		profiles []deschedulerconfig.DeschedulerProfile
		wantErr  string
	}{
		{
			name: "descheduler name is need",
			profiles: []deschedulerconfig.DeschedulerProfile{
				{
					Plugins: &deschedulerconfig.Plugins{
						Deschedule: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{},
						},
						Evictor: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{
								{Name: names.MigrationController},
							},
						},
					},
				},
			},
			wantErr: "descheduler name is needed",
		},
		{
			name: "descheduler plugin is required",
			profiles: []deschedulerconfig.DeschedulerProfile{
				{
					Name: "descheduler-1",
				},
			},
			wantErr: "plugins required for profile",
		},
		{
			name: "duplicate descheduler name",
			profiles: []deschedulerconfig.DeschedulerProfile{
				{
					Name: "profile-1",
					Plugins: &deschedulerconfig.Plugins{
						Deschedule: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{},
						},
						Evictor: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{
								{Name: names.MigrationController},
							},
						},
					},
				}, {
					Name: "profile-1",
					Plugins: &deschedulerconfig.Plugins{
						Deschedule: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{},
						},
						Evictor: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{
								{Name: names.MigrationController},
							},
						},
					},
				},
			},
			wantErr: "duplicate profile",
		},
		{
			name: "valid profile",
			profiles: []deschedulerconfig.DeschedulerProfile{
				{
					Name: "profile-1",
					Plugins: &deschedulerconfig.Plugins{
						Deschedule: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{},
						},
						Evictor: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{
								{Name: names.MigrationController},
							},
						},
					},
				},
			},
		},
		{
			name: "invalid profile",
			profiles: []deschedulerconfig.DeschedulerProfile{
				{
					Name: "invalid-profile",
					Plugins: &deschedulerconfig.Plugins{
						Deschedule: deschedulerconfig.PluginSet{
							Enabled: []deschedulerconfig.Plugin{},
						},
					},
				},
			},
			wantErr: "no evict plugin is enabled",
		},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			m, err := NewMap(tc.profiles, fakeRegistry, nilRecordFactory)
			if err := checkErr(err, tc.wantErr); err != nil {
				t.Fatal(err)
			}
			if len(tc.wantErr) != 0 {
				return
			}
			if len(m) != len(tc.profiles) {
				t.Errorf("got %d profiles, want %d", len(m), len(tc.profiles))
			}
		})
	}
}

type fakePlugin struct {
	name string
}

func (p *fakePlugin) Name() string {
	return p.name
}

func (p *fakePlugin) Filter(_ *corev1.Pod) bool {
	return false
}

func (p *fakePlugin) Evict(_ context.Context, _ *corev1.Pod, _ framework.EvictOptions) bool {
	return false
}

func newFakePlugin(name string) func(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	return func(_ runtime.Object, _ framework.Handle) (framework.Plugin, error) {
		return &fakePlugin{name: name}, nil
	}
}

func nilRecordFactory(_ string) events.EventRecorder {
	return nil
}

func checkErr(err error, wantErr string) error {
	if len(wantErr) == 0 {
		return err
	}

	if err == nil || !strings.Contains(err.Error(), wantErr) {
		return fmt.Errorf("got error %q, want %q", err, wantErr)
	}

	return nil
}
